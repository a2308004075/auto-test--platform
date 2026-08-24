/**
 * @author HXN
 * @date 2026-08-24
 * @description Swagger 同步配置响应 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiSyncConfigResponse {

    private Long id;
    private String name;
    private String url;
    private Long moduleId;
    private String moduleName;
    private String headers;
    private String authUsername;
    private String authPassword;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
}
