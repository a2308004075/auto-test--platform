package com.platform.action.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.action.dto.*;
import com.platform.action.entity.Action;
import com.platform.action.entity.ActionNode;
import com.platform.action.mapper.ActionMapper;
import com.platform.action.mapper.ActionNodeMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.execution.entity.TestCase;
import com.platform.execution.mapper.TestCaseMapper;
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
    private final ActionNodeMapper actionNodeMapper;
    private final KeywordMapper keywordMapper;
    private final TestCaseMapper testCaseMapper;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询 Action 列表
     */
    public PageResponse<ActionResponse> list(Long projectId, String keyword,
                                               int page, int pageSize) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Action::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Action::getName, keyword)
                    .or().like(Action::getDescription, keyword));
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

        // 保存节点到 action_node 表
        saveNodes(action.getId(), request.getNodes());

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
            // 重建节点
            deleteNodes(actionId);
            saveNodes(actionId, request.getNodes());
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

        // 删除保护检查 - 被 test_case_step 引用时不可删除
        Keyword actionKeyword = findActionKeyword(actionId);
        if (actionKeyword != null) {
            long refCount = countTestCaseReferences(actionKeyword.getId());
            if (refCount > 0) {
                throw new BusinessException(ErrorCode.ACTION_DEPENDENCY_CONFLICT,
                        "Action 被 " + refCount + " 个测试用例引用，无法删除");
            }
        }

        deleteNodes(actionId);
        actionMapper.deleteById(actionId);
    }

    /**
     * 调试执行 Action
     */
    public ActionDebugResponse debug(Long actionId, ActionDebugRequest request) {
        Action action = findById(actionId);

        // 获取节点列表
        LambdaQueryWrapper<ActionNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(ActionNode::getActionId, actionId)
                .orderByAsc(ActionNode::getPositionY);
        List<ActionNode> nodes = actionNodeMapper.selectList(nodeWrapper);

        if (nodes.isEmpty()) {
            return ActionDebugResponse.fail("Action 没有节点");
        }

        long start = System.currentTimeMillis();
        List<Map<String, Object>> nodeResults = new ArrayList<>();

        // 简化执行：按顺序执行每个节点
        for (ActionNode node : nodes) {
            Map<String, Object> nodeResult = new LinkedHashMap<>();
            nodeResult.put("nodeKey", node.getNodeKey());
            nodeResult.put("nodeType", node.getNodeType());
            nodeResult.put("status", "SKIPPED");
            nodeResult.put("message", "调试模式：节点执行引擎待 M8 完成后实现");
            nodeResults.add(nodeResult);
        }

        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("message", "调试执行完成（节点执行引擎待完善）");
        output.put("nodeCount", nodes.size());

        return ActionDebugResponse.ok(output, nodeResults, elapsed);
    }

    /**
     * 获取引用该 Action 的用例列表
     */
    public List<Map<String, Object>> getReferences(Long actionId) {
        findById(actionId);
        Keyword actionKeyword = findActionKeyword(actionId);
        if (actionKeyword == null) {
            return Collections.emptyList();
        }

        Long keywordId = actionKeyword.getId();
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(TestCase::getSteps, keywordId)
                .or().like(TestCase::getSetupSteps, keywordId)
                .or().like(TestCase::getTeardownSteps, keywordId));
        List<TestCase> cases = testCaseMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (TestCase tc : cases) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("caseId", tc.getId());
            ref.put("caseName", tc.getName());
            ref.put("suiteId", tc.getSuiteId());
            result.add(ref);
        }
        return result;
    }

    // ───────────────────── 私有方法 ─────────────────────

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

    private void saveNodes(Long actionId, List<ActionNodeDTO> nodeDTOs) {
        if (nodeDTOs == null || nodeDTOs.isEmpty()) {
            return;
        }
        for (ActionNodeDTO dto : nodeDTOs) {
            ActionNode node = new ActionNode();
            node.setActionId(actionId);
            node.setNodeKey(dto.getNodeKey());
            node.setNodeType(dto.getNodeType());
            node.setRefKeywordId(dto.getRefKeywordId());
            node.setRefToolId(dto.getRefToolId());
            node.setConfig(dto.getConfig());
            node.setPositionX(dto.getPositionX());
            node.setPositionY(dto.getPositionY());
            actionNodeMapper.insert(node);
        }
    }

    private void deleteNodes(Long actionId) {
        LambdaQueryWrapper<ActionNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActionNode::getActionId, actionId);
        actionNodeMapper.delete(wrapper);
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
     * 统计引用指定关键字的测试用例数量（搜索 steps / setup_steps / teardown_steps JSON）
     */
    private long countTestCaseReferences(Long keywordId) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(TestCase::getSteps, keywordId)
                .or().like(TestCase::getSetupSteps, keywordId)
                .or().like(TestCase::getTeardownSteps, keywordId));
        return testCaseMapper.selectCount(wrapper);
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
        // 计算引用次数：被测试用例引用的数量
        Keyword actionKeyword = findActionKeyword(action.getId());
        if (actionKeyword != null) {
            resp.setReferenceCount((int) countTestCaseReferences(actionKeyword.getId()));
        } else {
            resp.setReferenceCount(0);
        }
        return resp;
    }
}
