package com.platform.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 接口访问日志过滤器
 * <p>
 * 仅记录接口调用报错到 access.log（调用成功不记录，避免日志量过大），
 * 包括：请求方法、地址、查询参数、来源IP、响应状态码、耗时，以及请求体/响应体（敏感字段自动脱敏）。
 * 报错判定：HTTP 状态码 >= 400，或响应体业务 code != 0（ApiResponse.code）。
 * <p>
 * 访问日志通过独立 logger「com.platform.access」输出（logback-spring.xml 中
 * additivity=false），仅写入 access.log，不进入 all.log，实现日志分类清晰。
 * <p>
 * 说明：
 * <ul>
 *     <li>OPTIONS 预检请求已由 {@link CorsFilter} 在外层拦截，不会进入本过滤器。</li>
 *     <li>WebSocket 升级请求（Upgrade: websocket）跳过包装，避免影响握手。</li>
 *     <li>请求体/响应体超长时自动截断，避免单条日志过大。</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessLogFilter implements Filter {

    /** 访问日志独立 logger，对应 logback-spring.xml 中 com.platform.access */
    private static final Logger accessLog = LoggerFactory.getLogger("com.platform.access");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 敏感字段名（归一化后匹配：小写并去除下划线/横线） */
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "pwd", "pass", "oldpassword", "newpassword", "confirmpassword",
            "token", "secret", "accesstoken", "refreshtoken", "authorization",
            "captcha", "verifycode", "ticket", "credential"
    ));

    /** 单条请求体/响应体最大记录长度（字符），超出截断 */
    private static final int MAX_BODY_LOG_LENGTH = 2000;

    /** 请求体缓存上限（字节），超过则不缓存以避免内存溢出（如文件上传场景） */
    private static final int REQUEST_CACHE_LIMIT = 1024 * 1024;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse)) {
            chain.doFilter(req, res);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // WebSocket 升级请求跳过包装，避免影响握手流程
        String upgrade = request.getHeader("Upgrade");
        if (upgrade != null && "websocket".equalsIgnoreCase(upgrade.trim())) {
            chain.doFilter(request, response);
            return;
        }

        // 限制缓存上限，超过的请求体不再缓存，防止大文件上传导致内存溢出
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            try {
                logAccess(wrappedRequest, wrappedResponse, duration);
            } catch (Exception e) {
                accessLog.warn("访问日志记录失败: {}", e.getMessage());
            } finally {
                // 必须将缓存的响应体写回客户端
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    /**
     * 记录接口访问日志（仅报错时记录）
     */
    private void logAccess(ContentCachingRequestWrapper request,
                           ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        byte[] responseBytes = response.getContentAsByteArray();

        // 报错判定：HTTP 状态码 >= 400 直接算报错；否则按响应体业务 code 判定（code != 0 为业务失败）
        boolean error;
        if (status >= 400) {
            error = true;
        } else {
            Integer bizCode = extractBizCode(responseBytes);
            error = (bizCode != null && bizCode != 0);
        }
        if (!error) {
            // 调用成功，不记录
            return;
        }

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String ip = getClientIp(request);
        String requestBody = resolveRequestBody(request);
        String responseBody = maskBody(responseBytes);

        StringBuilder sb = new StringBuilder("接口调用报错 ");
        sb.append("[方法=").append(method).append("] ");
        sb.append("[地址=").append(uri).append("] ");
        if (query != null && !query.isEmpty()) {
            sb.append("[查询参数=").append(maskQuery(query)).append("] ");
        }
        sb.append("[来源IP=").append(ip).append("] ");
        sb.append("[状态码=").append(status).append("] ");
        sb.append("[耗时=").append(duration).append("毫秒] ");
        if (requestBody != null) {
            sb.append("[请求体=").append(requestBody).append("] ");
        }
        if (responseBody != null) {
            sb.append("[响应体=").append(responseBody).append("]");
        }
        String message = sb.toString().trim();
        // 服务端异常用 error，客户端错误/业务失败用 warn
        if (status >= 500) {
            accessLog.error(message);
        } else {
            accessLog.warn(message);
        }
    }

    /**
     * 解析请求体：声明长度超过缓存上限时给出提示，否则脱敏后返回
     */
    private String resolveRequestBody(ContentCachingRequestWrapper request) {
        long declaredLength = request.getContentLengthLong();
        if (declaredLength > REQUEST_CACHE_LIMIT) {
            return "(请求体过大,已跳过)";
        }
        return maskBody(request.getContentAsByteArray());
    }

    /**
     * 从响应体解析业务 code（ApiResponse.code）；非 JSON 或无 code 字段返回 null
     */
    private Integer extractBizCode(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(new String(body, StandardCharsets.UTF_8));
            JsonNode codeNode = node.get("code");
            if (codeNode != null && codeNode.isInt()) {
                return codeNode.asInt();
            }
        } catch (Exception e) {
            // 非 JSON 或解析失败，返回 null
        }
        return null;
    }

    /**
     * 获取客户端真实IP（穿透常见反向代理头）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isBlank(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 请求体/响应体脱敏并截断
     */
    private String maskBody(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }
        String raw = new String(body, StandardCharsets.UTF_8).trim();
        if (raw.isEmpty()) {
            return null;
        }
        String masked = maskJson(raw);
        return truncate(masked);
    }

    /**
     * 对 JSON 文本递归脱敏敏感字段；非 JSON 原样返回
     */
    private String maskJson(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            maskNode(node);
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            // 非 JSON 或解析失败，原样返回（截断由上层处理）
            return json;
        }
    }

    /**
     * 递归脱敏：对象字段中命中敏感名的字符串值替换为 ***
     */
    private void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            obj.fieldNames().forEachRemaining(fieldNames::add);
            for (String name : fieldNames) {
                JsonNode child = obj.get(name);
                if (child != null && child.isTextual() && isSensitive(name)) {
                    obj.set(name, TextNode.valueOf("***"));
                } else {
                    maskNode(child);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                maskNode(element);
            }
        }
    }

    /**
     * 对查询字符串中的敏感参数值脱敏
     */
    private String maskQuery(String query) {
        StringBuilder sb = new StringBuilder();
        for (String pair : query.split("&")) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                if (isSensitive(key)) {
                    value = "***";
                }
                sb.append(key).append("=").append(value);
            } else {
                sb.append(pair);
            }
        }
        return truncate(sb.toString());
    }

    private boolean isSensitive(String key) {
        return key != null && SENSITIVE_KEYS.contains(normalizeKey(key));
    }

    private String normalizeKey(String key) {
        return key.toLowerCase().replace("_", "").replace("-", "");
    }

    private String truncate(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= MAX_BODY_LOG_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_BODY_LOG_LENGTH)
                + "...(已截断,共" + content.length() + "字符)";
    }

    private boolean isBlank(String s) {
        return s == null || s.isEmpty() || "unknown".equalsIgnoreCase(s);
    }
}
