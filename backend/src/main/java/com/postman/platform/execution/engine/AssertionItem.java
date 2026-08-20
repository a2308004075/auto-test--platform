package com.postman.platform.execution.engine;

import lombok.Data;

/**
 * 断言定义模型
 *
 * <p>用于验证关键字执行结果是否符合预期。
 */
@Data
public class AssertionItem {

    /**
     * 断言类型：STATUS_CODE / RESPONSE_BODY / JSON_PATH / HEADER / RESPONSE_TIME
     */
    private String type;

    /**
     * 断言字段（如 JSONPath 表达式、Header 名）
     */
    private String field;

    /**
     * 操作符：eq / ne / contains / not_contains / gt / lt / ge / le / exists / not_exists
     */
    private String operator;

    /**
     * 期望值
     */
    private Object expected;
}
