package com.postman.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全局配置实体
 *
 * <p>字段与 BaseEntity 不一致，不继承 BaseEntity。
 */
@Data
@TableName("global_settings")
public class GlobalSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值（JSON 字符串）
     */
    private String configValue;

    /**
     * 配置说明
     */
    private String description;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
