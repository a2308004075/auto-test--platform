package com.postman.platform.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体基类
 *
 * <p>包含绝大多数业务表的公共字段：
 * <ul>
 *   <li>{@code id} - UUID 主键（CHAR(36)，由 MyBatis-Plus assign_uuid 策略生成）</li>
 *   <li>{@code createdAt} - 创建时间（对应数据库 {@code created_at}，DEFAULT CURRENT_TIMESTAMP）</li>
 *   <li>{@code updatedAt} - 更新时间（对应数据库 {@code updated_at}，ON UPDATE CURRENT_TIMESTAMP）</li>
 * </ul>
 *
 * <p>软删除字段 {@code is_active} 由全局配置 {@code logic-delete-field: isActive} 统一处理，
 * 需要软删除的实体自行声明 {@code isActive} 字段并标注 {@code @TableLogic}。
 *
 * <p>特殊表（如 {@code global_settings}、{@code test_result}）字段与基类不一致时可不继承本类。
 */
@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * UUID 主键
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
