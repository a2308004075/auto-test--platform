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
