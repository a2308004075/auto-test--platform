/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description ProjectDashboard 响应 DTO
 */
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
    /** 状态：0-停用，1-启用 */
    private Integer status;
    private DashboardStats stats;
    private DashboardTrendResponse trend;
    private List<RecentExecution> recentExecutions;
}
