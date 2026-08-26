/**
 * @author HXN
 * @date 2026-08-26
 * @description 接口关键字分组响应 DTO
 */
package com.platform.keyword.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口关键字分组响应
 */
@Data
public class ApiKeywordGroupResponse {

    private Long id;
    private Long projectId;
    private String name;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 分组下关键字数量
     */
    private Integer keywordCount;
}
