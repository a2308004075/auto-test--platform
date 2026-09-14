/**
 * @author HXN
 * @date 2026-08-30
 * @description 通用任务控制器
 */
package com.platform.common.controller;

import com.platform.common.dto.TaskCreateRequest;
import com.platform.common.dto.TaskResponse;
import com.platform.common.dto.TaskUpdateRequest;
import com.platform.common.response.ApiResponse;
import com.platform.common.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 通用任务接口
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 查询我的任务列表
     */
    @GetMapping("/my-tasks")
    public ApiResponse<List<TaskResponse>> myTasks(@RequestParam Long userId,
                                                    @RequestParam(required = false) String taskType,
                                                    @RequestParam(required = false) String status) {
        return ApiResponse.ok(taskService.listMyTasks(userId, taskType, status));
    }

    /**
     * 统计待完成任务数
     */
    @GetMapping("/my-tasks/count")
    public ApiResponse<Long> myTaskCount(@RequestParam Long userId) {
        return ApiResponse.ok(taskService.countMyPendingTasks(userId));
    }

    /**
     * 创建任务
     */
    @PostMapping
    public ApiResponse<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.ok(taskService.createTask(request));
    }

    /**
     * 更新任务
     */
    @PostMapping("/{id}")
    public ApiResponse<TaskResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody TaskUpdateRequest request) {
        return ApiResponse.ok(taskService.updateTask(id, request));
    }

    /**
     * 删除任务
     */
    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ApiResponse.ok();
    }
}
