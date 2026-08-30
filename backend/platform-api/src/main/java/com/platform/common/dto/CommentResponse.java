/**
 * @author HXN
 * @date 2026-08-30
 * @description 评论响应
 */
package com.platform.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论响应
 */
@Data
public class CommentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论 ID
     */
    private Long id;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务对象 ID
     */
    private Long bizId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论 ID
     */
    private Long parentId;

    /**
     * 评论人 ID
     */
    private Long createdBy;

    /**
     * 评论人显示名称
     */
    private String createdByName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 子评论（回复列表）
     */
    private List<CommentResponse> children;
}
