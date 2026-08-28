/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 节点实体类
 */
package com.platform.action.entity;

import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Action 节点 POJO（从 action.nodes JSON 反序列化，无独立数据库表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActionNode extends BaseEntity {

    private Long actionId;

    private String nodeKey;

    /**
     * 节点类型：API_KEYWORD / TOOL_METHOD / CONDITION / LOOP / START / END
     */
    private String nodeType;

    private Long refKeywordId;

    private Long refToolId;

    /**
     * 节点配置（JSON）
     */
    private String config;

    private Integer positionX;

    private Integer positionY;
}
