package com.postman.platform.action.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.postman.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Action 节点实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("action_node")
public class ActionNode extends BaseEntity {

    private String actionId;

    private String nodeKey;

    /**
     * 节点类型：API_KEYWORD / TOOL_METHOD / CONDITION / LOOP / START / END
     */
    private String nodeType;

    private String refKeywordId;

    private String refToolId;

    /**
     * 节点配置（JSON）
     */
    private String config;

    private Integer positionX;

    private Integer positionY;
}
