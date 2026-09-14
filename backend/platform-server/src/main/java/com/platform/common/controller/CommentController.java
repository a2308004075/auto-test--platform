/**
 * @author HXN
 * @date 2026-08-30
 * @description 评论控制器
 */
package com.platform.common.controller;

import com.platform.common.dto.CommentCreateRequest;
import com.platform.common.dto.CommentResponse;
import com.platform.common.response.ApiResponse;
import com.platform.common.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 评论接口
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 查询业务对象下的评论列表
     */
    @GetMapping("/comments")
    public ApiResponse<List<CommentResponse>> list(@RequestParam String bizType,
                                                    @RequestParam Long bizId) {
        return ApiResponse.ok(commentService.listByBiz(bizType, bizId));
    }

    /**
     * 发表评论/回复
     */
    @PostMapping("/comments")
    public ApiResponse<CommentResponse> create(@Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.ok(commentService.create(request));
    }

    /**
     * 删除评论
     */
    @PostMapping("/comments/{id}/delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ApiResponse.ok();
    }
}
