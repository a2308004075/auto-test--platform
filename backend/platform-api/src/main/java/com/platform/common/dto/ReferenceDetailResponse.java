/**
 * @author HXN
 * @date 2026-08-23
 * @description 引用关系详情响应 DTO
 */
package com.platform.common.dto;

import lombok.Data;

/**
 * 引用关系详情（关键字/工具方法被哪些 Action 或自动化用例引用）
 */
@Data
public class ReferenceDetailResponse {

    /**
     * 引用类型：ACTION / AUTO_CASE
     */
    private String refType;

    /**
     * 引用者 ID（Action ID 或 AutoCase ID）
     */
    private Long refId;

    /**
     * 引用者名称
     */
    private String refName;

    /**
     * 引用者描述
     */
    private String refDescription;
}
