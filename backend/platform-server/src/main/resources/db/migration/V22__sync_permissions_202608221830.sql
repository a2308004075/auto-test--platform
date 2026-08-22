-- ============================================================
-- 自动生成：权限同步检查脚本
-- 生成时间：2026-08-22 18:30
-- 
-- 使用方法：
--   1. 执行本迁移脚本（或重启后端由 Flyway 自动执行）
--   2. 在角色管理页面点击【同步】按钮，将 sys_menu 同步到 permission 表
--   3. 为需要的角色分配新增权限
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. sys_menu 新增 BUTTON 条目（57 条）
--    执行后在角色管理页面点击【同步】即可自动同步到 permission 表
-- ============================================================

-- 按钮: project:action:edit (保存) → 父菜单: Action [id=16]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (16, '保存', 3, NULL, NULL, 0, 'project:action:edit');

-- 按钮: project:action:add (新建 Action) → 父菜单: Action [id=16]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (16, '新建 Action', 3, NULL, NULL, 0, 'project:action:add');

-- 按钮: project:action:debug (调试) → 父菜单: Action [id=16]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (16, '调试', 3, NULL, NULL, 0, 'project:action:debug');

-- 按钮: project:action:delete (删除) → 父菜单: Action [id=16]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (16, '删除', 3, NULL, NULL, 0, 'project:action:delete');

-- 按钮: project:api:edit (保存) → 父菜单: 接口管理 [id=12]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (12, '保存', 3, NULL, NULL, 0, 'project:api:edit');

-- 按钮: project:api:swagger (导入 Swagger) → 父菜单: 接口管理 [id=12]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (12, '导入 Swagger', 3, NULL, NULL, 0, 'project:api:swagger');

-- 按钮: project:api:add (+ 新建接口) → 父菜单: 接口管理 [id=12]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (12, '+ 新建接口', 3, NULL, NULL, 0, 'project:api:add');

-- 按钮: project:api:group (+ 新建) → 父菜单: 接口管理 [id=12]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (12, '+ 新建', 3, NULL, NULL, 0, 'project:api:group');

-- 按钮: project:api:batch (Batch) → 父菜单: 接口管理 [id=12]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (12, 'Batch', 3, NULL, NULL, 0, 'project:api:batch');

-- 按钮: project:api:delete (删除) → 父菜单: 接口管理 [id=12]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (12, '删除', 3, NULL, NULL, 0, 'project:api:delete');

-- 按钮: project:case:edit (保存) → 父菜单: 测试套件 [id=17]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (17, '保存', 3, NULL, NULL, 0, 'project:case:edit');

-- 按钮: project:case:add (新建用例) → 父菜单: 测试用例 [id=18]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (18, '新建用例', 3, NULL, NULL, 0, 'project:case:add');

-- 按钮: project:case:toggle (Toggle) → 父菜单: 测试用例 [id=18]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (18, 'Toggle', 3, NULL, NULL, 0, 'project:case:toggle');

-- 按钮: project:case:delete (删除) → 父菜单: 测试用例 [id=18]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (18, '删除', 3, NULL, NULL, 0, 'project:case:delete');

-- 按钮: project:suite:edit (保存) → 父菜单: 测试套件 [id=17]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (17, '保存', 3, NULL, NULL, 0, 'project:suite:edit');

-- 按钮: project:suite:add (新建套件) → 父菜单: 测试套件 [id=17]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (17, '新建套件', 3, NULL, NULL, 0, 'project:suite:add');

-- 按钮: project:suite:steps (步骤配置) → 父菜单: 测试套件 [id=17]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (17, '步骤配置', 3, NULL, NULL, 0, 'project:suite:steps');

-- 按钮: project:suite:delete (删除) → 父菜单: 测试套件 [id=17]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (17, '删除', 3, NULL, NULL, 0, 'project:suite:delete');

-- 按钮: project:env:add (新建环境) → 父菜单: 环境配置 [id=13]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (13, '新建环境', 3, NULL, NULL, 0, 'project:env:add');

-- 按钮: project:env:activate (Activate) → 父菜单: 环境配置 [id=13]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (13, 'Activate', 3, NULL, NULL, 0, 'project:env:activate');

-- 按钮: project:env:test (Test) → 父菜单: 环境配置 [id=13]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (13, 'Test', 3, NULL, NULL, 0, 'project:env:test');

-- 按钮: project:env:edit (编辑) → 父菜单: 环境配置 [id=13]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (13, '编辑', 3, NULL, NULL, 0, 'project:env:edit');

-- 按钮: project:env:delete (删除) → 父菜单: 环境配置 [id=13]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (13, '删除', 3, NULL, NULL, 0, 'project:env:delete');

-- 按钮: project:execution:cancel (取消) → 父菜单: 执行记录 [id=20]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (20, '取消', 3, NULL, NULL, 0, 'project:execution:cancel');

-- 按钮: project:plan:add (新建计划) → 父菜单: 测试计划 [id=19]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (19, '新建计划', 3, NULL, NULL, 0, 'project:plan:add');

-- 按钮: project:plan:run (执行) → 父菜单: 测试计划 [id=19]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (19, '执行', 3, NULL, NULL, 0, 'project:plan:run');

-- 按钮: project:plan:edit (编辑) → 父菜单: 测试计划 [id=19]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (19, '编辑', 3, NULL, NULL, 0, 'project:plan:edit');

-- 按钮: project:plan:delete (删除) → 父菜单: 测试计划 [id=19]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (19, '删除', 3, NULL, NULL, 0, 'project:plan:delete');

-- 按钮: project:keyword:edit (保存) → 父菜单: 接口关键字 [id=14]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (14, '保存', 3, NULL, NULL, 0, 'project:keyword:edit');

-- 按钮: project:keyword:from-api (从接口生成) → 父菜单: 接口关键字 [id=14]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (14, '从接口生成', 3, NULL, NULL, 0, 'project:keyword:from-api');

-- 按钮: project:keyword:add (+ 新建关键字) → 父菜单: 接口关键字 [id=14]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (14, '+ 新建关键字', 3, NULL, NULL, 0, 'project:keyword:add');

-- 按钮: project:keyword:batch-delete (Batch-delete) → 父菜单: 接口关键字 [id=14]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (14, 'Batch-delete', 3, NULL, NULL, 0, 'project:keyword:batch-delete');

-- 按钮: project:keyword:delete (删除) → 父菜单: 接口关键字 [id=14]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (14, '删除', 3, NULL, NULL, 0, 'project:keyword:delete');

-- 按钮: home:project:add (+ 新建项目) → 父菜单: 首页 [id=1]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (1, '+ 新建项目', 3, NULL, NULL, 0, 'home:project:add');

-- 按钮: home:project:toggle (Toggle) → 父菜单: 首页 [id=1]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (1, 'Toggle', 3, NULL, NULL, 0, 'home:project:toggle');

-- 按钮: home:project:edit (Edit) → 父菜单: 首页 [id=1]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (1, 'Edit', 3, NULL, NULL, 0, 'home:project:edit');

-- 按钮: home:project:delete (Delete) → 父菜单: 首页 [id=1]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (1, 'Delete', 3, NULL, NULL, 0, 'home:project:delete');

-- 按钮: system:cache:set (设置缓存) → 父菜单: 缓存管理 [id=9]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (9, '设置缓存', 3, NULL, NULL, 0, 'system:cache:set');

-- 按钮: system:cache:delete (删除) → 父菜单: 缓存管理 [id=9]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (9, '删除', 3, NULL, NULL, 0, 'system:cache:delete');

-- 按钮: system:dict:add (新增字典) → 父菜单: 字典管理 [id=8]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (8, '新增字典', 3, NULL, NULL, 0, 'system:dict:add');

-- 按钮: system:dict:delete (批量删除) → 父菜单: 字典管理 [id=8]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (8, '批量删除', 3, NULL, NULL, 0, 'system:dict:delete');

-- 按钮: system:dict:import (导入) → 父菜单: 字典管理 [id=8]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (8, '导入', 3, NULL, NULL, 0, 'system:dict:import');

-- 按钮: system:dict:export (导出) → 父菜单: 字典管理 [id=8]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (8, '导出', 3, NULL, NULL, 0, 'system:dict:export');

-- 按钮: system:dict:edit (编辑) → 父菜单: 字典管理 [id=8]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (8, '编辑', 3, NULL, NULL, 0, 'system:dict:edit');

-- 按钮: system:config:save (保存) → 父菜单: 全局设置 [id=6]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (6, '保存', 3, NULL, NULL, 0, 'system:config:save');

-- 按钮: system:config:test (测试发送) → 父菜单: 全局设置 [id=6]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (6, '测试发送', 3, NULL, NULL, 0, 'system:config:test');

-- 按钮: system:menu:add (新增顶级菜单) → 父菜单: 菜单管理 [id=7]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (7, '新增顶级菜单', 3, NULL, NULL, 0, 'system:menu:add');

-- 按钮: system:menu:import (导入) → 父菜单: 菜单管理 [id=7]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (7, '导入', 3, NULL, NULL, 0, 'system:menu:import');

-- 按钮: system:menu:export (导出) → 父菜单: 菜单管理 [id=7]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (7, '导出', 3, NULL, NULL, 0, 'system:menu:export');

-- 按钮: system:menu:edit (编辑) → 父菜单: 菜单管理 [id=7]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (7, '编辑', 3, NULL, NULL, 0, 'system:menu:edit');

-- 按钮: system:menu:toggle (Toggle) → 父菜单: 菜单管理 [id=7]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (7, 'Toggle', 3, NULL, NULL, 0, 'system:menu:toggle');

-- 按钮: system:menu:delete (删除) → 父菜单: 菜单管理 [id=7]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (7, '删除', 3, NULL, NULL, 0, 'system:menu:delete');

-- 按钮: system:user:toggle (禁用) → 父菜单: 用户列表 [id=4]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (4, '禁用', 3, NULL, NULL, 0, 'system:user:toggle');

-- 按钮: project:tool:add (新建工具) → 父菜单: 工具方法 [id=15]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (15, '新建工具', 3, NULL, NULL, 0, 'project:tool:add');

-- 按钮: project:tool:test (测试) → 父菜单: 工具方法 [id=15]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (15, '测试', 3, NULL, NULL, 0, 'project:tool:test');

-- 按钮: project:tool:edit (编辑) → 父菜单: 工具方法 [id=15]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (15, '编辑', 3, NULL, NULL, 0, 'project:tool:edit');

-- 按钮: project:tool:delete (删除) → 父菜单: 工具方法 [id=15]
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (15, '删除', 3, NULL, NULL, 0, 'project:tool:delete');

-- ============================================================
-- 2. permission 名称更新（7 条）
-- ============================================================

-- 前端按钮文本: "新增" → DB 当前: "新建角色"  [src/views/settings/RoleManagementView.vue]
UPDATE `permission` SET `permission_name` = '新增' WHERE `permission_code` = 'system:role:add';

-- 前端按钮文本: "导入" → DB 当前: "导入角色"  [src/views/settings/RoleManagementView.vue]
UPDATE `permission` SET `permission_name` = '导入' WHERE `permission_code` = 'system:role:import';

-- 前端按钮文本: "导出" → DB 当前: "导出角色"  [src/views/settings/RoleManagementView.vue]
UPDATE `permission` SET `permission_name` = '导出' WHERE `permission_code` = 'system:role:export';

-- 前端按钮文本: "编辑" → DB 当前: "编辑角色"  [src/views/settings/RoleManagementView.vue]
UPDATE `permission` SET `permission_name` = '编辑' WHERE `permission_code` = 'system:role:edit';

-- 前端按钮文本: "+ 新建用户" → DB 当前: "新建用户"  [src/views/settings/UserManagementView.vue]
UPDATE `permission` SET `permission_name` = '+ 新建用户' WHERE `permission_code` = 'system:user:add';

-- 前端按钮文本: "编辑" → DB 当前: "编辑用户"  [src/views/settings/UserManagementView.vue]
UPDATE `permission` SET `permission_name` = '编辑' WHERE `permission_code` = 'system:user:edit';

-- 前端按钮文本: "删除" → DB 当前: "删除用户"  [src/views/settings/UserManagementView.vue]
UPDATE `permission` SET `permission_name` = '删除' WHERE `permission_code` = 'system:user:delete';

-- ============================================================
-- 3. sys_menu permission_code 补充（已由 V20 处理，此处仅作说明）
--    菜单管理(id=7)→system:menu、字典管理(id=8)→system:dict、
--    缓存管理(id=9)→system:cache 已在 V20__add_full_permissions.sql 中设置。
-- ============================================================
