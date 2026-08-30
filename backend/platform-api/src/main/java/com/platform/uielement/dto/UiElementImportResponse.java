/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 界面元素导入结果响应 DTO
 */
package com.platform.uielement.dto;

import lombok.Data;

/**
 * 界面元素导入结果响应
 *
 * <p>导入为覆盖式：同仓库重新导入会先删除旧解析结果再全量重建。
 */
@Data
public class UiElementImportResponse {

    private Long repositoryId;

    /**
     * 仓库名称
     */
    private String repositoryName;

    /**
     * 成功解析的源码文件数（含无交互元素的文件）
     */
    private Integer fileCount;

    /**
     * 解析出的元素总数
     */
    private Integer elementCount;

    /**
     * 解析失败的文件数
     */
    private Integer failedFileCount;

    /**
     * 是否因超出文件数上限被截断
     */
    private Boolean truncated;

    /**
     * 导入耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 概要信息
     */
    private String message;
}
