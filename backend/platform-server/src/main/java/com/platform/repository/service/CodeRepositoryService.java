/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 测试代码仓库管理服务
 */
package com.platform.repository.service;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.util.AesCryptoUtil;
import com.platform.project.service.ProjectService;
import com.platform.repository.dto.PullLogResponse;
import com.platform.repository.dto.PullResultResponse;
import com.platform.repository.dto.RepositoryCreateRequest;
import com.platform.repository.dto.RepositoryResponse;
import com.platform.repository.dto.RepositoryUpdateRequest;
import com.platform.repository.entity.CodeRepository;
import com.platform.repository.entity.CodeRepositoryPullLog;
import com.platform.repository.mapper.CodeRepositoryMapper;
import com.platform.repository.mapper.CodeRepositoryPullLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试代码仓库管理服务
 *
 * <p>提供仓库 CRUD、JGit 克隆/增量拉取、拉取历史记录能力。
 * 本地代码目录规则：{storage-path}/{projectId}/{repoId}，仓库删除时同步清理。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeRepositoryService {

    private final CodeRepositoryMapper repositoryMapper;
    private final CodeRepositoryPullLogMapper pullLogMapper;
    private final ProjectService projectService;

    @Value("${repository.storage-path}")
    private String storagePath;

    @Value("${repository.crypto-key}")
    private String cryptoKey;

    @Value("${repository.clone-timeout-seconds}")
    private Integer cloneTimeoutSeconds;

    /**
     * 拉取类型：首次克隆
     */
    private static final String PULL_TYPE_CLONE = "CLONE";

    /**
     * 拉取类型：增量更新
     */
    private static final String PULL_TYPE_PULL = "PULL";

    /**
     * 拉取状态：进行中
     */
    private static final String STATUS_RUNNING = "RUNNING";

    /**
     * 拉取状态：成功
     */
    private static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * 拉取状态：失败
     */
    private static final String STATUS_FAILED = "FAILED";

    /**
     * Git 仓库目录标识
     */
    private static final String GIT_DIR_MARKER = ".git";

    /**
     * 本地分支引用前缀
     */
    private static final String REFS_HEADS_PREFIX = "refs/heads/";

    /**
     * 拉取历史信息字段最大长度（与表字段一致）
     */
    private static final int MESSAGE_MAX_LENGTH = 2000;

    /**
     * 拉取历史默认返回条数
     */
    private static final int DEFAULT_LOG_LIMIT = 20;

    /**
     * 查询项目下的仓库列表
     */
    public List<RepositoryResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<CodeRepository> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeRepository::getProjectId, projectId);
        wrapper.orderByDesc(CodeRepository::getCreatedAt);

        List<CodeRepository> list = repositoryMapper.selectList(wrapper);
        List<RepositoryResponse> result = new ArrayList<>();
        for (CodeRepository repo : list) {
            result.add(toResponse(repo));
        }
        return result;
    }

    /**
     * 创建仓库（认证密码 AES 加密入库）
     */
    @Transactional(rollbackFor = Exception.class)
    public RepositoryResponse create(RepositoryCreateRequest request) {
        projectService.findActiveById(request.getProjectId());
        checkNameDuplicate(request.getProjectId(), request.getName(), null);

        CodeRepository repo = new CodeRepository();
        repo.setProjectId(request.getProjectId());
        repo.setName(request.getName());
        repo.setGitUrl(request.getGitUrl());
        repo.setBranch(normalizeToNull(request.getBranch()));
        repo.setDescription(request.getDescription());
        repo.setAuthUsername(normalizeToNull(request.getAuthUsername()));
        if (StringUtils.hasText(request.getAuthPassword())) {
            repo.setAuthPassword(encryptPassword(request.getAuthPassword()));
        }

        repositoryMapper.insert(repo);
        return toResponse(repo);
    }

    /**
     * 更新仓库（authPassword 留空表示保持原密码不变）
     */
    @Transactional(rollbackFor = Exception.class)
    public RepositoryResponse update(Long repoId, RepositoryUpdateRequest request) {
        CodeRepository repo = findById(repoId);
        checkNameDuplicate(repo.getProjectId(), request.getName(), repoId);

        repo.setName(request.getName());
        repo.setGitUrl(request.getGitUrl());
        repo.setBranch(normalizeToNull(request.getBranch()));
        repo.setDescription(request.getDescription());
        repo.setAuthUsername(normalizeToNull(request.getAuthUsername()));
        if (StringUtils.hasText(request.getAuthPassword())) {
            repo.setAuthPassword(encryptPassword(request.getAuthPassword()));
        }

        repositoryMapper.updateById(repo);
        return toResponse(repo);
    }

    /**
     * 删除仓库（物理删除记录 + 递归删除本地代码目录，拉取历史由 FK 级联删除）
     */
    public void delete(Long repoId) {
        CodeRepository repo = findById(repoId);

        File localDir = buildLocalDir(repo.getProjectId(), repoId);
        if (localDir.exists()) {
            FileUtil.del(localDir);
            log.info("已删除仓库本地代码目录: repoId={}, path={}", repoId, localDir.getAbsolutePath());
        }

        repositoryMapper.deleteById(repoId);
    }

    /**
     * 拉取仓库代码（同步执行 + 历史记录）
     *
     * <p>本地目录不存在（或残留无效）时执行 CLONE，否则执行 PULL。
     * 拉取失败不抛异常，转为 success=false 的业务结果返回。
     */
    public PullResultResponse pull(Long repoId) {
        CodeRepository repo = findById(repoId);
        File localDir = buildLocalDir(repo.getProjectId(), repo.getId());

        boolean isClone = !isGitRepository(localDir);
        if (localDir.exists() && !isGitRepository(localDir)) {
            // 目录存在但非 Git 仓库（上次克隆失败残留），清理后重新克隆
            FileUtil.del(localDir);
        }

        // 记录拉取历史（RUNNING）
        CodeRepositoryPullLog pullLog = new CodeRepositoryPullLog();
        pullLog.setRepositoryId(repo.getId());
        pullLog.setPullType(isClone ? PULL_TYPE_CLONE : PULL_TYPE_PULL);
        pullLog.setBranch(repo.getBranch());
        pullLog.setStatus(STATUS_RUNNING);
        pullLogMapper.insert(pullLog);

        long startTime = System.currentTimeMillis();
        boolean success = false;
        String commitId = null;
        String message = null;

        try {
            UsernamePasswordCredentialsProvider credentialsProvider = buildCredentialsProvider(repo);
            if (isClone) {
                try (Git git = cloneRepository(repo, localDir, credentialsProvider)) {
                    commitId = resolveHeadCommitId(git);
                }
                message = "克隆成功";
            } else {
                try (Git git = Git.open(localDir)) {
                    // 先同步远程引用，确保配置分支的远程跟踪引用存在
                    git.fetch()
                            .setCredentialsProvider(credentialsProvider)
                            .setRemoveDeletedRefs(true)
                            .setTimeout(cloneTimeoutSeconds)
                            .call();
                    checkoutBranchIfNeeded(git, repo.getBranch());
                    git.pull()
                            .setCredentialsProvider(credentialsProvider)
                            .setTimeout(cloneTimeoutSeconds)
                            .call();
                    commitId = resolveHeadCommitId(git);
                }
                message = "拉取成功";
            }
            success = true;
        } catch (GitAPIException | IOException | BusinessException e) {
            log.warn("仓库 [{}] 拉取失败: {}", repo.getName(), e.getMessage());
            message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (isClone && localDir.exists()) {
                // 克隆失败清理残留目录，保证下次可重新克隆
                FileUtil.del(localDir);
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        finishPullLog(pullLog, success, commitId, message, durationMs);
        updateRepositoryAfterPull(repo, success, commitId);

        PullResultResponse response = new PullResultResponse();
        response.setLogId(pullLog.getId());
        response.setSuccess(success);
        response.setPullType(pullLog.getPullType());
        response.setBranch(repo.getBranch());
        response.setCommitId(commitId);
        response.setMessage(message);
        response.setDurationMs(durationMs);
        response.setFinishedAt(LocalDateTime.now());
        return response;
    }

    /**
     * 查询仓库拉取历史（最近 20 条）
     */
    public List<PullLogResponse> listPullLogs(Long repoId) {
        findById(repoId);

        LambdaQueryWrapper<CodeRepositoryPullLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeRepositoryPullLog::getRepositoryId, repoId)
                .orderByDesc(CodeRepositoryPullLog::getId)
                .last("LIMIT " + DEFAULT_LOG_LIMIT);

        List<CodeRepositoryPullLog> logs = pullLogMapper.selectList(wrapper);
        List<PullLogResponse> result = new ArrayList<>();
        for (CodeRepositoryPullLog logEntry : logs) {
            result.add(toPullLogResponse(logEntry));
        }
        return result;
    }

    // ───────────────────── 私有方法 ─────────────────────

    private CodeRepository findById(Long repoId) {
        CodeRepository repo = repositoryMapper.selectById(repoId);
        if (repo == null) {
            throw new BusinessException(ErrorCode.REPOSITORY_NOT_FOUND, "仓库不存在：" + repoId);
        }
        return repo;
    }

    /**
     * 校验项目内仓库名称唯一（excludeId 用于编辑时排除自身）
     */
    private void checkNameDuplicate(Long projectId, String name, Long excludeId) {
        LambdaQueryWrapper<CodeRepository> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeRepository::getProjectId, projectId)
                .eq(CodeRepository::getName, name);
        if (excludeId != null) {
            wrapper.ne(CodeRepository::getId, excludeId);
        }
        Long count = repositoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.REPOSITORY_NAME_DUPLICATE, "仓库名称已存在：" + name);
        }
    }

    /**
     * 构建本地代码目录：{storage-path}/{projectId}/{repoId}
     */
    private File buildLocalDir(Long projectId, Long repoId) {
        return new File(storagePath, projectId + File.separator + repoId);
    }

    /**
     * 判断目录是否为有效 Git 仓库（存在 .git 元数据）
     */
    private boolean isGitRepository(File dir) {
        return dir.exists() && new File(dir, GIT_DIR_MARKER).isDirectory();
    }

    /**
     * 执行克隆
     */
    private Git cloneRepository(CodeRepository repo, File localDir,
                                UsernamePasswordCredentialsProvider credentialsProvider) throws GitAPIException {
        CloneCommand command = Git.cloneRepository()
                .setURI(repo.getGitUrl())
                .setDirectory(localDir)
                .setTimeout(cloneTimeoutSeconds);
        if (StringUtils.hasText(repo.getBranch())) {
            command.setBranch(REFS_HEADS_PREFIX + repo.getBranch());
        }
        if (credentialsProvider != null) {
            command.setCredentialsProvider(credentialsProvider);
        }
        return command.call();
    }

    /**
     * 配置了分支且与当前分支不一致时切换分支（本地不存在则从远程跟踪分支创建）
     */
    private void checkoutBranchIfNeeded(Git git, String branch) throws GitAPIException, IOException {
        if (!StringUtils.hasText(branch)) {
            return;
        }
        String currentBranch = git.getRepository().getBranch();
        if (branch.equals(currentBranch)) {
            return;
        }
        git.checkout()
                .setName(branch)
                .setCreateBranch(true)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .call();
    }

    /**
     * 读取当前 HEAD commit ID
     */
    private String resolveHeadCommitId(Git git) throws IOException {
        ObjectId head = git.getRepository().resolve("HEAD");
        return head != null ? head.getName() : null;
    }

    /**
     * 构建认证提供者（用户名与密码/Token 均已配置时启用）
     */
    private UsernamePasswordCredentialsProvider buildCredentialsProvider(CodeRepository repo) {
        if (!StringUtils.hasText(repo.getAuthUsername()) || !StringUtils.hasText(repo.getAuthPassword())) {
            return null;
        }
        String password = decryptPassword(repo.getAuthPassword());
        return new UsernamePasswordCredentialsProvider(repo.getAuthUsername(), password);
    }

    /**
     * 更新拉取历史为最终状态
     */
    private void finishPullLog(CodeRepositoryPullLog pullLog, boolean success,
                               String commitId, String message, long durationMs) {
        pullLog.setStatus(success ? STATUS_SUCCESS : STATUS_FAILED);
        pullLog.setCommitId(commitId);
        pullLog.setMessage(truncate(message));
        pullLog.setDurationMs(durationMs);
        pullLogMapper.updateById(pullLog);
    }

    /**
     * 拉取结束后更新仓库的最近拉取状态
     */
    private void updateRepositoryAfterPull(CodeRepository repo, boolean success, String commitId) {
        repo.setLastPullAt(LocalDateTime.now());
        repo.setLastPullStatus(success ? STATUS_SUCCESS : STATUS_FAILED);
        if (success) {
            repo.setLastCommitId(commitId);
            repo.setLocalPath(repo.getProjectId() + "/" + repo.getId());
        }
        repositoryMapper.updateById(repo);
    }

    private String encryptPassword(String plain) {
        try {
            return AesCryptoUtil.encrypt(plain, cryptoKey);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.REPOSITORY_CRYPTO_ERROR, "仓库凭证加密失败");
        }
    }

    private String decryptPassword(String cipher) {
        try {
            return AesCryptoUtil.decrypt(cipher, cryptoKey);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.REPOSITORY_CRYPTO_ERROR, "仓库凭证解密失败");
        }
    }

    /**
     * 字符串规范化：去首尾空白，空串转 null
     */
    private String normalizeToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MESSAGE_MAX_LENGTH ? message.substring(0, MESSAGE_MAX_LENGTH) : message;
    }

    private RepositoryResponse toResponse(CodeRepository repo) {
        RepositoryResponse response = new RepositoryResponse();
        response.setId(repo.getId());
        response.setProjectId(repo.getProjectId());
        response.setName(repo.getName());
        response.setGitUrl(repo.getGitUrl());
        response.setBranch(repo.getBranch());
        response.setDescription(repo.getDescription());
        response.setAuthUsername(repo.getAuthUsername());
        response.setHasAuth(StringUtils.hasText(repo.getAuthUsername()) && StringUtils.hasText(repo.getAuthPassword()));
        response.setLocalPath(repo.getLocalPath());
        response.setLastPullAt(repo.getLastPullAt());
        response.setLastPullStatus(repo.getLastPullStatus());
        response.setLastCommitId(repo.getLastCommitId());
        response.setCreatedAt(repo.getCreatedAt());
        response.setUpdatedAt(repo.getUpdatedAt());
        return response;
    }

    private PullLogResponse toPullLogResponse(CodeRepositoryPullLog logEntry) {
        PullLogResponse response = new PullLogResponse();
        response.setId(logEntry.getId());
        response.setPullType(logEntry.getPullType());
        response.setBranch(logEntry.getBranch());
        response.setStatus(logEntry.getStatus());
        response.setCommitId(logEntry.getCommitId());
        response.setMessage(logEntry.getMessage());
        response.setDurationMs(logEntry.getDurationMs());
        response.setCreatedAt(logEntry.getCreatedAt());
        return response;
    }
}
