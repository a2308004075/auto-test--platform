/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 关键字管理服务
 */
package com.platform.keyword.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.action.entity.Action;
import com.platform.action.mapper.ActionMapper;
import com.platform.apidoc.dto.ApiDebugRequest;
import com.platform.apidoc.dto.ApiDebugResponse;
import com.platform.apidoc.entity.Api;
import com.platform.apidoc.mapper.ApiMapper;
import com.platform.apidoc.service.ApiService;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.dto.ReferenceDetailResponse;
import com.platform.common.response.PageResponse;
import com.platform.execution.entity.TestCase;
import com.platform.execution.mapper.TestCaseMapper;
import com.platform.keyword.dto.ApiKeywordCreateRequest;
import com.platform.keyword.dto.ApiKeywordDebugRequest;
import com.platform.keyword.dto.ApiKeywordResponse;
import com.platform.keyword.dto.ApiKeywordUpdateRequest;
import com.platform.keyword.entity.ApiKeyword;
import com.platform.keyword.entity.ApiKeywordGroup;
import com.platform.keyword.entity.Keyword;
import com.platform.keyword.mapper.ApiKeywordGroupMapper;
import com.platform.keyword.mapper.ApiKeywordMapper;
import com.platform.keyword.mapper.KeywordMapper;
import com.platform.project.entity.ApiModule;
import com.platform.project.mapper.ApiModuleMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 接口关键字管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeywordService {

    private final KeywordMapper keywordMapper;
    private final ApiKeywordMapper apiKeywordMapper;
    private final ApiKeywordGroupMapper apiKeywordGroupMapper;
    private final ApiKeywordGroupService apiKeywordGroupService;
    private final ApiMapper apiMapper;
    private final ApiModuleMapper apiModuleMapper;
    private final ActionMapper actionMapper;
    private final TestCaseMapper testCaseMapper;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;
    private final ApiService apiService;

    /**
     * 分页查询接口关键字列表
     */
    public PageResponse<ApiKeywordResponse> list(Long projectId, String keyword, String category,
                                                   Long moduleId, Long groupId, int page, int pageSize) {
        LambdaQueryWrapper<Keyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Keyword::getProjectId, projectId)
                .eq(Keyword::getType, "API");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Keyword::getName, keyword);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Keyword::getCategory, category);
        }
        // 按关联接口分组筛选：查出该分组下所有接口关联的关键字 ID，再 in 筛选
        if (moduleId != null) {
            List<Long> keywordIds = findKeywordIdsByModuleId(moduleId);
            if (keywordIds.isEmpty()) {
                return PageResponse.of(new ArrayList<>(), 0, page, pageSize);
            }
            wrapper.in(Keyword::getId, keywordIds);
        }
        // 按接口关键字分组筛选（含子孙分组）
        if (groupId != null) {
            Set<Long> groupIds = apiKeywordGroupService.getDescendantGroupIds(groupId);
            List<Long> keywordIds = findKeywordIdsByGroupIds(new ArrayList<>(groupIds));
            if (keywordIds.isEmpty()) {
                return PageResponse.of(new ArrayList<>(), 0, page, pageSize);
            }
            wrapper.in(Keyword::getId, keywordIds);
        }
        wrapper.orderByDesc(Keyword::getCreatedAt);

        Page<Keyword> pageParam = new Page<>(page, pageSize);
        Page<Keyword> result = keywordMapper.selectPage(pageParam, wrapper);

        List<ApiKeywordResponse> records = new ArrayList<>();
        for (Keyword kw : result.getRecords()) {
            records.add(buildResponse(kw));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 创建接口关键字
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiKeywordResponse create(ApiKeywordCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        // 名称唯一性检查（同项目下关键字名称不可重复）
        LambdaQueryWrapper<Keyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Keyword::getProjectId, request.getProjectId())
                .eq(Keyword::getName, request.getName());
        if (keywordMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.KEYWORD_NAME_DUPLICATE,
                    "关键字名称已存在：" + request.getName());
        }

        // 创建 keyword 记录
        Keyword kw = new Keyword();
        kw.setName(request.getName());
        kw.setType("API");
        kw.setProjectId(request.getProjectId());
        kw.setDescription(request.getDescription());
        kw.setCategory(request.getCategory());
        kw.setTags(request.getTags());
        keywordMapper.insert(kw);

        // 创建 api_keyword 绑定记录
        ApiKeyword apiKw = new ApiKeyword();
        apiKw.setKeywordId(kw.getId());
        apiKw.setProjectId(request.getProjectId());
        apiKw.setApiId(request.getApiId());
        apiKw.setGroupId(request.getGroupId());
        apiKw.setTestData(request.getTestData());
        apiKw.setResponseAssertion(request.getResponseAssertion());
        apiKeywordMapper.insert(apiKw);

        return buildResponse(kw);
    }

    /**
     * 更新接口关键字
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiKeywordResponse update(Long keywordId, ApiKeywordUpdateRequest request) {
        Keyword kw = findKeywordById(keywordId);

        // 名称唯一性检查（排除自身）
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(kw.getName())) {
            LambdaQueryWrapper<Keyword> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Keyword::getProjectId, kw.getProjectId())
                    .eq(Keyword::getName, request.getName())
                    .ne(Keyword::getId, keywordId);
            if (keywordMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.KEYWORD_NAME_DUPLICATE,
                        "关键字名称已存在：" + request.getName());
            }
            kw.setName(request.getName());
        }
        if (request.getDescription() != null) {
            kw.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            kw.setCategory(request.getCategory());
        }
        if (request.getTags() != null) {
            kw.setTags(request.getTags());
        }
        keywordMapper.updateById(kw);

        // 更新 api_keyword 绑定记录
        if (request.getGroupId() != null || request.getTestData() != null || request.getResponseAssertion() != null) {
            ApiKeyword apiKw = findApiKeywordByKeywordId(keywordId);
            if (apiKw != null) {
                if (request.getGroupId() != null) {
                    apiKw.setGroupId(request.getGroupId());
                }
                if (request.getTestData() != null) {
                    apiKw.setTestData(request.getTestData());
                }
                if (request.getResponseAssertion() != null) {
                    apiKw.setResponseAssertion(request.getResponseAssertion());
                }
                apiKeywordMapper.updateById(apiKw);
            }
        }

        return buildResponse(kw);
    }

    /**
     * 获取关键字详情
     */
    public ApiKeywordResponse getById(Long keywordId) {
        Keyword kw = findKeywordById(keywordId);
        return buildResponse(kw);
    }

    /**
     * 删除接口关键字（含删除保护检查）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long keywordId) {
        Keyword kw = findKeywordById(keywordId);

        // 删除保护检查 - 被 Action 节点引用时不可删除
        long actionRefCount = countActionReferences(keywordId);
        if (actionRefCount > 0) {
            throw new BusinessException(ErrorCode.KEYWORD_DEPENDENCY_CONFLICT,
                    "关键字被 " + actionRefCount + " 个 Action 节点引用，无法删除");
        }

        // 删除保护检查 - 被 test_case_step 引用时不可删除
        long caseRefCount = countTestCaseReferences(keywordId);
        if (caseRefCount > 0) {
            throw new BusinessException(ErrorCode.KEYWORD_DEPENDENCY_CONFLICT,
                    "关键字被 " + caseRefCount + " 个测试用例引用，无法删除");
        }

        // 删除 api_keyword 绑定记录
        LambdaQueryWrapper<ApiKeyword> apiKwWrapper = new LambdaQueryWrapper<>();
        apiKwWrapper.eq(ApiKeyword::getKeywordId, keywordId);
        apiKeywordMapper.delete(apiKwWrapper);

        // 删除 keyword 记录
        keywordMapper.deleteById(keywordId);
    }

    /**
     * 清空分组及其子孙分组中的所有关键字（被引用的关键字跳过）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByGroup(Long groupId) {
        Set<Long> groupIds = apiKeywordGroupService.getDescendantGroupIds(groupId);
        List<Long> keywordIds = findKeywordIdsByGroupIds(new ArrayList<>(groupIds));
        if (keywordIds.isEmpty()) {
            return;
        }
        deleteUnreferenced(keywordMapper.selectBatchIds(keywordIds));
    }

    /**
     * 清空项目下所有接口关键字（被引用的关键字跳过）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByProject(Long projectId) {
        LambdaQueryWrapper<Keyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Keyword::getProjectId, projectId)
                .eq(Keyword::getType, "API");
        deleteUnreferenced(keywordMapper.selectList(wrapper));
    }

    /**
     * 批量删除未被引用的关键字，被 Action 节点或测试用例引用的跳过
     */
    private void deleteUnreferenced(List<Keyword> keywords) {
        if (keywords.isEmpty()) {
            return;
        }
        // ponytail: 引用检查为 JSON 列 like 查询只能逐条计数；量大时可引入冗余引用计数列
        for (Keyword kw : keywords) {
            if (countActionReferences(kw.getId()) > 0 || countTestCaseReferences(kw.getId()) > 0) {
                continue;
            }
            LambdaQueryWrapper<ApiKeyword> apiKwWrapper = new LambdaQueryWrapper<>();
            apiKwWrapper.eq(ApiKeyword::getKeywordId, kw.getId());
            apiKeywordMapper.delete(apiKwWrapper);
            keywordMapper.deleteById(kw.getId());
        }
    }

    /**
     * 从接口快速生成关键字
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiKeywordResponse generateFromApi(Long projectId, Long apiId) {
        projectService.findActiveById(projectId);

        Api api = apiMapper.selectById(apiId);
        if (api == null) {
            throw new BusinessException(ErrorCode.API_NOT_FOUND, "接口不存在：" + apiId);
        }

        // 检查是否已存在同接口生成的关键字
        LambdaQueryWrapper<ApiKeyword> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(ApiKeyword::getProjectId, projectId)
                .eq(ApiKeyword::getApiId, apiId);
        if (apiKeywordMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(ErrorCode.KEYWORD_NAME_DUPLICATE,
                    "该接口已生成过关键字");
        }

        // 使用接口名称作为关键字名称
        String kwName = api.getName();
        LambdaQueryWrapper<Keyword> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(Keyword::getProjectId, projectId)
                .eq(Keyword::getName, kwName);
        if (keywordMapper.selectCount(nameWrapper) > 0) {
            kwName = kwName + "_关键字_" + System.currentTimeMillis() % 10000;
        }

        Keyword kw = new Keyword();
        kw.setName(kwName);
        kw.setType("API");
        kw.setProjectId(projectId);
        kw.setDescription("由接口 " + api.getName() + " 自动生成");
        keywordMapper.insert(kw);

        ApiKeyword apiKw = new ApiKeyword();
        apiKw.setKeywordId(kw.getId());
        apiKw.setProjectId(projectId);
        apiKw.setApiId(apiId);
        apiKeywordMapper.insert(apiKw);

        return buildResponse(kw);
    }

    /**
     * 接口关键字在线调试
     * <p>按关键字保存的 testData 作为请求参数，调用关联接口的真实请求。</p>
     */
    public ApiDebugResponse debug(Long keywordId, ApiKeywordDebugRequest request) {
        findKeywordById(keywordId);
        ApiKeyword apiKeyword = findApiKeywordByKeywordId(keywordId);
        if (apiKeyword == null) {
            return ApiDebugResponse.error("关键字未绑定接口");
        }

        Api api = apiMapper.selectById(apiKeyword.getApiId());
        if (api == null) {
            return ApiDebugResponse.error("关联接口不存在");
        }

        // 解析关键字保存的测试数据
        Map<String, String> testDataMap = parseTestData(apiKeyword.getTestData());

        // 提取路径参数：路径中 {xxx} 占位符对应的参数从 testData 中剔除
        Map<String, String> pathParams = new LinkedHashMap<>();
        Map<String, String> remainingParams = new LinkedHashMap<>(testDataMap);
        if (api.getPath() != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{(\\w+)\\}");
            java.util.regex.Matcher matcher = pattern.matcher(api.getPath());
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = testDataMap.get(key);
                if (value != null) {
                    pathParams.put(key, value);
                    remainingParams.remove(key);
                }
            }
        }

        ApiDebugRequest debugRequest = new ApiDebugRequest();
        debugRequest.setEnvironmentId(request.getEnvironmentId());
        debugRequest.setPathParams(pathParams);

        String method = api.getHttpMethod() == null ? "GET" : api.getHttpMethod().toUpperCase();
        if ("GET".equals(method) || "DELETE".equals(method)) {
            debugRequest.setQueryParams(remainingParams);
        } else {
            // POST / PUT / PATCH 使用 JSON body
            // ponytail: __body__ 整体参数直接发送原始 JSON，而非包装为 {"__body__": "..."}
            if (remainingParams.size() == 1 && remainingParams.containsKey("__body__")) {
                String bodyValue = remainingParams.get("__body__");
                debugRequest.setBody(StringUtils.hasText(bodyValue) ? bodyValue : "");
            } else {
                try {
                    debugRequest.setBody(objectMapper.writeValueAsString(remainingParams));
                } catch (Exception e) {
                    log.warn("关键字测试数据序列化为 JSON 失败: {}", e.getMessage());
                    debugRequest.setBody("");
                }
            }
        }

        return apiService.debug(api.getId(), debugRequest);
    }

    /**
     * 查询关键字引用关系详情（被哪些 Action 和测试用例引用）
     */
    public List<ReferenceDetailResponse> getDependencies(Long keywordId) {
        findKeywordById(keywordId);
        List<ReferenceDetailResponse> result = new ArrayList<>();

        // 查询 Action 节点引用（搜索 nodes JSON 列中的 refKeywordId）
        List<Action> refActions = findActionsReferencingKeyword(keywordId);
        for (Action action : refActions) {
            ReferenceDetailResponse ref = new ReferenceDetailResponse();
            ref.setRefType("ACTION");
            ref.setRefId(action.getId());
            ref.setRefName(action.getName());
            ref.setRefDescription(action.getDescription());
            result.add(ref);
        }

        // 查询测试用例引用
        LambdaQueryWrapper<TestCase> caseWrapper = new LambdaQueryWrapper<>();
        String kwIdStr = String.valueOf(keywordId);
        caseWrapper.and(w -> w.like(TestCase::getSteps, kwIdStr)
                .or().like(TestCase::getSetupSteps, kwIdStr)
                .or().like(TestCase::getTeardownSteps, kwIdStr));
        List<TestCase> cases = testCaseMapper.selectList(caseWrapper);
        for (TestCase tc : cases) {
            ReferenceDetailResponse ref = new ReferenceDetailResponse();
            ref.setRefType("TEST_CASE");
            ref.setRefId(tc.getId());
            ref.setRefName(tc.getName());
            ref.setRefDescription(tc.getDescription());
            result.add(ref);
        }

        return result;
    }

    // ───────────────────── 私有方法 ─────────────────────

    private Keyword findKeywordById(Long keywordId) {
        Keyword kw = keywordMapper.selectById(keywordId);
        if (kw == null) {
            throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND, "关键字不存在：" + keywordId);
        }
        return kw;
    }

    /**
     * 解析关键字测试数据（JSON 数组）为键值对
     */
    private Map<String, String> parseTestData(String testData) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(testData)) {
            return result;
        }
        try {
            JsonNode array = objectMapper.readTree(testData);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    String name = node.has("name") ? node.get("name").asText() : null;
                    String value = node.has("value") ? node.get("value").asText() : null;
                    if (StringUtils.hasText(name)) {
                        result.put(name, value == null ? "" : value);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析关键字测试数据失败: {}", e.getMessage());
        }
        return result;
    }

    private ApiKeyword findApiKeywordByKeywordId(Long keywordId) {
        LambdaQueryWrapper<ApiKeyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyword::getKeywordId, keywordId)
                .last("LIMIT 1");
        return apiKeywordMapper.selectOne(wrapper);
    }

    /**
     * 查询关联指定接口分组的所有关键字 ID
     */
    private List<Long> findKeywordIdsByModuleId(Long moduleId) {
        // 查询该分组下的所有接口 ID
        LambdaQueryWrapper<Api> apiWrapper = new LambdaQueryWrapper<>();
        apiWrapper.eq(Api::getModuleId, moduleId);
        List<Api> apis = apiMapper.selectList(apiWrapper);
        if (apis.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> apiIds = apis.stream().map(Api::getId).collect(Collectors.toList());
        // 查询关联这些接口的关键字 ID
        LambdaQueryWrapper<ApiKeyword> akWrapper = new LambdaQueryWrapper<>();
        akWrapper.in(ApiKeyword::getApiId, apiIds);
        List<ApiKeyword> aks = apiKeywordMapper.selectList(akWrapper);
        return aks.stream().map(ApiKeyword::getKeywordId).distinct().collect(Collectors.toList());
    }

    /**
     * 查询指定接口关键字分组下的所有关键字 ID
     */
    private List<Long> findKeywordIdsByGroupId(Long groupId) {
        LambdaQueryWrapper<ApiKeyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyword::getGroupId, groupId);
        List<ApiKeyword> aks = apiKeywordMapper.selectList(wrapper);
        return aks.stream().map(ApiKeyword::getKeywordId).distinct().collect(Collectors.toList());
    }

    /**
     * 查询多个接口关键字分组下的所有关键字 ID
     */
    private List<Long> findKeywordIdsByGroupIds(List<Long> groupIds) {
        LambdaQueryWrapper<ApiKeyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ApiKeyword::getGroupId, groupIds);
        List<ApiKeyword> aks = apiKeywordMapper.selectList(wrapper);
        return aks.stream().map(ApiKeyword::getKeywordId).distinct().collect(Collectors.toList());
    }

    /**
     * 统计引用指定关键字的 Action 数量（搜索 action.nodes JSON 列中的 refKeywordId）
     */
    private long countActionReferences(Long keywordId) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Action::getNodes, "\"refKeywordId\":" + keywordId);
        return actionMapper.selectCount(wrapper);
    }

    /**
     * 查找引用指定关键字的所有 Action（搜索 action.nodes JSON 列中的 refKeywordId）
     */
    private List<Action> findActionsReferencingKeyword(Long keywordId) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Action::getNodes, "\"refKeywordId\":" + keywordId);
        return actionMapper.selectList(wrapper);
    }

    /**
     * 统计引用指定关键字的测试用例数量（搜索 steps / setup_steps / teardown_steps JSON）
     */
    private long countTestCaseReferences(Long keywordId) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(TestCase::getSteps, keywordId)
                .or().like(TestCase::getSetupSteps, keywordId)
                .or().like(TestCase::getTeardownSteps, keywordId));
        return testCaseMapper.selectCount(wrapper);
    }

    private ApiKeywordResponse buildResponse(Keyword kw) {
        ApiKeywordResponse resp = new ApiKeywordResponse();
        resp.setId(kw.getId());
        resp.setKeywordId(kw.getId());
        resp.setProjectId(kw.getProjectId());
        resp.setName(kw.getName());
        resp.setType(kw.getType());
        resp.setDescription(kw.getDescription());
        resp.setCategory(kw.getCategory());
        resp.setTags(kw.getTags());
        resp.setCreatedBy(kw.getCreatedBy());
        resp.setUpdatedBy(kw.getUpdatedBy());
        resp.setCreatedAt(kw.getCreatedAt());
        resp.setUpdatedAt(kw.getUpdatedAt());
        // 计算引用次数：Action 节点引用 + test_case 步骤引用
        long actionRefCount = countActionReferences(kw.getId());
        long caseRefCount = countTestCaseReferences(kw.getId());
        resp.setReferenceCount((int) (actionRefCount + caseRefCount));

        // 关联查询 api_keyword
        ApiKeyword apiKw = findApiKeywordByKeywordId(kw.getId());
        if (apiKw != null) {
            resp.setApiId(apiKw.getApiId());
            resp.setTestData(apiKw.getTestData());
            resp.setResponseAssertion(apiKw.getResponseAssertion());
            resp.setGroupId(apiKw.getGroupId());
            // 查询关键字分组名称
            if (apiKw.getGroupId() != null) {
                ApiKeywordGroup group = apiKeywordGroupMapper.selectById(apiKw.getGroupId());
                if (group != null) {
                    resp.setGroupName(group.getName());
                }
            }
            // 查询关联接口详情
            Api api = apiMapper.selectById(apiKw.getApiId());
            if (api != null) {
                resp.setApiName(api.getName());
                resp.setApiPath(api.getPath());
                resp.setHttpMethod(api.getHttpMethod());
                resp.setModuleId(api.getModuleId());
                // 查询接口分组名称
                if (api.getModuleId() != null) {
                    ApiModule module = apiModuleMapper.selectById(api.getModuleId());
                    if (module != null) {
                        resp.setModuleName(module.getName());
                    }
                }
            }
        }

        return resp;
    }
}
