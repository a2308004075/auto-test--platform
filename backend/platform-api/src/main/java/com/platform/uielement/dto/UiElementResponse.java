/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 界面元素响应 DTO
 */
package com.platform.uielement.dto;

import lombok.Data;

/**
 * 界面元素响应
 */
@Data
public class UiElementResponse {

    private Long id;

    private Long repositoryId;

    /**
     * 源码文件相对路径（相对仓库根目录）
     */
    private String filePath;

    /**
     * 元素标签名
     */
    private String elementTag;

    private String elementId;

    private String elementName;

    private String elementClass;

    private String elementText;

    private String elementPlaceholder;

    /**
     * 元素 type 属性值（input 等使用）
     */
    private String elementType;

    /**
     * 智能 XPath（语义化定位）
     */
    private String smartXPath;

    /**
     * 绝对 XPath（文档根完整路径）
     */
    private String absoluteXPath;

    /**
     * 元素在文件内的出现顺序号
     */
    private Integer sortNo;
}
