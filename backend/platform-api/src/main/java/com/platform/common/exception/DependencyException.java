/**
 * @author HXN
 * @date 2026-08-20 10:05
 * @description 依赖异常类
 */
package com.platform.common.exception;

import lombok.Getter;

/**
 * 依赖冲突异常 - 用于删除保护场景
 *
 * <p>当资源被其他资源引用时拒绝删除，抛出此异常。
 * 错误码 {@link ErrorCode#RESOURCE_CONFLICT}（1007），HTTP 状态码 409。
 *
 * <p>使用示例：
 * <pre>
 *   throw new DependencyException("删除失败：接口仍被关键字引用", dependencyList);
 * </pre>
 */
@Getter
public class DependencyException extends BusinessException {

    /**
     * 引用该资源的依赖项列表（用于前端展示具体冲突项）
     */
    private final Object dependencies;

    public DependencyException(String message, Object dependencies) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
        this.dependencies = dependencies;
    }
}
