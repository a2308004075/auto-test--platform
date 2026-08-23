/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境响应 DTO
 */
package com.platform.environment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境响应
 */
@Data
public class EnvironmentResponse {

    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private Integer isCurrent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 环境变量列表（详情接口返回，列表接口可为 null）
     */
    private List<EnvironmentVariableDTO> variables;
}
