/**
 * @author HXN
 * @date 2026-08-30
 * @description 评论创建请求
 */
package com.platform.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 评论创建请求
 */
@Data
public class CommentCreateRequest {

    /**
     * 业务类型
     */
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 50, message = "业务类型长度不能超过 50")
    private String bizType;

    /**
     * 业务对象 ID
     */
    @NotNull(message = "业务对象 ID 不能为空")
    private Long bizId;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容长度不能超过 2000")
    private String content;

    /**
     * 父评论 ID（null 表示一级评论）
     */
    private Long parentId;
}
