package com.postman.platform.auth.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全局配置响应
 */
@Data
public class GlobalConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置说明
     */
    private String description;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
