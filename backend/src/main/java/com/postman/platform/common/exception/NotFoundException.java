package com.postman.platform.common.exception;

/**
 * 资源不存在异常
 *
 * <p>当请求的资源在数据库中不存在时抛出此异常。
 * 错误码 {@link ErrorCode#RESOURCE_NOT_FOUND}（1006），HTTP 状态码 404。
 *
 * <p>使用示例：
 * <pre>
 *   throw new NotFoundException("项目", projectId);
 * </pre>
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, buildMessage(resource, id));
    }

    public NotFoundException(String resource) {
        this(resource, null);
    }

    private static String buildMessage(String resource, Object id) {
        if (id == null) {
            return resource + " 不存在";
        }
        String idStr = id.toString();
        if (idStr.isEmpty()) {
            return resource + " 不存在";
        }
        return resource + " 不存在 (id=" + idStr + ")";
    }
}
