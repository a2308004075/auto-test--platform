package com.platform.action.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Action 节点实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("action_node")
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
