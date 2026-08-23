/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 套件内用例级生命周期服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.execution.dto.SuiteCaseLifecycleDTO;
import com.platform.execution.dto.SuiteCaseLifecycleSaveRequest;
import com.platform.execution.entity.SuiteCaseLifecycle;
import com.platform.execution.entity.TestCase;
import com.platform.execution.mapper.SuiteCaseLifecycleMapper;
import com.platform.execution.mapper.TestCaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 套件内用例级生命周期服务
 *
 * <p>管理套件内每条用例差异化的 Setup/Teardown 步骤配置。
 * 当 suite_case_lifecycle 中存在某条用例的记录时，
 * 其 setup_steps / teardown_steps 优先于用例自身的配置。
 */
@Service
@RequiredArgsConstructor
public class SuiteCaseLifecycleService {

    private final SuiteCaseLifecycleMapper lifecycleMapper;
    private final TestCaseMapper testCaseMapper;

    /**
     * 查询套件内所有用例级生命周期配置
     *
     * @param suiteId 套件 ID
     * @return 生命周期配置列表（附带用例名称）
     */
    public List<SuiteCaseLifecycleDTO> listBySuite(Long suiteId) {
        LambdaQueryWrapper<SuiteCaseLifecycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SuiteCaseLifecycle::getSuiteId, suiteId);
        List<SuiteCaseLifecycle> list = lifecycleMapper.selectList(wrapper);

        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询用例名称
        Set<Long> caseIds = list.stream()
                .map(SuiteCaseLifecycle::getCaseId)
                .collect(Collectors.toSet());
        Map<Long, String> caseNameMap = new HashMap<>();
        if (!caseIds.isEmpty()) {
            List<TestCase> cases = testCaseMapper.selectBatchIds(caseIds);
            for (TestCase tc : cases) {
                caseNameMap.put(tc.getId(), tc.getName());
            }
        }

        List<SuiteCaseLifecycleDTO> result = new ArrayList<>();
        for (SuiteCaseLifecycle lc : list) {
            SuiteCaseLifecycleDTO dto = new SuiteCaseLifecycleDTO();
            dto.setId(lc.getId());
            dto.setSuiteId(lc.getSuiteId());
            dto.setCaseId(lc.getCaseId());
            dto.setCaseName(caseNameMap.getOrDefault(lc.getCaseId(), ""));
            dto.setSetupSteps(lc.getSetupSteps());
            dto.setTeardownSteps(lc.getTeardownSteps());
            dto.setCreatedAt(lc.getCreatedAt());
            dto.setUpdatedAt(lc.getUpdatedAt());
            result.add(dto);
        }
        return result;
    }

    /**
     * 批量保存套件内用例级生命周期配置
     *
     * <p>对每个 item：
     * <ul>
     *   <li>如果该用例已有记录则更新</li>
     *   <li>如果没有记录则新增</li>
     *   <li>如果 setupSteps 和 teardownSteps 均为空，则删除该记录（恢复默认）</li>
     * </ul>
     *
     * @param suiteId 套件 ID
     * @param request 保存请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLifecycle(Long suiteId, SuiteCaseLifecycleSaveRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            // 清空所有配置
            LambdaQueryWrapper<SuiteCaseLifecycle> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(SuiteCaseLifecycle::getSuiteId, suiteId);
            lifecycleMapper.delete(delWrapper);
            return;
        }

        // 查询现有记录
        LambdaQueryWrapper<SuiteCaseLifecycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SuiteCaseLifecycle::getSuiteId, suiteId);
        List<SuiteCaseLifecycle> existing = lifecycleMapper.selectList(wrapper);
        Map<Long, SuiteCaseLifecycle> existingMap = new HashMap<>();
        for (SuiteCaseLifecycle lc : existing) {
            existingMap.put(lc.getCaseId(), lc);
        }

        Set<Long> incomingCaseIds = new HashSet<>();
        for (SuiteCaseLifecycleSaveRequest.LifecycleItem item : request.getItems()) {
            incomingCaseIds.add(item.getCaseId());

            boolean emptySetup = item.getSetupSteps() == null || item.getSetupSteps().trim().isEmpty();
            boolean emptyTeardown = item.getTeardownSteps() == null || item.getTeardownSteps().trim().isEmpty();

            if (emptySetup && emptyTeardown) {
                // 删除已有记录（恢复默认）
                SuiteCaseLifecycle lc = existingMap.get(item.getCaseId());
                if (lc != null) {
                    lifecycleMapper.deleteById(lc.getId());
                }
                continue;
            }

            SuiteCaseLifecycle lc = existingMap.get(item.getCaseId());
            if (lc == null) {
                // 新增
                lc = new SuiteCaseLifecycle();
                lc.setSuiteId(suiteId);
                lc.setCaseId(item.getCaseId());
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
        for (SuiteCaseLifecycle lc : existing) {
            if (!incomingCaseIds.contains(lc.getCaseId())) {
                lifecycleMapper.deleteById(lc.getId());
            }
        }
    }

    /**
     * 根据套件和用例 ID 查询生命周期配置
     *
     * @param suiteId 套件 ID
     * @param caseId  用例 ID
     * @return 生命周期配置，不存在则返回 null
     */
    public SuiteCaseLifecycle findBySuiteAndCase(Long suiteId, Long caseId) {
        LambdaQueryWrapper<SuiteCaseLifecycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SuiteCaseLifecycle::getSuiteId, suiteId)
                .eq(SuiteCaseLifecycle::getCaseId, caseId);
        return lifecycleMapper.selectOne(wrapper);
    }
}
