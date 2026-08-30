/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 界面元素实体类
 */
package com.platform.uielement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 界面元素实体
 *
 * <p>从前端源码（.vue/.html）解析出的交互元素及其 XPath，供 UI 自动化功能调用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ui_element")
public class UiElement extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 来源仓库 ID（code_repository.id）
     */
    private Long repositoryId;

    /**
     * 源码文件相对路径（相对仓库根目录，/ 分隔）
     */
    private String filePath;

    /**
     * 元素标签名（小写）
     */
    private String elementTag;

    /**
     * 元素 id 属性值
     */
    private String elementId;

    /**
     * 元素 name 属性值
     */
    private String elementName;

    /**
     * 元素 class 属性值
     */
    private String elementClass;

    /**
     * 元素文本内容（截断）
     */
    private String elementText;

    /**
     * 元素 placeholder 属性值
     */
    private String elementPlaceholder;

    /**
     * 元素 type 属性值（input 等使用）
     */
    private String elementType;

    /**
     * 智能 XPath（语义化定位，无法语义定位时为绝对路径）
     *
     * <p>连续大写驼峰（smartXPath）默认转换为 smart_x_path，与表列名 smart_xpath 不一致，需显式指定。
     */
    @TableField("smart_xpath")
    private String smartXPath;

    /**
     * 绝对 XPath（文档根完整路径）
     *
     * <p>连续大写驼峰（absoluteXPath）默认转换为 absolute_x_path，与表列名 absolute_xpath 不一致，需显式指定。
     */
    @TableField("absolute_xpath")
    private String absoluteXPath;

    /**
     * 元素在文件内的出现顺序号（从 1 开始）
     */
    private Integer sortNo;
}
