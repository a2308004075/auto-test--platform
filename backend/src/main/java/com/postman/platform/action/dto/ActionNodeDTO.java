package com.postman.platform.action.dto;

import lombok.Data;

@Data
public class ActionNodeDTO {

    private String nodeKey;
    private String nodeType;
    private String refKeywordId;
    private String refToolId;
    private String config;
    private Integer positionX;
    private Integer positionY;
}
