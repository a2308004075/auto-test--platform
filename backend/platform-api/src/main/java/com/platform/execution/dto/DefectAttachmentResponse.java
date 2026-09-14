/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷附件响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷附件响应
 */
@Data
public class DefectAttachmentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long defectId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
