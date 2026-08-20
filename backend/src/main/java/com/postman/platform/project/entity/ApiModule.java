package com.postman.platform.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.postman.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接口分组实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_module")
public class ApiModule extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 服务前缀
     */
    private String servicePrefix;

    /**
     * 分组描述
     */
    private String description;

    /**
     * 来源类型：SWAGGER_IMPORT / MANUAL
     */
    private String sourceType;

    /**
     * 导入的 Swagger 文件路径
     */
    private String swaggerFile;

    /**
     * 是否系统默认分组（全部/未分组）
     */
    private Boolean isSystem;
}
