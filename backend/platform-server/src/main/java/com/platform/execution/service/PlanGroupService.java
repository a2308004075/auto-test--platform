/**
 * @author HXN
 * @date 2026-08-23
 * @description 测试计划分组服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.dto.PlanGroupRequest;
import com.platform.execution.dto.PlanGroupResponse;
import com.platform.execution.entity.PlanGroup;
import com.platform.execution.entity.TestPlan;
import com.platform.execution.mapper.PlanGroupMapper;
import com.platform.execution.mapper.TestPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试计划分组服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanGroupService {

    private final PlanGroupMapper planGroupMapper;
    private final TestPlanMapper testPlanMapper;

    /**
     * 查询项目下的分组列表（扁平结构，前端自行组装树）
     */
    public List<PlanGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<PlanGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanGroup::getProjectId, projectId)
                .orderByAsc(PlanGroup::getSortOrder)
                .orderByAsc(PlanGroup::getId);

        List<PlanGroup> groups = planGroupMapper.selectList(wrapper);
        return groups.stream().map(g -> {
            PlanGroupResponse resp = new PlanGroupResponse();
            BeanUtils.copyProperties(g, resp);
            resp.setPlanCount(countPlansInGroup(g.getId(), projectId));
            return resp;
        }).collect(Collectors.toList());
    }

    /**
     * 创建分组
     */
    @Transactional(rollbackFor = Exception.class)
    public PlanGroupResponse create(Long projectId, PlanGroupRequest request) {
        // 名称唯一性检查（同层级）
        LambdaQueryWrapper<PlanGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanGroup::getProjectId, projectId)
                .eq(PlanGroup::getName, request.getName())
                .eq(PlanGroup::getParentId, request.getParentId());
        if (planGroupMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "分组名称已存在：" + request.getName());
        }

        // 校验父分组存在性
        if (request.getParentId() != null) {
            PlanGroup parent = planGroupMapper.selectById(request.getParentId());
            if (parent == null) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "父分组不存在");
            }
            if (!parent.getProjectId().equals(projectId)) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "父分组不属于当前项目");
            }
        }

        PlanGroup group = new PlanGroup();
        group.setProjectId(projectId);
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setParentId(request.getParentId());
        group.setSortOrder(0);
        planGroupMapper.insert(group);

        PlanGroupResponse resp = new PlanGroupResponse();
        BeanUtils.copyProperties(group, resp);
        resp.setPlanCount(0);
        return resp;
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public PlanGroupResponse update(Long groupId, PlanGroupRequest request) {
        PlanGroup group = planGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "分组不存在");
        }

        // 名称唯一性检查（同层级）
        LambdaQueryWrapper<PlanGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanGroup::getProjectId, group.getProjectId())
                .eq(PlanGroup::getName, request.getName())
                .eq(PlanGroup::getParentId, request.getParentId())
                .ne(PlanGroup::getId, groupId);
        if (planGroupMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "分组名称已存在：" + request.getName());
        }

        // 防止将自己设为自己的父分组
        if (request.getParentId() != null && request.getParentId().equals(groupId)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "不能将分组设为自身的子分组");
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        if (request.getParentId() != null) {
            group.setParentId(request.getParentId());
        }
        planGroupMapper.updateById(group);

        PlanGroupResponse resp = new PlanGroupResponse();
        BeanUtils.copyProperties(group, resp);
        resp.setPlanCount(countPlansInGroup(groupId, group.getProjectId()));
        return resp;
    }

    /**
     * 删除分组（级联删除子分组，计划 group_id SET NULL）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        PlanGroup group = planGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "分组不存在");
        }

        // 获取所有后代分组 ID（含自身）
        List<Long> descendantIds = getDescendantIds(groupId);

        // 将这些分组下的计划设为未分组
        LambdaQueryWrapper<TestPlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.in(TestPlan::getGroupId, descendantIds);
        List<TestPlan> affectedPlans = testPlanMapper.selectList(planWrapper);
        for (TestPlan plan : affectedPlans) {
            plan.setGroupId(null);
            testPlanMapper.updateById(plan);
        }

        // 删除所有后代分组（从子到父逆序删除，避免外键约束冲突）
        for (int i = descendantIds.size() - 1; i >= 0; i--) {
            planGroupMapper.deleteById(descendantIds.get(i));
        }

        log.info("已删除分组 {} 及其 {} 个子分组，{} 个计划已归入未分组",
                groupId, descendantIds.size() - 1, affectedPlans.size());
    }

    /**
     * 获取分组及其所有后代 ID 列表
     */
    private List<Long> getDescendantIds(Long groupId) {
        List<Long> ids = new ArrayList<>();
        ids.add(groupId);

        LambdaQueryWrapper<PlanGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanGroup::getParentId, groupId);
        List<PlanGroup> children = planGroupMapper.selectList(wrapper);
        for (PlanGroup child : children) {
            ids.addAll(getDescendantIds(child.getId()));
        }
        return ids;
    }

    /**
     * 统计分组下的计划数量（含子分组递归）
     */
    private int countPlansInGroup(Long groupId, Long projectId) {
        List<Long> allGroupIds = getDescendantIds(groupId);
        LambdaQueryWrapper<TestPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestPlan::getProjectId, projectId)
                .in(TestPlan::getGroupId, allGroupIds);
        return Math.toIntExact(testPlanMapper.selectCount(wrapper));
    }
}
