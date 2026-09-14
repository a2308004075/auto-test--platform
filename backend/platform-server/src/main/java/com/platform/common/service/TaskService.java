/**
 * @author HXN
 * @date 2026-08-30
 * @description 通用任务服务
 */
package com.platform.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.auth.mapper.UserMapper;
import com.platform.common.dto.TaskCreateRequest;
import com.platform.common.dto.TaskResponse;
import com.platform.common.dto.TaskUpdateRequest;
import com.platform.common.entity.Task;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 通用任务服务
 *
 * <p>支持多种任务类型（需求评审、用例评审、缺陷处理等）的统一管理。
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;

    private static final Set<String> PENDING_STATUSES = new HashSet<>(Arrays.asList("PENDING", "IN_PROGRESS"));

    /**
     * 查询我的任务列表（支持按类型/状态筛选）
     */
    public List<TaskResponse> listMyTasks(Long userId, String taskType, String status) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getAssigneeId, userId);
        if (StringUtils.hasText(taskType)) {
            wrapper.eq(Task::getTaskType, taskType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Task::getStatus, status);
        }
        wrapper.orderByDesc(Task::getCreatedAt);
        List<Task> tasks = taskMapper.selectList(wrapper);

        Map<Long, User> userCache = new HashMap<>();
        List<TaskResponse> result = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            result.add(toResponse(task, userCache));
        }
        return result;
    }

    /**
     * 统计待完成任务数（PENDING + IN_PROGRESS）
     */
    public long countMyPendingTasks(Long userId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getAssigneeId, userId)
                .in(Task::getStatus, PENDING_STATUSES);
        return taskMapper.selectCount(wrapper);
    }

    /**
     * 创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse createTask(TaskCreateRequest request) {
        Task task = new Task();
        BeanUtils.copyProperties(request, task);
        task.setStatus("PENDING");
        task.setCreatedBy(getCurrentUserId());
        if (!StringUtils.hasText(task.getPriority())) {
            task.setPriority("中");
        }
        taskMapper.insert(task);

        Map<Long, User> userCache = new HashMap<>();
        return toResponse(task, userCache);
    }

    /**
     * 更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }

        if (StringUtils.hasText(request.getTitle())) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getStatus())) {
            task.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPriority())) {
            task.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            task.setAssigneeId(request.getAssigneeId());
        }
        if (request.getBizType() != null) {
            task.setBizType(request.getBizType());
        }
        if (request.getBizId() != null) {
            task.setBizId(request.getBizId());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        taskMapper.updateById(task);

        Map<Long, User> userCache = new HashMap<>();
        return toResponse(task, userCache);
    }

    /**
     * 删除任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        taskMapper.deleteById(taskId);
    }

    private TaskResponse toResponse(Task task, Map<Long, User> userCache) {
        TaskResponse resp = new TaskResponse();
        BeanUtils.copyProperties(task, resp);

        if (task.getAssigneeId() != null) {
            User user = userCache.computeIfAbsent(task.getAssigneeId(), userMapper::selectById);
            if (user != null) {
                resp.setAssigneeName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            }
        }
        if (task.getCreatedBy() != null) {
            User user = userCache.computeIfAbsent(task.getCreatedBy(), userMapper::selectById);
            if (user != null) {
                resp.setCreatedByName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            }
        }
        return resp;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }
}
