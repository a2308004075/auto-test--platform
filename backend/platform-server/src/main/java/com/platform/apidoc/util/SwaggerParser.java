/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Swagger 文档解析工具类
 */
package com.platform.apidoc.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.apidoc.entity.Api;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * OpenAPI / Swagger 文档解析器
 * 同时支持 Swagger 2.0 和 OpenAPI 3.0，将文档解析为 Api 实体列表
 */
@Slf4j
public class SwaggerParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] HTTP_METHODS = {"get", "post", "put", "delete", "patch"};

    /**
     * 解析结果
     */
    @Data
    public static class ParseResult {
        private List<ApiEntry> apis = new ArrayList<>();
        private String basePath;
        private String title;
        private String version;
    }

    @Data
    public static class ApiEntry {
        private String operationId;
        private String name;
        private String httpMethod;
        private String path;
        private String service;
        private String description;
        private String requestParams;   // JSON
        private String requestBody;     // JSON
        private String responseBody;    // JSON
        private String headers;         // JSON
        private String contentType;    // 默认 Content-Type
    }

    /**
     * 解析 OpenAPI 3.0 / Swagger 2.0 JSON
     */
    public static ParseResult parse(String swaggerJson) {
        ParseResult result = new ParseResult();
        try {
            JsonNode root = MAPPER.readTree(swaggerJson);

            // 检测版本：有 "openapi" 字段则为 3.0，有 "swagger" 字段则为 2.0
            boolean isOpenApi3 = root.has("openapi");
            String version = isOpenApi3 ? getTextValue(root, "openapi") : getTextValue(root, "swagger");
            log.info("Swagger 解析开始，版本标识={}, title={}", version, result.getTitle());

            // 基本信息
            JsonNode info = root.get("info");
            if (info != null) {
                result.setTitle(getTextValue(info, "title"));
                result.setVersion(getTextValue(info, "version"));
            }
            result.setBasePath(getTextValue(root, "basePath"));

            // 构建统一的 schema 引用表（Swagger 2.0: definitions / OpenAPI 3.0: components.schemas）
            Map<String, JsonNode> definitions = new HashMap<>();
            JsonNode defs = isOpenApi3 ? root.path("components").path("schemas") : root.get("definitions");
            if (defs != null && defs.isObject()) {
                Iterator<String> names = defs.fieldNames();
                while (names.hasNext()) {
                    String name = names.next();
                    definitions.put(name, defs.get(name));
                }
            }

            // 提取根级 consumes（Swagger 2.0 全局请求数据类型）
            JsonNode rootConsumes = root.get("consumes");

            // 解析 paths
            JsonNode paths = root.get("paths");
            if (paths == null || !paths.isObject()) {
                log.warn("Swagger 解析失败：缺少 paths 节点或类型不正确");
                return result;
            }
            log.info("Swagger paths 节点包含 {} 个路径", paths.size());

            Iterator<String> pathNames = paths.fieldNames();
            while (pathNames.hasNext()) {
                String pathStr = pathNames.next();
                JsonNode pathNode = paths.get(pathStr);

                for (String method : HTTP_METHODS) {
                    JsonNode operation = pathNode.get(method);
                    if (operation == null) {
                        continue;
                    }

                    ApiEntry entry = parseOperation(pathStr, method.toUpperCase(), operation, definitions, isOpenApi3, rootConsumes);
                    result.getApis().add(entry);
                }
            }
            log.info("Swagger 解析结束，共解析 {} 个接口", result.getApis().size());
        } catch (Exception e) {
            log.error("OpenAPI/Swagger JSON 解析失败", e);
            throw new RuntimeException("OpenAPI/Swagger JSON 解析失败：" + e.getMessage(), e);
        }
        return result;
    }

    private static ApiEntry parseOperation(String path, String httpMethod, JsonNode operation,
                                            Map<String, JsonNode> definitions, boolean isOpenApi3, JsonNode rootConsumes) {
        ApiEntry entry = new ApiEntry();
        entry.setHttpMethod(httpMethod);
        entry.setPath(path);
        entry.setOperationId(getTextValue(operation, "operationId"));
        entry.setDescription(getTextValue(operation, "summary"));

        // 名称：优先使用 summary，否则使用 operationId 或 method+path
        String name = getTextValue(operation, "summary");
        if (!StringUtils.hasText(name)) {
            name = getTextValue(operation, "operationId");
        }
        if (!StringUtils.hasText(name)) {
            name = httpMethod + " " + path;
        }
        entry.setName(name);

        // 从 tags 提取服务名
        JsonNode tags = operation.get("tags");
        if (tags != null && tags.isArray() && tags.size() > 0) {
            entry.setService(tags.get(0).asText());
        }

        // 解析参数
        List<Map<String, Object>> params = new ArrayList<>();
        List<Map<String, Object>> headerParams = new ArrayList<>();
        JsonNode bodySchema = null;
        String contentType = null;

        // --- 请求体 ---
        if (isOpenApi3) {
            // OpenAPI 3.0: requestBody.content.{mediaType}.schema
            JsonNode requestBody = operation.get("requestBody");
            if (requestBody != null) {
                JsonNode content = requestBody.get("content");
                if (content != null) {
                    Iterator<String> mediaTypes = content.fieldNames();
                    if (mediaTypes.hasNext()) {
                        contentType = mediaTypes.next();
                        JsonNode mediaType = content.get(contentType);
                        JsonNode schema = mediaType.get("schema");
                        if (schema != null) {
                            bodySchema = resolveRef(schema, definitions);
                        }
                    }
                }
            }
        }

        // --- 参数（query / path / header / formData / body） ---
        JsonNode parameters = operation.get("parameters");
        if (parameters != null && parameters.isArray()) {
            for (JsonNode param : parameters) {
                String in = getTextValue(param, "in");
                if ("body".equals(in)) {
                    // Swagger 2.0: parameters[in=body].schema
                    JsonNode schema = param.get("schema");
                    if (schema != null) {
                        bodySchema = resolveRef(schema, definitions);
                    }
                } else if ("header".equals(in)) {
                    Map<String, Object> hp = new LinkedHashMap<>();
                    hp.put("name", getTextValue(param, "name"));
                    hp.put("type", "string");
                    hp.put("required", param.path("required").asBoolean(false));
                    hp.put("description", resolveDescription(param, isOpenApi3));
                    headerParams.add(hp);
                } else {
                    // query / path / formData
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("name", getTextValue(param, "name"));
                    p.put("in", in);
                    // OpenAPI 3.0: type/format 在 schema 内；Swagger 2.0: 直接在 param 上
                    JsonNode schemaNode = isOpenApi3 ? param.get("schema") : param;
                    p.put("type", schemaNode != null ? getTextValue(schemaNode, "type") : null);
                    p.put("format", schemaNode != null ? getTextValue(schemaNode, "format") : null);
                    p.put("required", param.path("required").asBoolean(false));
                    p.put("description", resolveDescription(param, isOpenApi3));
                    params.add(p);
                }
            }
        }

        // Swagger 2.0: 从 consumes 获取请求数据类型
        if (!isOpenApi3) {
            JsonNode consumes = operation.get("consumes");
            if (consumes != null && consumes.isArray() && consumes.size() > 0) {
                contentType = consumes.get(0).asText();
            } else if (rootConsumes != null && rootConsumes.isArray() && rootConsumes.size() > 0) {
                contentType = rootConsumes.get(0).asText();
            }
        }

        // 将请求数据类型写入 Content-Type 请求头
        if (contentType != null) {
            entry.setContentType(contentType);
            boolean hasContentType = false;
            for (Map<String, Object> h : headerParams) {
                if ("Content-Type".equalsIgnoreCase((String) h.get("name"))) {
                    hasContentType = true;
                    break;
                }
            }
            if (!hasContentType) {
                Map<String, Object> ct = new LinkedHashMap<>();
                ct.put("name", "Content-Type");
                ct.put("type", "string");
                ct.put("required", false);
                ct.put("description", "内容类型");
                ct.put("value", contentType);
                headerParams.add(ct);
            }
        }

        try {
            if (!params.isEmpty()) {
                entry.setRequestParams(MAPPER.writeValueAsString(params));
            }
            if (!headerParams.isEmpty()) {
                entry.setHeaders(MAPPER.writeValueAsString(headerParams));
            }
            if (bodySchema != null) {
                entry.setRequestBody(MAPPER.writeValueAsString(bodySchema));
            }
        } catch (Exception e) {
            log.warn("序列化参数失败", e);
        }

        // 解析响应
        JsonNode responses = operation.get("responses");
        if (responses != null) {
            JsonNode okResponse = responses.get("200");
            if (okResponse == null) {
                okResponse = responses.get("201");
            }
            if (okResponse != null) {
                JsonNode schema;
                if (isOpenApi3) {
                    // OpenAPI 3.0: responses.200.content.{mediaType}.schema
                    JsonNode content = okResponse.get("content");
                    schema = null;
                    if (content != null) {
                        Iterator<String> mediaTypes = content.fieldNames();
                        if (mediaTypes.hasNext()) {
                            schema = content.get(mediaTypes.next()).get("schema");
                        }
                    }
                } else {
                    // Swagger 2.0: responses.200.schema
                    schema = okResponse.get("schema");
                }
                if (schema != null) {
                    JsonNode resolved = resolveRef(schema, definitions);
                    try {
                        entry.setResponseBody(MAPPER.writeValueAsString(resolved));
                    } catch (Exception e) {
                        log.warn("序列化响应失败", e);
                    }
                }
            }
        }

        return entry;
    }

    /**
     * 解析 $ref 引用（兼容 #/definitions/Name 和 #/components/schemas/Name）
     */
    private static JsonNode resolveRef(JsonNode node, Map<String, JsonNode> definitions) {
        if (node == null) {
            return null;
        }
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            // #/definitions/ClassName
            String defName = ref.substring(ref.lastIndexOf('/') + 1);
            JsonNode resolved = definitions.get(defName);
            return resolved != null ? resolved : node;
        }
        // 递归解析数组 items
        if (node.has("items")) {
            JsonNode items = node.get("items");
            if (items.has("$ref")) {
                String ref = items.get("$ref").asText();
                String defName = ref.substring(ref.lastIndexOf('/') + 1);
                JsonNode resolved = definitions.get(defName);
                if (resolved != null) {
                    // 创建新节点替换 items
                    return node;
                }
            }
        }
        return node;
    }

    private static String getTextValue(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }

    /**
     * 解析参数说明：优先 parameter 层 description，OpenAPI 3.0 回退到 schema 层
     */
    private static String resolveDescription(JsonNode param, boolean isOpenApi3) {
        String desc = getTextValue(param, "description");
        if (desc == null && isOpenApi3) {
            JsonNode schema = param.get("schema");
            if (schema != null) {
                desc = getTextValue(schema, "description");
            }
        }
        return desc;
    }

    /**
     * 将 ApiEntry 转为 Api 实体
     */
    public static Api toApiEntity(ApiEntry entry, Long projectId, Long moduleId) {
        Api api = new Api();
        api.setProjectId(projectId);
        api.setModuleId(moduleId);
        api.setName(entry.getName());
        api.setService(entry.getService());
        api.setHttpMethod(entry.getHttpMethod());
        api.setPath(entry.getPath());
        api.setRequestParams(entry.getRequestParams());
        api.setRequestBody(entry.getRequestBody());
        api.setResponseBody(entry.getResponseBody());
        api.setHeaders(entry.getHeaders());
        api.setContentType(entry.getContentType());
        api.setDescription(entry.getDescription());
        api.setSourceType("SWAGGER_IMPORT");
        api.setSwaggerOperationId(entry.getOperationId());
        return api;
    }
}
