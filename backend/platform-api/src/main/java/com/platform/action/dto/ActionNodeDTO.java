/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 节点数据传输对象
 */
package com.platform.action.dto;

import lombok.Data;

@Data
public class ActionNodeDTO {

    private String nodeKey;
    private String nodeType;
    private Long refKeywordId;
    private Long refToolId;
    private String config;
    private Integer positionX;
    private Integer positionY;
}
