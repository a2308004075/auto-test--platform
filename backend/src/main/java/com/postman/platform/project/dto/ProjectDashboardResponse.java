package com.postman.platform.project.dto;

import lombok.Data;

import java.util.List;

/**
 * 项目仪表板响应
 */
@Data
public class ProjectDashboardResponse {

    private String projectId;
    private String projectName;
    private DashboardStats stats;
    private List<RecentExecution> recentExecutions;
}
