/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 仓库拉取结果响应 DTO
 */
package com.platform.repository.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库拉取结果响应
 *
 * <p>拉取失败也返回 HTTP 200，通过 success=false 与 message 描述失败原因。
 */
@Data
public class PullResultResponse {

    private Long logId;

    /**
     * 拉取是否成功
     */
    private Boolean success;

    /**
     * 拉取类型：CLONE-首次克隆，PULL-增量更新
     */
    private String pullType;

    private String branch;

    private String commitId;

    private String message;

    private Long durationMs;

    private LocalDateTime finishedAt;
}
