/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷附件实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷附件实体
 *
 * <p>对应数据库 defect_attachment 表。</p>
 */
@Data
@TableName("defect_attachment")
public class DefectAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 缺陷 ID
     */
    private Long defectId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件访问 URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 上传人 ID
     */
    private Long createdBy;

    /**
     * 上传时间
     */
    private LocalDateTime createdAt;
}
