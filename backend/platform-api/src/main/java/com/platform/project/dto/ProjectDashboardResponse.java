package com.platform.project.dto;

import lombok.Data;

import java.util.List;

/**
 * 项目仪表板响应
 */
@Data
public class ProjectDashboardResponse {

    private Long projectId;
    private String projectName;
    private String projectDescription;
    private Boolean isActive;
    private DashboardStats stats;
    private DashboardTrendResponse trend;
    private List<RecentExecution> recentExecutions;
}
