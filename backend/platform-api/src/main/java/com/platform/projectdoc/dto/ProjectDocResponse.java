/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档响应 DTO
 */
package com.platform.projectdoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目文档响应
 */
@Data
public class ProjectDocResponse {

    private Long id;
    private Long projectId;
    private Long groupId;
    private String docName;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
