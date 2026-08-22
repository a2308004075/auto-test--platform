-- ============================================================
-- V20 全量权限导入：补齐所有页面和按钮权限
-- 1. permission 表新增 65 条记录（ID 28-92）
-- 2. sys_menu 表补充缺失的 permission_code 映射
-- 3. ADMIN 角色关联全部新增权限
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 新增 MENU 权限（3 条）
-- ============================================================

INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `description`, `control_mode`) VALUES
(28, '菜单管理', 'system:menu',  'MENU', 2, '/settings/menu',  5, '菜单管理页面', 'display'),
(29, '字典管理', 'system:dict',  'MENU', 2, '/settings/dict',  6, '字典管理页面', 'display'),
(30, '缓存管理', 'system:cache', 'MENU', 2, '/settings/cache', 7, '缓存管理页面', 'display');

-- ============================================================
-- 2. 新增 BUTTON 权限（62 条）
-- ============================================================

-- ----- 用户管理 (parent=4) 新增 1 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(31, '启停用户', 'system:user:toggle', 'BUTTON', 4, 5, '禁用/启用用户按钮', 'display');

-- ----- 角色管理 (parent=5) 新增 1 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(32, '启停角色', 'system:role:toggle', 'BUTTON', 5, 7, '启用/停用角色按钮', 'display');

-- ----- 全局设置 (parent=6) 新增 2 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(33, '保存配置', 'system:config:save', 'BUTTON', 6, 1, '保存全局配置按钮', 'display'),
(34, '测试发送', 'system:config:test', 'BUTTON', 6, 2, '测试邮件/Webhook发送按钮', 'display');

-- ----- 菜单管理 (parent=28) 新增 6 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(35, '新增菜单', 'system:menu:add',    'BUTTON', 28, 1, '新增顶级/子菜单按钮', 'display'),
(36, '编辑菜单', 'system:menu:edit',   'BUTTON', 28, 2, '编辑菜单按钮', 'display'),
(37, '删除菜单', 'system:menu:delete', 'BUTTON', 28, 3, '删除菜单按钮', 'display'),
(38, '启停菜单', 'system:menu:toggle', 'BUTTON', 28, 4, '启用/停用菜单按钮', 'display'),
(39, '导入菜单', 'system:menu:import', 'BUTTON', 28, 5, 'Excel 导入菜单按钮', 'display'),
(40, '导出菜单', 'system:menu:export', 'BUTTON', 28, 6, 'Excel 导出菜单按钮', 'display');

-- ----- 字典管理 (parent=29) 新增 5 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(41, '新增字典', 'system:dict:add',    'BUTTON', 29, 1, '新增字典按钮', 'display'),
(42, '编辑字典', 'system:dict:edit',   'BUTTON', 29, 2, '编辑字典按钮', 'display'),
(43, '删除字典', 'system:dict:delete', 'BUTTON', 29, 3, '批量删除字典按钮', 'display'),
(44, '导入字典', 'system:dict:import', 'BUTTON', 29, 4, 'Excel 导入字典按钮', 'display'),
(45, '导出字典', 'system:dict:export', 'BUTTON', 29, 5, 'Excel 导出字典按钮', 'display');

-- ----- 缓存管理 (parent=30) 新增 2 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(46, '设置缓存', 'system:cache:set',    'BUTTON', 30, 1, '设置缓存按钮', 'display'),
(47, '删除缓存', 'system:cache:delete', 'BUTTON', 30, 2, '删除缓存按钮', 'display');

-- ----- 首页/项目管理 (parent=1) 新增 4 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(48, '新建项目', 'home:project:add',    'BUTTON', 1, 1, '新建项目按钮', 'display'),
(49, '编辑项目', 'home:project:edit',   'BUTTON', 1, 2, '编辑项目按钮', 'display'),
(50, '删除项目', 'home:project:delete', 'BUTTON', 1, 3, '删除项目按钮', 'display'),
(51, '启停项目', 'home:project:toggle', 'BUTTON', 1, 4, '启用/停用项目按钮', 'display');

-- ----- 接口管理 (parent=19) 新增 6 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(52, '导入Swagger',   'project:api:swagger', 'BUTTON', 19, 1, 'Swagger 导入按钮', 'display'),
(53, '新建接口',      'project:api:add',     'BUTTON', 19, 2, '新建接口按钮', 'display'),
(54, '编辑接口',      'project:api:edit',    'BUTTON', 19, 3, '编辑接口按钮', 'display'),
(55, '删除接口',      'project:api:delete',  'BUTTON', 19, 4, '删除接口按钮', 'display'),
(56, '分组管理',      'project:api:group',   'BUTTON', 19, 5, '新建/编辑分组按钮', 'display'),
(57, '批量操作',      'project:api:batch',   'BUTTON', 19, 6, '批量修改/删除按钮', 'display');

-- ----- 环境配置 (parent=20) 新增 5 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(58, '新建环境', 'project:env:add',      'BUTTON', 20, 1, '新建环境按钮', 'display'),
(59, '编辑环境', 'project:env:edit',     'BUTTON', 20, 2, '编辑环境按钮', 'display'),
(60, '删除环境', 'project:env:delete',   'BUTTON', 20, 3, '删除环境按钮', 'display'),
(61, '激活环境', 'project:env:activate', 'BUTTON', 20, 4, '激活/取消激活按钮', 'display'),
(62, '测试环境', 'project:env:test',     'BUTTON', 20, 5, '测试环境连接按钮', 'display');

-- ----- 接口关键字 (parent=21) 新增 5 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(63, '新建关键字',  'project:keyword:add',          'BUTTON', 21, 1, '新建关键字按钮', 'display'),
(64, '编辑关键字',  'project:keyword:edit',         'BUTTON', 21, 2, '编辑关键字按钮', 'display'),
(65, '删除关键字',  'project:keyword:delete',       'BUTTON', 21, 3, '删除关键字按钮', 'display'),
(66, '批量删除',    'project:keyword:batch-delete', 'BUTTON', 21, 4, '批量删除关键字按钮', 'display'),
(67, '从接口生成',  'project:keyword:from-api',     'BUTTON', 21, 5, '从接口生成关键字按钮', 'display');

-- ----- 工具方法 (parent=22) 新增 4 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(68, '新建工具', 'project:tool:add',   'BUTTON', 22, 1, '新建工具按钮', 'display'),
(69, '编辑工具', 'project:tool:edit',  'BUTTON', 22, 2, '编辑工具按钮', 'display'),
(70, '删除工具', 'project:tool:delete','BUTTON', 22, 3, '删除工具按钮', 'display'),
(71, '测试工具', 'project:tool:test',  'BUTTON', 22, 4, '测试工具按钮', 'display');

-- ----- Action (parent=23) 新增 4 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(72, '新建Action',  'project:action:add',   'BUTTON', 23, 1, '新建 Action 按钮', 'display'),
(73, '编辑Action',  'project:action:edit',  'BUTTON', 23, 2, '编辑 Action 按钮', 'display'),
(74, '删除Action',  'project:action:delete','BUTTON', 23, 3, '删除 Action 按钮', 'display'),
(75, '调试Action',  'project:action:debug', 'BUTTON', 23, 4, '调试 Action 按钮', 'display');

-- ----- 测试套件 (parent=24) 新增 4 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(76, '新建套件', 'project:suite:add',   'BUTTON', 24, 1, '新建套件按钮', 'display'),
(77, '编辑套件', 'project:suite:edit',  'BUTTON', 24, 2, '编辑套件按钮', 'display'),
(78, '删除套件', 'project:suite:delete','BUTTON', 24, 3, '删除套件按钮', 'display'),
(79, '步骤配置', 'project:suite:steps', 'BUTTON', 24, 4, '步骤配置按钮', 'display');

-- ----- 测试用例 (parent=25) 新增 4 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(80, '新建用例', 'project:case:add',    'BUTTON', 25, 1, '新建用例按钮', 'display'),
(81, '编辑用例', 'project:case:edit',   'BUTTON', 25, 2, '编辑用例按钮', 'display'),
(82, '删除用例', 'project:case:delete', 'BUTTON', 25, 3, '删除用例按钮', 'display'),
(83, '启停用例', 'project:case:toggle', 'BUTTON', 25, 4, '禁用/启用用例按钮', 'display');

-- ----- 测试计划 (parent=26) 新增 4 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(84, '新建计划', 'project:plan:add',   'BUTTON', 26, 1, '新建计划按钮', 'display'),
(85, '编辑计划', 'project:plan:edit',  'BUTTON', 26, 2, '编辑计划按钮', 'display'),
(86, '删除计划', 'project:plan:delete','BUTTON', 26, 3, '删除计划按钮', 'display'),
(87, '执行计划', 'project:plan:run',   'BUTTON', 26, 4, '执行计划按钮', 'display');

-- ----- 执行记录 (parent=27) 新增 2 条 -----
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `sort_order`, `description`, `control_mode`) VALUES
(88, '取消执行', 'project:execution:cancel', 'BUTTON', 27, 1, '取消执行按钮', 'display'),
(89, '查看详情', 'project:execution:detail', 'BUTTON', 27, 2, '查看详情按钮', 'display');

-- ============================================================
-- 3. sys_menu 补充 permission_code 映射
-- ============================================================

UPDATE `sys_menu` SET `permission_code` = 'system:menu'  WHERE `id` = 7;
UPDATE `sys_menu` SET `permission_code` = 'system:dict'  WHERE `id` = 8;
UPDATE `sys_menu` SET `permission_code` = 'system:cache' WHERE `id` = 9;

-- ============================================================
-- 4. ADMIN 角色（role_id=1）关联全部新增权限
-- ============================================================

INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `id` BETWEEN 28 AND 89;
