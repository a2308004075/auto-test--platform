package com.platform.environment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境响应
 */
@Data
public class EnvironmentResponse {

    private Long id;
    private Long projectId;
    private String name;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private String dbUrl;
    private String dbUsername;
    private String redisHost;
    private String redisPort;
    private String configJson;
    private Integer isCurrent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
