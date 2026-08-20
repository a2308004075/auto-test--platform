package com.platform.execution.engine;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 步骤树节点模型
 *
 * <p>从 test_case.steps / setup_steps / teardown_steps 的 JSON 数组反序列化。
 * 每个节点引用一个关键字，携带参数和断言。
 */
@Data
public class StepNode {

    /**
     * 关键字 ID
     */
    private Long keywordId;

    /**
     * 步骤名称
     */
    private String name;

    /**
     * 步骤参数
     */
    private Map<String, Object> params;

    /**
     * 断言列表
     */
    private List<AssertionItem> assertions;

    /**
     * 嵌套子步骤（可选，用于条件/循环等复合节点）
     */
    private List<StepNode> children;
}
