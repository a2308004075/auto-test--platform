package com.platform.project.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目响应
 */
@Data
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String sourcePath;
    /** 状态：0-停用，1-启用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── 项目卡片统计字段（列表查询时填充） ──

    private Long apiCount = 0L;
    private Long keywordCount = 0L;
    private Long actionCount = 0L;
    private Long caseCount = 0L;
    private Long suiteCount = 0L;
    private Long planCount = 0L;
}
