/**
 * @author HXN
 * @date 2026-08-24
 * @description Swagger 同步配置实体类
 */
package com.platform.apidoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Swagger 同步配置实体（对应 api_sync_config 表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_sync_config")
public class ApiSyncConfig extends BaseEntity {

    private Long projectId;

    /** 配置名称 */
    private String name;

    /** OpenAPI/Swagger 文档 URL */
    private String url;

    /** 目标分组 ID */
    private Long moduleId;

    /** 自定义请求头（多行 Key: Value 文本） */
    private String headers;

    /** 最后一次同步时间 */
    private LocalDateTime lastSyncAt;
}
