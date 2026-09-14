/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档实体类
 */
package com.platform.projectdoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目文档实体
 *
 * <p>对应数据库 project_doc 表。文件本体存于本地磁盘（doc.storage-path），
 * 本实体仅保存元数据（文件名、磁盘存储名、大小、类型等）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_doc")
public class ProjectDoc extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 所属分组 ID（null=未分组）
     */
    private Long groupId;

    /**
     * 文档显示名
     */
    private String docName;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 磁盘存储文件名（UUID+扩展名）
     */
    private String storedName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件 MIME 类型
     */
    private String contentType;

    /**
     * 文档描述
     */
    private String description;

    /**
     * 上传人 ID
     */
    private Long createdBy;
}
