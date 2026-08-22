-- ============================================================
-- V16 初始化字典数据
-- 将项目中所有硬编码的枚举/选项值导入 sys_dict 表
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. HTTP 方法
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('http_method', 'HTTP方法', 'GET',    'GET',    1, 'GET 请求'),
('http_method', 'HTTP方法', 'POST',   'POST',   2, 'POST 请求'),
('http_method', 'HTTP方法', 'PUT',    'PUT',    3, 'PUT 请求'),
('http_method', 'HTTP方法', 'DELETE', 'DELETE', 4, 'DELETE 请求'),
('http_method', 'HTTP方法', 'PATCH',  'PATCH',  5, 'PATCH 请求');

-- ============================================================
-- 2. 优先级
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('priority', '优先级', 'P0', 'P0', 1, '最高优先级'),
('priority', '优先级', 'P1', 'P1', 2, '高优先级'),
('priority', '优先级', 'P2', 'P2', 3, '中优先级'),
('priority', '优先级', 'P3', 'P3', 4, '低优先级');

-- ============================================================
-- 3. 执行状态
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('execution_status', '执行状态', 'PENDING',   '待执行',  1, '等待执行'),
('execution_status', '执行状态', 'RUNNING',   '执行中',  2, '正在执行'),
('execution_status', '执行状态', 'COMPLETED', '已完成',  3, '执行完成'),
('execution_status', '执行状态', 'FAILED',    '失败',    4, '执行失败'),
('execution_status', '执行状态', 'CANCELLED', '已取消',  5, '已取消执行');

-- ============================================================
-- 4. 测试结果状态
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('test_result_status', '测试结果状态', 'PASSED',  '通过',  1, '测试通过'),
('test_result_status', '测试结果状态', 'FAILED',  '失败',  2, '测试失败'),
('test_result_status', '测试结果状态', 'SKIPPED', '跳过',  3, '测试跳过'),
('test_result_status', '测试结果状态', 'ERROR',   '错误',  4, '执行错误');

-- ============================================================
-- 5. 触发方式
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('trigger_type', '触发方式', 'MANUAL',    '手动触发',  1, '手动执行'),
('trigger_type', '触发方式', 'SCHEDULED', '定时触发',  2, '定时计划执行'),
('trigger_type', '触发方式', 'CI',        'CI/CD触发', 3, 'CI/CD 流水线触发');

-- ============================================================
-- 6. 关键字类型
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('keyword_type', '关键字类型', 'API',       'API关键字',     1, 'API 接口关键字'),
('keyword_type', '关键字类型', 'TOOL',      '工具方法关键字', 2, '工具方法关键字'),
('keyword_type', '关键字类型', 'ACTION',    'Action关键字',  3, 'Action 关键字'),
('keyword_type', '关键字类型', 'TEST_CASE', '测试用例关键字', 4, '测试用例关键字');

-- ============================================================
-- 7. 接口来源类型
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('source_type', '接口来源', 'SWAGGER_IMPORT', 'Swagger导入', 1, '通过 Swagger 文件导入'),
('source_type', '接口来源', 'MANUAL',         '手动创建',   2, '手动创建接口');

-- ============================================================
-- 8. 参数类型
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('param_type', '参数类型', 'string',  '字符串',  1, '字符串类型'),
('param_type', '参数类型', 'integer', '整数',    2, '整数类型'),
('param_type', '参数类型', 'number',  '数字',    3, '数字类型（含小数）'),
('param_type', '参数类型', 'boolean', '布尔值',  4, '布尔类型'),
('param_type', '参数类型', 'array',   '数组',    5, '数组类型'),
('param_type', '参数类型', 'object',  '对象',    6, '对象类型');

-- ============================================================
-- 9. Action 节点类型
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('action_node_type', 'Action节点类型', 'START',        '开始节点',     1, '流程开始'),
('action_node_type', 'Action节点类型', 'END',          '结束节点',     2, '流程结束'),
('action_node_type', 'Action节点类型', 'API_KEYWORD',  '接口关键字节点', 3, '调用 API 关键字'),
('action_node_type', 'Action节点类型', 'TOOL_METHOD',  '工具方法节点', 4, '调用工具方法'),
('action_node_type', 'Action节点类型', 'CONDITION',    '条件判断节点', 5, '条件分支'),
('action_node_type', 'Action节点类型', 'LOOP',         '循环节点',     6, '循环执行');

-- ============================================================
-- 10. 项目状态
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('project_status', '项目状态', '0', '停用', 1, '项目已停用'),
('project_status', '项目状态', '1', '启用', 2, '项目已启用');

-- ============================================================
-- 11. 用户角色
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('user_role', '用户角色', 'ADMIN',  '管理员',   1, '系统管理员，拥有全部权限'),
('user_role', '用户角色', 'TESTER', '测试人员', 2, '测试人员，仅可查看和执行');

-- ============================================================
-- 12. 菜单类型
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('menu_type', '菜单类型', '1', '目录', 1, '菜单目录'),
('menu_type', '菜单类型', '2', '菜单', 2, '页面菜单'),
('menu_type', '菜单类型', '3', '按钮', 3, '操作按钮');

-- ============================================================
-- 13. 启用/停用状态
-- ============================================================
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`) VALUES
('is_active', '启用状态', '0', '停用', 1, '已停用'),
('is_active', '启用状态', '1', '启用', 2, '已启用');
