/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 接口管理服务
 */
package com.platform.apidoc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.apidoc.dto.*;
import com.platform.apidoc.entity.Api;
import com.platform.apidoc.mapper.ApiMapper;
import com.platform.apidoc.util.SwaggerParser;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.environment.service.EnvironmentService;
import com.platform.keyword.entity.ApiKeyword;
import com.platform.keyword.entity.Keyword;
import com.platform.keyword.mapper.ApiKeywordMapper;
import com.platform.keyword.mapper.KeywordMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 接口文档管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiService {

    private final ApiMapper apiMapper;
    private final ApiKeywordMapper apiKeywordMapper;
    private final KeywordMapper keywordMapper;
    private final ProjectService projectService;
    private final EnvironmentService environmentService;
    private final ApiModuleService apiModuleService;

    /**
     * 分页查询接口列表
     */
    public PageResponse<ApiInfoResponse> list(Long projectId, Long moduleId, String keyword,
                                               String httpMethod, int page, int pageSize) {
        LambdaQueryWrapper<Api> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Api::getProjectId, projectId);

        if (moduleId != null) {
            // 查询该分组及其所有子孙分组的接口
            Set<Long> moduleIds = apiModuleService.getDescendantModuleIds(moduleId);
            wrapper.in(Api::getModuleId, moduleIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Api::getName, keyword)
                    .or().like(Api::getPath, keyword)
                    .or().like(Api::getService, keyword));
        }
        if (StringUtils.hasText(httpMethod)) {
            wrapper.eq(Api::getHttpMethod, httpMethod);
        }
        wrapper.orderByDesc(Api::getCreatedAt);

        Page<Api> pageParam = new Page<>(page, pageSize);
        Page<Api> result = apiMapper.selectPage(pageParam, wrapper);

        List<ApiInfoResponse> records = new ArrayList<>();
        for (Api api : result.getRecords()) {
            records.add(toResponse(api));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 创建接口
     */
    public ApiInfoResponse create(ApiCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        Api api = new Api();
        BeanUtils.copyProperties(request, api);
        api.setSourceType("MANUAL");

        apiMapper.insert(api);
        return toResponse(api);
    }

    /**
     * 更新接口
     */
    public ApiInfoResponse update(Long apiId, ApiUpdateRequest request) {
        Api api = findById(apiId);

        if (request.getModuleId() != null) {
            api.setModuleId(request.getModuleId());
        }
        if (StringUtils.hasText(request.getName())) {
            api.setName(request.getName());
        }
        if (request.getService() != null) {
            api.setService(request.getService());
        }
        if (StringUtils.hasText(request.getHttpMethod())) {
            api.setHttpMethod(request.getHttpMethod());
        }
        if (StringUtils.hasText(request.getPath())) {
            api.setPath(request.getPath());
        }
        if (request.getRequestParams() != null) {
            api.setRequestParams(request.getRequestParams());
        }
        if (request.getRequestBody() != null) {
            api.setRequestBody(request.getRequestBody());
        }
        if (request.getResponseBody() != null) {
            api.setResponseBody(request.getResponseBody());
        }
        if (request.getHeaders() != null) {
            api.setHeaders(request.getHeaders());
        }
        if (request.getDescription() != null) {
            api.setDescription(request.getDescription());
        }

        apiMapper.updateById(api);
        return toResponse(api);
    }

    /**
     * 获取接口详情
     */
    public ApiInfoResponse getById(Long apiId) {
        return toResponse(findById(apiId));
    }

    /**
     * 删除接口
     */
    public void delete(Long apiId) {
        findById(apiId);
        // 删除保护检查 - 被 ApiKeyword 引用时不可删除
        LambdaQueryWrapper<ApiKeyword> kwWrapper = new LambdaQueryWrapper<>();
        kwWrapper.eq(ApiKeyword::getApiId, apiId);
        long refCount = apiKeywordMapper.selectCount(kwWrapper);
        if (refCount > 0) {
            throw new BusinessException(ErrorCode.API_DEPENDENCY_CONFLICT,
                    "接口被 " + refCount + " 个关键字引用，无法删除");
        }
        apiMapper.deleteById(apiId);
    }

    /**
     * 批量删除接口
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> apiIds) {
        for (Long apiId : apiIds) {
            delete(apiId);
        }
    }

    /**
     * 批量移动接口到指定分组
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchMove(List<Long> apiIds, Long targetModuleId) {
        for (Long apiId : apiIds) {
            Api api = findById(apiId);
            api.setModuleId(targetModuleId);
            apiMapper.updateById(api);
        }
    }

    /**
     * Swagger 导入（增量）
     */
    @Transactional(rollbackFor = Exception.class)
    public SwaggerImportResult importSwagger(SwaggerImportRequest request) {
        projectService.findActiveById(request.getProjectId());

        SwaggerParser.ParseResult parseResult = SwaggerParser.parse(request.getSwaggerJson());
        List<SwaggerParser.ApiEntry> entries = parseResult.getApis();

        int created = 0;
        int updated = 0;
        int skipped = 0;

        // 查询该分组下已有的 Swagger 导入接口
        Map<String, Api> existingByOpId = new HashMap<>();
        LambdaQueryWrapper<Api> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Api::getModuleId, request.getModuleId())
                .eq(Api::getSourceType, "SWAGGER_IMPORT")
                .isNotNull(Api::getSwaggerOperationId);
        List<Api> existing = apiMapper.selectList(wrapper);
        for (Api api : existing) {
            if (api.getSwaggerOperationId() != null) {
                existingByOpId.put(api.getSwaggerOperationId(), api);
            }
        }

        for (SwaggerParser.ApiEntry entry : entries) {
            String opId = entry.getOperationId();

            if (opId != null && existingByOpId.containsKey(opId)) {
                // 更新已有接口
                Api existingApi = existingByOpId.get(opId);
                existingApi.setName(entry.getName());
                existingApi.setHttpMethod(entry.getHttpMethod());
                existingApi.setPath(entry.getPath());
                existingApi.setService(entry.getService());
                existingApi.setRequestParams(entry.getRequestParams());
                existingApi.setRequestBody(entry.getRequestBody());
                existingApi.setResponseBody(entry.getResponseBody());
                existingApi.setHeaders(entry.getHeaders());
                existingApi.setDescription(entry.getDescription());
                apiMapper.updateById(existingApi);
                updated++;
            } else {
                // 创建新接口
                Api newApi = SwaggerParser.toApiEntity(entry, request.getProjectId(), request.getModuleId());
                apiMapper.insert(newApi);
                created++;
            }
        }

        return SwaggerImportResult.of(entries.size(), created, updated, skipped);
    }

    /**
     * 接口调试
     */
    public ApiDebugResponse debug(Long apiId, ApiDebugRequest request) {
        Api api = findById(apiId);

        // 获取环境变量
        Map<String, String> envVars = environmentService.getVariablesAsMap(request.getEnvironmentId());
        if (envVars.isEmpty()) {
            return ApiDebugResponse.error("环境不存在或没有配置变量");
        }

        // 从变量中构建 baseUrl（查找 host 变量）
        String baseUrl = envVars.getOrDefault("host", "");

        String path = api.getPath();
        // 替换路径参数
        if (request.getPathParams() != null) {
            for (Map.Entry<String, String> entry : request.getPathParams().entrySet()) {
                path = path.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        // 拼接查询参数
        StringBuilder urlBuilder = new StringBuilder(baseUrl).append(path);
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            urlBuilder.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : request.getQueryParams().entrySet()) {
                if (!first) {
                    urlBuilder.append("&");
                }
                urlBuilder.append(URLEncoder.encode(entry.getKey())).append("=")
                        .append(URLEncoder.encode(entry.getValue()));
                first = false;
            }
        }

        long start = System.currentTimeMillis();
        try {
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(api.getHttpMethod());
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            // 设置请求头
            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            conn.setRequestProperty("Content-Type", "application/json");

            // 发送请求体
            if (request.getBody() != null && !request.getBody().isEmpty()) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(request.getBody().getBytes(StandardCharsets.UTF_8));
                }
            }

            // 读取响应
            int statusCode = conn.getResponseCode();
            InputStream is = (statusCode >= 200 && statusCode < 400)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String responseBody = readStream(is);
            long elapsed = System.currentTimeMillis() - start;

            ApiDebugResponse response = new ApiDebugResponse();
            response.setStatusCode(statusCode);
            response.setStatusText(conn.getResponseMessage());
            response.setResponseBody(responseBody);
            response.setResponseTimeMs(elapsed);
            response.setResponseSizeBytes((long) responseBody.getBytes(StandardCharsets.UTF_8).length);
            response.setSuccess(statusCode >= 200 && statusCode < 400 ? 1 : 0);

            // 收集响应头
            Map<String, String> respHeaders = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> header : conn.getHeaderFields().entrySet()) {
                if (header.getKey() != null) {
                    respHeaders.put(header.getKey(), String.join(", ", header.getValue()));
                }
            }
            response.setResponseHeaders(respHeaders);

            return response;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("接口调试失败 [{}]: {}", api.getName(), e.getMessage());
            ApiDebugResponse response = ApiDebugResponse.error(e.getMessage());
            response.setResponseTimeMs(elapsed);
            return response;
        }
    }

    /**
     * 查询接口被关键字引用的关系
     */
    public List<ApiReferenceResponse> getReferences(Long apiId) {
        // 查询绑定了此接口的 api_keyword 记录
        LambdaQueryWrapper<ApiKeyword> kwWrapper = new LambdaQueryWrapper<>();
        kwWrapper.eq(ApiKeyword::getApiId, apiId);
        List<ApiKeyword> apiKeywords = apiKeywordMapper.selectList(kwWrapper);

        List<ApiReferenceResponse> result = new ArrayList<>();
        for (ApiKeyword ak : apiKeywords) {
            Keyword kw = keywordMapper.selectById(ak.getKeywordId());
            if (kw != null) {
                ApiReferenceResponse resp = new ApiReferenceResponse();
                resp.setKeywordId(kw.getId());
                resp.setKeywordName(kw.getName());
                resp.setKeywordType(kw.getType());
                resp.setCategory(kw.getCategory());
                resp.setReferenceCount(0);
                result.add(resp);
            }
        }
        return result;
    }

    // ───────────────────── 私有方法 ─────────────────────

    private Api findById(Long apiId) {
        Api api = apiMapper.selectById(apiId);
        if (api == null) {
            throw new BusinessException(ErrorCode.API_NOT_FOUND, "接口不存在：" + apiId);
        }
        return api;
    }

    private ApiInfoResponse toResponse(Api api) {
        ApiInfoResponse response = new ApiInfoResponse();
        BeanUtils.copyProperties(api, response);
        return response;
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
