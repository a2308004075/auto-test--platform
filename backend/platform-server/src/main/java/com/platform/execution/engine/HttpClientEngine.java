/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description HTTP 客户端引擎
 */
package com.platform.execution.engine;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 请求执行引擎
 *
 * <p>基于 OkHttp 构建，负责发送 API 请求并收集响应。
 * 支持变量替换（从 ExecutionContext 获取 ${var} 值）。
 */
@Component
@Slf4j
public class HttpClientEngine {

    private final OkHttpClient client;

    public HttpClientEngine() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 执行 HTTP 请求
     *
     * @param method HTTP 方法（GET/POST/PUT/PATCH/DELETE）
     * @param path   请求路径（不含 baseUrl，支持 ${var}）
     * @param headers 请求头
     * @param body   请求体（POST/PUT/PATCH 时有效）
     * @param context 执行上下文
     * @return 步骤执行结果（含请求和响应详情）
     */
    public StepResult execute(String method, String path,
                              Map<String, String> headers, String body,
                              ExecutionContext context) {
        StepResult result = new StepResult();
        long start = System.currentTimeMillis();

        // 构建 URL（baseUrl + path + 变量替换）
        String baseUrl = context.getBaseUrl() != null ? context.getBaseUrl() : "";
        String fullUrl = context.resolveVariables(baseUrl + path);

        // 构建请求头
        Map<String, String> resolvedHeaders = new LinkedHashMap<>();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                resolvedHeaders.put(entry.getKey(), context.resolveVariables(entry.getValue()));
            }
        }

        // 记录请求详情
        Map<String, Object> reqDetail = new LinkedHashMap<>();
        reqDetail.put("method", method);
        reqDetail.put("url", fullUrl);
        reqDetail.put("headers", resolvedHeaders);
        reqDetail.put("body", body);

        try {
            Request.Builder reqBuilder = new Request.Builder().url(fullUrl);

            // 设置请求头
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            for (Map.Entry<String, String> entry : resolvedHeaders.entrySet()) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            // 根据方法设置请求体
            String methodUpper = method.toUpperCase();
            if ("GET".equals(methodUpper) || "DELETE".equals(methodUpper)) {
                reqBuilder.method(methodUpper, null);
            } else {
                String resolvedBody = body != null ? context.resolveVariables(body) : "";
                RequestBody reqBody = RequestBody.create(resolvedBody, mediaType);
                reqBuilder.method(methodUpper, reqBody);
            }

            // 发送请求
            try (Response response = client.newCall(reqBuilder.build()).execute()) {
                long elapsed = System.currentTimeMillis() - start;
                String respBody = response.body() != null ? response.body().string() : "";

                // 记录响应详情
                Map<String, Object> respDetail = new LinkedHashMap<>();
                respDetail.put("statusCode", response.code());
                respDetail.put("headers", response.headers().toMultimap());
                respDetail.put("body", respBody);
                respDetail.put("durationMs", elapsed);

                result.setRequest(reqDetail);
                result.setResponse(respDetail);
                result.setDurationMs(elapsed);

                if (response.isSuccessful()) {
                    result.setStatus("PASSED");
                    result.setMessage("HTTP " + response.code() + " 响应成功");
                } else {
                    result.setStatus("FAILED");
                    result.setMessage("HTTP " + response.code() + " 响应失败");
                }
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("HTTP 请求异常: {} {}", method, fullUrl, e);
            result.setStatus("ERROR");
            result.setMessage("请求异常：" + e.getMessage());
            result.setRequest(reqDetail);
            result.setDurationMs(elapsed);
        }

        return result;
    }
}
