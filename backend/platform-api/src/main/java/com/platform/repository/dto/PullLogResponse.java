/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 仓库拉取历史响应 DTO
 */
package com.platform.repository.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库拉取历史响应
 */
@Data
public class PullLogResponse {

    private Long id;

    /**
     * 拉取类型：CLONE-首次克隆，PULL-增量更新
     */
    private String pullType;

    private String branch;

    /**
     * 拉取状态：RUNNING/SUCCESS/FAILED
     */
    private String status;

    private String commitId;

    private String message;

    private Long durationMs;

    private LocalDateTime createdAt;
}
