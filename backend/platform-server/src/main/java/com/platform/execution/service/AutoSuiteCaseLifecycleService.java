/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件内自动化用例级生命周期服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.dto.AutoSuiteCaseLifecycleDTO;
import com.platform.execution.dto.AutoSuiteCaseLifecycleSaveRequest;
import com.platform.execution.entity.AutoCase;
import com.platform.execution.entity.AutoSuiteCaseLifecycle;
import com.platform.execution.mapper.AutoCaseMapper;
import com.platform.execution.mapper.AutoSuiteCaseLifecycleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自动化套件内自动化用例级生命周期服务
 *
 * <p>管理自动化套件内每条自动化用例差异化的 Setup/Teardown 步骤配置。
 * 当 auto_suite_case_lifecycle 中存在某条自动化用例的记录时，
 * 其 setup_steps / teardown_steps 优先于自动化用例自身的配置。
 */
@Service
@RequiredArgsConstructor
public class AutoSuiteCaseLifecycleService {

    private final AutoSuiteCaseLifecycleMapper lifecycleMapper;
    private final AutoCaseMapper autoCaseMapper;

    /**
     * 查询自动化套件内所有自动化用例级生命周期配置
     *
     * @param autoSuiteId 自动化套件 ID
     * @return 生命周期配置列表（附带自动化用例名称）
     */
    public List<AutoSuiteCaseLifecycleDTO> listByAutoSuite(Long autoSuiteId) {
        LambdaQueryWrapper<AutoSuiteCaseLifecycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuiteCaseLifecycle::getAutoSuiteId, autoSuiteId);
        List<AutoSuiteCaseLifecycle> list = lifecycleMapper.selectList(wrapper);

        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询自动化用例名称
        Set<Long> caseIds = list.stream()
                .map(AutoSuiteCaseLifecycle::getAutoCaseId)
                .collect(Collectors.toSet());
        Map<Long, String> caseNameMap = new HashMap<>();
        if (!caseIds.isEmpty()) {
            List<AutoCase> cases = autoCaseMapper.selectBatchIds(caseIds);
            for (AutoCase tc : cases) {
                caseNameMap.put(tc.getId(), tc.getName());
            }
        }

        List<AutoSuiteCaseLifecycleDTO> result = new ArrayList<>();
        for (AutoSuiteCaseLifecycle lc : list) {
            AutoSuiteCaseLifecycleDTO dto = new AutoSuiteCaseLifecycleDTO();
            dto.setId(lc.getId());
            dto.setAutoSuiteId(lc.getAutoSuiteId());
            dto.setAutoCaseId(lc.getAutoCaseId());
            dto.setCaseName(caseNameMap.getOrDefault(lc.getAutoCaseId(), ""));
            dto.setSetupSteps(lc.getSetupSteps());
            dto.setTeardownSteps(lc.getTeardownSteps());
            dto.setCreatedAt(lc.getCreatedAt());
            dto.setUpdatedAt(lc.getUpdatedAt());
            result.add(dto);
        }
        return result;
    }

    /**
     * 批量保存自动化套件内自动化用例级生命周期配置
     *
     * <p>对每个 item：
     * <ul>
     *   <li>先校验该自动化用例必须存在于 auto_case 表且归属当前自动化套件（自动化套件仅由自动化用例组成）</li>
     *   <li>如果该自动化用例已有记录则更新</li>
     *   <li>如果没有记录则新增</li>
     *   <li>如果 setupSteps 和 teardownSteps 均为空，则删除该记录（恢复默认）</li>
     * </ul>
     *
     * @param autoSuiteId 自动化套件 ID
     * @param request     保存请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLifecycle(Long autoSuiteId, AutoSuiteCaseLifecycleSaveRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            // 清空所有配置
            LambdaQueryWrapper<AutoSuiteCaseLifecycle> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(AutoSuiteCaseLifecycle::getAutoSuiteId, autoSuiteId);
            lifecycleMapper.delete(delWrapper);
            return;
        }

        // 归属校验：自动化套件仅由自动化用例组成，
        // 每个 item 的 autoCaseId 必须是存在于 auto_case 表且归属当前套件的自动化用例
        for (AutoSuiteCaseLifecycleSaveRequest.LifecycleItem item : request.getItems()) {
            if (item.getAutoCaseId() == null) {
                throw new BusinessException(ErrorCode.AUTO_CASE_NOT_IN_SUITE, "autoCaseId 不能为空");
            }
            AutoCase autoCase = autoCaseMapper.selectById(item.getAutoCaseId());
            if (autoCase == null || !autoSuiteId.equals(autoCase.getAutoSuiteId())) {
                throw new BusinessException(ErrorCode.AUTO_CASE_NOT_IN_SUITE,
                        "自动化用例不存在或不属于该自动化套件：" + item.getAutoCaseId());
            }
        }

        // 查询现有记录
        LambdaQueryWrapper<AutoSuiteCaseLifecycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuiteCaseLifecycle::getAutoSuiteId, autoSuiteId);
        List<AutoSuiteCaseLifecycle> existing = lifecycleMapper.selectList(wrapper);
        Map<Long, AutoSuiteCaseLifecycle> existingMap = new HashMap<>();
        for (AutoSuiteCaseLifecycle lc : existing) {
            existingMap.put(lc.getAutoCaseId(), lc);
        }

        Set<Long> incomingCaseIds = new HashSet<>();
        for (AutoSuiteCaseLifecycleSaveRequest.LifecycleItem item : request.getItems()) {
            incomingCaseIds.add(item.getAutoCaseId());

            boolean emptySetup = item.getSetupSteps() == null || item.getSetupSteps().trim().isEmpty();
            boolean emptyTeardown = item.getTeardownSteps() == null || item.getTeardownSteps().trim().isEmpty();

            if (emptySetup && emptyTeardown) {
                // 删除已有记录（恢复默认）
                AutoSuiteCaseLifecycle lc = existingMap.get(item.getAutoCaseId());
                if (lc != null) {
                    lifecycleMapper.deleteById(lc.getId());
                }
                continue;
            }

            AutoSuiteCaseLifecycle lc = existingMap.get(item.getAutoCaseId());
            if (lc == null) {
                // 新增
                lc = new AutoSuiteCaseLifecycle();
                lc.setAutoSuiteId(autoSuiteId);
                lc.setAutoCaseId(item.getAutoCaseId());
                lc.setSetupSteps(item.getSetupSteps());
                lc.setTeardownSteps(item.getTeardownSteps());
                lifecycleMapper.insert(lc);
            } else {
                // 更新
                lc.setSetupSteps(item.getSetupSteps());
                lc.setTeardownSteps(item.getTeardownSteps());
                lifecycleMapper.updateById(lc);
            }
        }

        // 删除请求中未包含的旧记录
        for (AutoSuiteCaseLifecycle lc : existing) {
            if (!incomingCaseIds.contains(lc.getAutoCaseId())) {
                lifecycleMapper.deleteById(lc.getId());
            }
        }
    }

    /**
     * 根据自动化套件和自动化用例 ID 查询生命周期配置
     *
     * @param autoSuiteId 自动化套件 ID
     * @param autoCaseId  自动化用例 ID
     * @return 生命周期配置，不存在则返回 null
     */
    public AutoSuiteCaseLifecycle findByAutoSuiteAndCase(Long autoSuiteId, Long autoCaseId) {
        LambdaQueryWrapper<AutoSuiteCaseLifecycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuiteCaseLifecycle::getAutoSuiteId, autoSuiteId)
                .eq(AutoSuiteCaseLifecycle::getAutoCaseId, autoCaseId);
        return lifecycleMapper.selectOne(wrapper);
    }
}
