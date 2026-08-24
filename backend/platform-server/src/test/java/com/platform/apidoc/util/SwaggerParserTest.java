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
            "          {\"name\": \"err\", \"in\": \"query\", \"required\": false, \"description\": \"是否设置错误\", \"schema\": {\"type\": \"boolean\", \"default\": true}},\n" +
            "          {\"name\": \"mode\", \"in\": \"query\", \"required\": false, \"schema\": {\"type\": \"string\", \"description\": \"模拟模式\"}}\n" +
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
            "          {\"name\": \"page\", \"in\": \"query\", \"required\": false, \"description\": \"页码\", \"schema\": {\"type\": \"integer\", \"format\": \"int32\"}},\n" +
            "          {\"name\": \"size\", \"in\": \"query\", \"required\": false, \"description\": \"每页条数\", \"schema\": {\"type\": \"integer\", \"format\": \"int32\"}}\n" +
            "        ],\n" +
            "        \"responses\": {\n" +
            "          \"200\": {\"description\": \"OK\", \"content\": {\"*/*\": {\"schema\": {\"type\": \"string\"}}}}\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"components\": {\n" +
            "    \"schemas\": {\n" +
            "      \"ResponseBoolean\": {\"type\": \"object\", \"properties\": {\"code\": {\"type\": \"integer\", \"description\": \"状态码\"}, \"data\": {\"type\": \"boolean\", \"description\": \"操作结果\"}, \"msg\": {\"type\": \"string\", \"description\": \"提示消息\"}}},\n" +
            "      \"PlcBinValueReqVO\": {\"required\": [\"binNo\"], \"type\": \"object\", \"properties\": {\"binNo\": {\"type\": \"integer\", \"description\": \"充电桩编号（1-24）\", \"example\": 1}}}\n" +
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

        // 第一个接口：POST + query params（含 param 层和 schema 层 description），无 body
        SwaggerParser.ApiEntry entry = result.getApis().get(0);
        assertEquals("setReadEpcErr", entry.getOperationId());
        assertEquals("/rfid/setReadEpcErr", entry.getPath());
        assertEquals("POST", entry.getHttpMethod());
        assertEquals("设备管理 - RFID", entry.getService());
        assertNotNull(entry.getRequestParams(), "QueryParams 应被解析");
        // param 层 description
        assertTrue(entry.getRequestParams().contains("\"name\":\"err\""));
        assertTrue(entry.getRequestParams().contains("\"type\":\"boolean\""));
        assertTrue(entry.getRequestParams().contains("\"description\":\"是否设置错误\""));
        // schema 层 description 回退
        assertTrue(entry.getRequestParams().contains("\"name\":\"mode\""));
        assertTrue(entry.getRequestParams().contains("\"type\":\"string\""));
        assertTrue(entry.getRequestParams().contains("\"description\":\"模拟模式\""), "schema 层 description 应回退解析");
        assertNull(entry.getRequestBody(), "无 requestBody 应为 null");
        assertNotNull(entry.getResponseBody(), "响应体应被解析");
        // 响应参数说明
        assertTrue(entry.getResponseBody().contains("\"description\":\"状态码\""));
        assertTrue(entry.getResponseBody().contains("\"description\":\"操作结果\""));
        assertTrue(entry.getResponseBody().contains("\"description\":\"提示消息\""));
    }

    @Test
    void parseOpenApi3_postWithBody() {
        SwaggerParser.ParseResult result = SwaggerParser.parse(OPENAPI_3_JSON);

        // 第二个接口：POST + body（含属性 description），无 query params
        SwaggerParser.ApiEntry entry = result.getApis().get(1);
        assertEquals("setWaterElectron", entry.getOperationId());
        assertEquals("/plc/setWaterElectron", entry.getPath());
        assertEquals("POST", entry.getHttpMethod());
        assertNull(entry.getRequestParams(), "无 query params 应为 null");
        assertNotNull(entry.getRequestBody(), "Body 应被解析");
        // Body 参数说明
        assertTrue(entry.getRequestBody().contains("\"binNo\""));
        assertTrue(entry.getRequestBody().contains("\"description\":\"充电桩编号（1-24）\""), "Body 属性 description 应被保留");
        assertNotNull(entry.getResponseBody());
    }

    @Test
    void parseOpenApi3_getWithQueryParams() {
        SwaggerParser.ParseResult result = SwaggerParser.parse(OPENAPI_3_JSON);

        // 第三个接口：GET + query params（含 description），无 body
        SwaggerParser.ApiEntry entry = result.getApis().get(2);
        assertEquals("getDataList", entry.getOperationId());
        assertEquals("GET", entry.getHttpMethod());
        assertNotNull(entry.getRequestParams(), "GET 的 QueryParams 应被解析");
        assertTrue(entry.getRequestParams().contains("\"name\":\"page\""));
        assertTrue(entry.getRequestParams().contains("\"type\":\"integer\""));
        assertTrue(entry.getRequestParams().contains("\"format\":\"int32\""));
        assertTrue(entry.getRequestParams().contains("\"description\":\"页码\""));
        assertTrue(entry.getRequestParams().contains("\"name\":\"size\""));
        assertTrue(entry.getRequestParams().contains("\"description\":\"每页条数\""));
        assertNull(entry.getRequestBody(), "GET 无 body 应为 null");
    }

    @Test
    void parseOpenApi3_contentTypeInHeaders() {
        SwaggerParser.ParseResult result = SwaggerParser.parse(OPENAPI_3_JSON);
        // 第二个接口有 requestBody，content type 为 application/json
        SwaggerParser.ApiEntry entry = result.getApis().get(1);
        assertNotNull(entry.getHeaders(), "有 requestBody 的接口应有 Content-Type 请求头");
        assertTrue(entry.getHeaders().contains("\"name\":\"Content-Type\""));
        assertTrue(entry.getHeaders().contains("\"value\":\"application/json\""));
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
