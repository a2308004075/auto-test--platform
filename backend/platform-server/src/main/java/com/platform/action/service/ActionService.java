/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 关键字管理服务
 */
package com.platform.action.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.action.dto.*;
import com.platform.action.entity.Action;
import com.platform.action.entity.ActionNode;
import com.platform.action.mapper.ActionMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.environment.service.EnvironmentService;
import com.platform.execution.engine.ActionExecutor;
import com.platform.execution.engine.ExecutionContext;
import com.platform.execution.engine.StepResult;
import com.platform.execution.entity.AutoCase;
import com.platform.execution.mapper.AutoCaseMapper;
import com.platform.keyword.entity.Keyword;
import com.platform.keyword.mapper.KeywordMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.Map.Entry;

/**
 * Action 关键字管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActionService {

    private final ActionMapper actionMapper;
    private final KeywordMapper keywordMapper;
    private final AutoCaseMapper autoCaseMapper;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;
    private final ActionExecutor actionExecutor;
    private final EnvironmentService environmentService;
    private final ActionGroupService actionGroupService;

    /**
     * 分页查询 Action 列表
     */
    public PageResponse<ActionResponse> list(Long projectId, String keyword, Long groupId,
                                               int page, int pageSize) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Action::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Action::getName, keyword)
                    .or().like(Action::getDescription, keyword));
        }
        if (groupId != null) {
            if (groupId == 0) {
                // 0 = 未分组
                wrapper.isNull(Action::getGroupId);
            } else {
                // 正数 = 指定分组含子分组
                Set<Long> idSet = actionGroupService.getDescendantGroupIds(groupId);
                wrapper.in(Action::getGroupId, idSet);
            }
        }
        wrapper.orderByDesc(Action::getCreatedAt);

        Page<Action> pageParam = new Page<>(page, pageSize);
        Page<Action> result = actionMapper.selectPage(pageParam, wrapper);

        List<ActionResponse> records = new ArrayList<>();
        for (Action action : result.getRecords()) {
            records.add(toResponse(action));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 创建 Action
     */
    @Transactional(rollbackFor = Exception.class)
    public ActionResponse create(ActionCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        // 名称唯一性检查
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Action::getProjectId, request.getProjectId())
                .eq(Action::getName, request.getName());
        if (actionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ACTION_NAME_DUPLICATE,
                    "Action 名称已存在：" + request.getName());
        }

        // 循环引用检测
        if (request.getNodes() != null && !request.getNodes().isEmpty()) {
            detectCircularReferences(request.getNodes());
        }

        Action action = new Action();
        action.setProjectId(request.getProjectId());
        action.setName(request.getName());
        action.setDescription(request.getDescription());
        action.setGroupId(request.getGroupId());
        action.setInputParams(request.getInputParams());
        action.setOutputParams(request.getOutputParams());
        action.setIsActive(1);

        // 序列化节点到 JSON
        if (request.getNodes() != null) {
            try {
                action.setNodes(objectMapper.writeValueAsString(request.getNodes()));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.ACTION_NODE_SERIALIZE_FAILED,
                        "节点序列化失败：" + e.getMessage());
            }
        }

        actionMapper.insert(action);

        return toResponse(action);
    }

    /**
     * 更新 Action
     */
    @Transactional(rollbackFor = Exception.class)
    public ActionResponse update(Long actionId, ActionUpdateRequest request) {
        Action action = findById(actionId);

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(action.getName())) {
            LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Action::getProjectId, action.getProjectId())
                    .eq(Action::getName, request.getName())
                    .ne(Action::getId, actionId);
            if (actionMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.ACTION_NAME_DUPLICATE,
                        "Action 名称已存在：" + request.getName());
            }
            action.setName(request.getName());
        }
        if (request.getDescription() != null) {
            action.setDescription(request.getDescription());
        }
        if (request.getGroupId() != null) {
            action.setGroupId(request.getGroupId());
        }
        if (request.getInputParams() != null) {
            action.setInputParams(request.getInputParams());
        }
        if (request.getOutputParams() != null) {
            action.setOutputParams(request.getOutputParams());
        }
        if (request.getNodes() != null) {
            detectCircularReferences(request.getNodes());
            try {
                action.setNodes(objectMapper.writeValueAsString(request.getNodes()));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.ACTION_NODE_SERIALIZE_FAILED,
                        "节点序列化失败：" + e.getMessage());
            }
        }

        actionMapper.updateById(action);
        return toResponse(action);
    }

    /**
     * 获取 Action 详情
     */
    public ActionResponse getById(Long actionId) {
        return toResponse(findById(actionId));
    }

    /**
     * 删除 Action（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long actionId) {
        findById(actionId);

        // 删除保护检查 - 被自动化用例引用时不可删除
        Keyword actionKeyword = findActionKeyword(actionId);
        if (actionKeyword != null) {
            long refCount = countAutoCaseReferences(actionKeyword.getId());
            if (refCount > 0) {
                throw new BusinessException(ErrorCode.ACTION_DEPENDENCY_CONFLICT,
                        "Action 被 " + refCount + " 个自动化用例引用，无法删除");
            }
        }

        actionMapper.deleteById(actionId);
    }

    /**
     * 批量修改 Action 分组
     * <p>targetGroupId 为 0 时设为 NULL（归入未分组）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchMove(Long projectId, List<Long> actionIds, Long targetGroupId) {
        Long resolvedGroupId = (targetGroupId != null && targetGroupId == 0) ? null : targetGroupId;
        for (Long id : actionIds) {
            Action action = actionMapper.selectById(id);
            if (action != null && action.getProjectId().equals(projectId)) {
                action.setGroupId(resolvedGroupId);
                actionMapper.updateById(action);
            }
        }
    }

    /**
     * 调试执行 Action
     * <p>加载环境配置 → 构建 ExecutionContext → 委托 ActionExecutor 执行 → 返回每个节点的真实执行结果。
     */
    public ActionDebugResponse debug(Long actionId, ActionDebugRequest request) {
        Action action = findById(actionId);

        // 从 JSON 反序列化节点列表
        List<ActionNode> nodes = parseNodes(action.getNodes());

        if (nodes.isEmpty()) {
            return ActionDebugResponse.fail("Action 没有节点");
        }

        // 构建执行上下文
        ExecutionContext context = new ExecutionContext();
        context.setProjectId(action.getProjectId());
        context.setEnvironmentId(request.getEnvironmentId());

        // 加载环境变量
        try {
            Map<String, String> envVars =
                    environmentService.getVariablesAsMap(request.getEnvironmentId());
            String host = envVars.get("host");
            if (host != null) {
                context.setBaseUrl(host);
            }
            // 将环境变量加载到上下文
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            log.warn("加载环境配置失败: {}", e.getMessage());
        }

        long start = System.currentTimeMillis();

        // 执行 Action
        StepResult result = actionExecutor.executeAction(action, request.getInputParams(), context);

        long elapsed = System.currentTimeMillis() - start;

        // 从 result.response 中提取 nodeResults
        List<Map<String, Object>> nodeResults = new ArrayList<>();
        if (result.getResponse() != null) {
            Object nr = result.getResponse().get("nodeResults");
            if (nr instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> casted = (List<Map<String, Object>>) nr;
                nodeResults = casted;
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", result.getStatus());
        output.put("message", result.getMessage());
        output.put("nodeCount", nodes.size());

        // 输出上下文变量（排除内部变量）
        Map<String, Object> vars = new LinkedHashMap<>(context.getVariables());
        vars.remove("_loopCount");
        vars.remove("_loopIndex");
        if (!vars.isEmpty()) {
            output.put("variables", vars);
        }

        // 按 outputParams 声明筛选输出参数（若未定义则输出全部上下文变量）
        if (action.getOutputParams() != null && !action.getOutputParams().isEmpty()) {
            try {
                List<Map<String, Object>> outputDefs = objectMapper.readValue(
                        action.getOutputParams(), new TypeReference<List<Map<String, Object>>>() {});
                Set<String> declaredNames = new LinkedHashSet<>();
                for (Map<String, Object> def : outputDefs) {
                    String name = (String) def.get("name");
                    if (name != null && !name.isEmpty()) {
                        declaredNames.add(name);
                    }
                }
                if (!declaredNames.isEmpty()) {
                    Map<String, Object> filteredOutputs = new LinkedHashMap<>();
                    for (String name : declaredNames) {
                        if (vars.containsKey(name)) {
                            filteredOutputs.put(name, vars.get(name));
                        }
                    }
                    output.put("outputValues", filteredOutputs);
                }
            } catch (Exception e) {
                log.warn("解析 Action 出参定义失败: {}", e.getMessage());
            }
        }

        return ActionDebugResponse.ok(output, nodeResults, elapsed);
    }

    /**
     * 获取引用该 Action 的自动化用例列表
     */
    public List<Map<String, Object>> getReferences(Long actionId) {
        findById(actionId);
        Keyword actionKeyword = findActionKeyword(actionId);
        if (actionKeyword == null) {
            return Collections.emptyList();
        }

        Long keywordId = actionKeyword.getId();
        LambdaQueryWrapper<AutoCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(AutoCase::getSteps, keywordId)
                .or().like(AutoCase::getSetupSteps, keywordId)
                .or().like(AutoCase::getTeardownSteps, keywordId));
        List<AutoCase> cases = autoCaseMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (AutoCase tc : cases) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("autoCaseId", tc.getId());
            ref.put("caseName", tc.getName());
            ref.put("autoSuiteId", tc.getAutoSuiteId());
            result.add(ref);
        }
        return result;
    }

    /**
     * 清空分组及其子孙分组中的所有 Action（被引用的跳过）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByGroup(Long groupId) {
        Set<Long> groupIds = actionGroupService.getDescendantGroupIds(groupId);
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Action::getGroupId, groupIds);
        deleteUnreferenced(actionMapper.selectList(wrapper));
    }

    /**
     * 清空项目下所有 Action（被引用的跳过）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByProject(Long projectId) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Action::getProjectId, projectId);
        deleteUnreferenced(actionMapper.selectList(wrapper));
    }

    // ───────────────────── 私有方法 ─────────────────────

    /**
     * 批量删除未被引用的 Action，被自动化用例引用的跳过
     */
    private void deleteUnreferenced(List<Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        // ponytail: 引用检查为 JSON 列 like 查询只能逐条计数；量大时可引入冗余引用计数列
        for (Action action : actions) {
            Keyword actionKeyword = findActionKeyword(action.getId());
            if (actionKeyword != null && countAutoCaseReferences(actionKeyword.getId()) > 0) {
                continue;
            }
            if (actionKeyword != null) {
                keywordMapper.deleteById(actionKeyword.getId());
            }
            actionMapper.deleteById(action.getId());
        }
    }

    private Action findById(Long actionId) {
        Action action = actionMapper.selectById(actionId);
        if (action == null) {
            throw new BusinessException(ErrorCode.ACTION_NOT_FOUND, "Action 不存在：" + actionId);
        }
        return action;
    }

    /**
     * DFS 循环引用检测
     * 检查节点间是否存在循环依赖（通过 nodeKey 和 config 中的 nextNode 引用）
     */
    private void detectCircularReferences(List<ActionNodeDTO> nodes) {
        if (nodes == null || nodes.size() <= 1) {
            return;
        }

        // 构建邻接表：nodeKey -> 依赖的 nodeKeys
        Map<String, List<String>> adjacency = new HashMap<>();
        Set<String> allKeys = new HashSet<>();

        for (ActionNodeDTO node : nodes) {
            String key = node.getNodeKey();
            allKeys.add(key);
            adjacency.putIfAbsent(key, new ArrayList<>());

            // 从 config JSON 中解析节点间依赖关系
            if (node.getConfig() != null) {
                try {
                    JsonNode configNode = objectMapper.readTree(node.getConfig());
                    scanConfigForNodeKeys(configNode, key, adjacency, allKeys);
                } catch (Exception e) {
                    log.warn("解析节点配置失败: nodeKey={}, error={}", key, e.getMessage());
                }
            }
        }

        // DFS 检测环
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();

        for (String key : allKeys) {
            if (!visited.contains(key)) {
                if (hasCycleDFS(key, adjacency, visited, inStack)) {
                    throw new BusinessException(ErrorCode.ACTION_CIRCULAR_REFERENCE,
                            "Action 节点中存在循环引用");
                }
            }
        }
    }

    private boolean hasCycleDFS(String node, Map<String, List<String>> adjacency,
                                 Set<String> visited, Set<String> inStack) {
        visited.add(node);
        inStack.add(node);

        List<String> neighbors = adjacency.getOrDefault(node, Collections.emptyList());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                if (hasCycleDFS(neighbor, adjacency, visited, inStack)) {
                    return true;
                }
            } else if (inStack.contains(neighbor)) {
                return true;
            }
        }

        inStack.remove(node);
        return false;
    }

    /**
     * 从 action.nodes JSON 字符串反序列化节点列表
     */
    private List<ActionNode> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ActionNode[] arr = objectMapper.readValue(nodesJson, ActionNode[].class);
            return Arrays.asList(arr);
        } catch (Exception e) {
            log.warn("反序列化 Action 节点失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 查找 Action 对应的 Keyword 记录（type=ACTION, refId=actionId）
     */
    private Keyword findActionKeyword(Long actionId) {
        LambdaQueryWrapper<Keyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Keyword::getType, "ACTION")
                .eq(Keyword::getRefId, actionId)
                .last("LIMIT 1");
        return keywordMapper.selectOne(wrapper);
    }

    /**
     * 统计引用指定关键字的自动化用例数量（搜索 steps / setup_steps / teardown_steps JSON）
     */
    private long countAutoCaseReferences(Long keywordId) {
        LambdaQueryWrapper<AutoCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(AutoCase::getSteps, keywordId)
                .or().like(AutoCase::getSetupSteps, keywordId)
                .or().like(AutoCase::getTeardownSteps, keywordId));
        return autoCaseMapper.selectCount(wrapper);
    }

    /**
     * 递归扫描 config JSON，提取引用其他节点的 nodeKey 并加入邻接表
     */
    private void scanConfigForNodeKeys(JsonNode config, String currentNodeKey,
                                        Map<String, List<String>> adjacency, Set<String> allKeys) {
        if (config.isObject()) {
            Iterator<Entry<String, JsonNode>> fields = config.fields();
            while (fields.hasNext()) {
                JsonNode value = fields.next().getValue();
                if (value.isTextual() && allKeys.contains(value.asText())) {
                    adjacency.get(currentNodeKey).add(value.asText());
                } else {
                    scanConfigForNodeKeys(value, currentNodeKey, adjacency, allKeys);
                }
            }
        } else if (config.isArray()) {
            for (JsonNode element : config) {
                scanConfigForNodeKeys(element, currentNodeKey, adjacency, allKeys);
            }
        }
    }

    private ActionResponse toResponse(Action action) {
        ActionResponse resp = new ActionResponse();
        BeanUtils.copyProperties(action, resp);
        // 计算引用次数：被自动化用例引用的数量
        Keyword actionKeyword = findActionKeyword(action.getId());
        if (actionKeyword != null) {
            resp.setReferenceCount((int) countAutoCaseReferences(actionKeyword.getId()));
        } else {
            resp.setReferenceCount(0);
        }
        return resp;
    }
}
