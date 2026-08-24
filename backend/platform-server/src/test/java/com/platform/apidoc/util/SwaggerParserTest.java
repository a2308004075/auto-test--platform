package com.platform.apidoc.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SwaggerParser OpenAPI 3.0 / Swagger 2.0 解析自检
 */
class SwaggerParserTest {

    private static final String OPENAPI_3_JSON = "{\n" +
            "  \"openapi\": \"3.0.1\",\n" +
            "  \"info\": {\"title\": \"测试服务\", \"version\": \"v1.0\"},\n" +
            "  \"servers\": [{\"url\": \"http://example.com/mock\"}],\n" +
            "  \"paths\": {\n" +
            "    \"/rfid/setReadEpcErr\": {\n" +
            "      \"post\": {\n" +
            "        \"tags\": [\"设备管理 - RFID\"],\n" +
            "        \"summary\": \"设置 EPC 读取错误\",\n" +
            "        \"operationId\": \"setReadEpcErr\",\n" +
            "        \"parameters\": [\n" +
            "          {\"name\": \"err\", \"in\": \"query\", \"required\": false, \"schema\": {\"type\": \"boolean\", \"default\": true}}\n" +
            "        ],\n" +
            "        \"responses\": {\n" +
            "          \"200\": {\"description\": \"OK\", \"content\": {\"*/*\": {\"schema\": {\"$ref\": \"#/components/schemas/ResponseBoolean\"}}}}\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"/plc/setWaterElectron\": {\n" +
            "      \"post\": {\n" +
            "        \"tags\": [\"设备管理 - PLC\"],\n" +
            "        \"summary\": \"设置水电电子状态\",\n" +
            "        \"operationId\": \"setWaterElectron\",\n" +
            "        \"requestBody\": {\n" +
            "          \"content\": {\"application/json\": {\"schema\": {\"$ref\": \"#/components/schemas/PlcBinValueReqVO\"}}},\n" +
            "          \"required\": true\n" +
            "        },\n" +
            "        \"responses\": {\n" +
            "          \"200\": {\"description\": \"OK\", \"content\": {\"*/*\": {\"schema\": {\"$ref\": \"#/components/schemas/ResponseBoolean\"}}}}\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"/data/list\": {\n" +
            "      \"get\": {\n" +
            "        \"tags\": [\"数据服务\"],\n" +
            "        \"summary\": \"获取数据列表\",\n" +
            "        \"operationId\": \"getDataList\",\n" +
            "        \"parameters\": [\n" +
            "          {\"name\": \"page\", \"in\": \"query\", \"required\": false, \"schema\": {\"type\": \"integer\"}},\n" +
            "          {\"name\": \"size\", \"in\": \"query\", \"required\": false, \"schema\": {\"type\": \"integer\"}}\n" +
            "        ],\n" +
            "        \"responses\": {\n" +
            "          \"200\": {\"description\": \"OK\", \"content\": {\"*/*\": {\"schema\": {\"type\": \"string\"}}}}\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"components\": {\n" +
            "    \"schemas\": {\n" +
            "      \"ResponseBoolean\": {\"type\": \"object\", \"properties\": {\"code\": {\"type\": \"integer\"}, \"data\": {\"type\": \"boolean\"}, \"msg\": {\"type\": \"string\"}}},\n" +
            "      \"PlcBinValueReqVO\": {\"required\": [\"binNo\"], \"type\": \"object\", \"properties\": {\"binNo\": {\"type\": \"integer\"}}}\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String SWAGGER_2_JSON = "{\n" +
            "  \"swagger\": \"2.0\",\n" +
            "  \"info\": {\"title\": \"旧版服务\", \"version\": \"1.0\"},\n" +
            "  \"basePath\": \"/api\",\n" +
            "  \"paths\": {\n" +
            "    \"/users\": {\n" +
            "      \"get\": {\n" +
            "        \"tags\": [\"用户\"],\n" +
            "        \"summary\": \"用户列表\",\n" +
            "        \"operationId\": \"listUsers\",\n" +
            "        \"parameters\": [{\"name\": \"name\", \"in\": \"query\", \"type\": \"string\", \"required\": false}],\n" +
            "        \"responses\": {\"200\": {\"description\": \"OK\", \"schema\": {\"type\": \"string\"}}}\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"definitions\": {\n" +
            "    \"User\": {\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"integer\"}}}\n" +
            "  }\n" +
            "}";

    @Test
    void parseOpenApi3_postWithQueryParams() {
        SwaggerParser.ParseResult result = SwaggerParser.parse(OPENAPI_3_JSON);
        assertEquals(3, result.getApis().size());

        // 第一个接口：POST + query params，无 body
        SwaggerParser.ApiEntry entry = result.getApis().get(0);
        assertEquals("setReadEpcErr", entry.getOperationId());
        assertEquals("/rfid/setReadEpcErr", entry.getPath());
        assertEquals("POST", entry.getHttpMethod());
        assertEquals("设备管理 - RFID", entry.getService());
        assertNotNull(entry.getRequestParams(), "QueryParams 应被解析");
        assertTrue(entry.getRequestParams().contains("\"name\":\"err\""));
        assertTrue(entry.getRequestParams().contains("\"type\":\"boolean\""));
        assertNull(entry.getRequestBody(), "无 requestBody 应为 null");
        assertNotNull(entry.getResponseBody(), "响应体应被解析");
        assertTrue(entry.getResponseBody().contains("\"type\":\"object\""));
    }

    @Test
    void parseOpenApi3_postWithBody() {
        SwaggerParser.ParseResult result = SwaggerParser.parse(OPENAPI_3_JSON);

        // 第二个接口：POST + body，无 query params
        SwaggerParser.ApiEntry entry = result.getApis().get(1);
        assertEquals("setWaterElectron", entry.getOperationId());
        assertEquals("/plc/setWaterElectron", entry.getPath());
        assertEquals("POST", entry.getHttpMethod());
        assertNull(entry.getRequestParams(), "无 query params 应为 null");
        assertNotNull(entry.getRequestBody(), "Body 应被解析");
        assertTrue(entry.getRequestBody().contains("\"binNo\""));
        assertNotNull(entry.getResponseBody());
    }

    @Test
    void parseOpenApi3_getWithQueryParams() {
        SwaggerParser.ParseResult result = SwaggerParser.parse(OPENAPI_3_JSON);

        // 第三个接口：GET + query params，无 body
        SwaggerParser.ApiEntry entry = result.getApis().get(2);
        assertEquals("getDataList", entry.getOperationId());
        assertEquals("GET", entry.getHttpMethod());
        assertNotNull(entry.getRequestParams(), "GET 的 QueryParams 应被解析");
        assertTrue(entry.getRequestParams().contains("\"name\":\"page\""));
        assertTrue(entry.getRequestParams().contains("\"name\":\"size\""));
        assertNull(entry.getRequestBody(), "GET 无 body 应为 null");
    }

    @Test
    void parseSwagger2_backwardsCompatible() {
        SwaggerParser.ParseResult result = SwaggerParser.parse(SWAGGER_2_JSON);
        assertEquals(1, result.getApis().size());

        SwaggerParser.ApiEntry entry = result.getApis().get(0);
        assertEquals("listUsers", entry.getOperationId());
        assertEquals("GET", entry.getHttpMethod());
        assertEquals("/users", entry.getPath());
        assertEquals("/api", result.getBasePath());
        assertNotNull(entry.getRequestParams());
        assertTrue(entry.getRequestParams().contains("\"name\":\"name\""));
        assertTrue(entry.getRequestParams().contains("\"type\":\"string\""));
    }
}
