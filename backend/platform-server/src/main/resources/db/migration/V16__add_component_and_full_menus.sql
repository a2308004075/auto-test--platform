-- ============================================================
-- V16 菜单完全动态化：添加组件字段，扩充首页与项目菜单
-- 1. sys_menu 表新增 component 列（前端组件路径，驱动动态路由）
-- 2. 重新填充菜单数据：首页 + 系统管理 + 项目菜单
-- ============================================================

SET NAMES utf8mb4;

-- 新增组件字段
ALTER TABLE `sys_menu` ADD COLUMN `component` VARCHAR(100) DEFAULT NULL COMMENT '前端组件路径（用于动态路由）' AFTER `route_path`;

-- 清除旧菜单数据（不影响 permission 表，两表无 FK 关联）
DELETE FROM `sys_menu`;
ALTER TABLE `sys_menu` AUTO_INCREMENT = 1;

-- ============================================================
-- 重新填充完整菜单数据
-- ============================================================

-- 首页（顶级菜单，无侧边栏）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `sort_no`) VALUES
(1, 0, '首页', 2, 'House', '/home', 'project/ProjectList', 0);

-- 系统管理（顶级目录）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `sort_no`) VALUES
(2, 0, '系统管理', 1, 'Setting', '', NULL, 1);

-- 系统管理 → 子菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `sort_no`) VALUES
(3,  2, '个人资料', 2, '', '/settings/profile',       'settings/ProfileView',       1),
(4,  2, '用户列表', 2, '', '/settings/users',         'settings/UserManagementView', 2),
(5,  2, '角色管理', 2, '', '/settings/roles',         'settings/RoleManagementView', 3),
(6,  2, '全局设置', 2, '', '/settings/global-config', 'settings/GlobalConfigView',   4),
(7,  2, '菜单管理', 2, '', '/settings/menu',          'settings/MenuManagementView', 5),
(8,  2, '字典管理', 2, '', '/settings/dict',          'settings/DictManagementView', 6),
(9,  2, '缓存管理', 2, '', '/settings/cache',         'settings/CacheManagementView', 7);

-- 项目菜单（顶级目录，项目内页面）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `sort_no`) VALUES
(10, 0, '项目菜单', 1, '', '', NULL, 2);

-- 项目菜单 → 子菜单（路径含 :id 占位符，运行时替换为项目 ID）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `sort_no`) VALUES
(11, 10, '仪表板',     2, '', '/project/:id/dashboard',    'project/ProjectDashboard',   1),
(12, 10, '接口管理',   2, '', '/project/:id/apis',         'api/ApiList',                2),
(13, 10, '环境配置',   2, '', '/project/:id/environments', 'environment/EnvironmentList', 3),
(14, 10, '接口关键字', 2, '', '/project/:id/keywords',     'keywords/KeywordList',       4),
(15, 10, '工具方法',   2, '', '/project/:id/tools',        'tool/ToolList',              5),
(16, 10, 'Action',    2, '', '/project/:id/actions',      'action/ActionList',          6),
(17, 10, '测试套件',   2, '', '/project/:id/suites',       'cases/SuiteList',            7),
(18, 10, '测试用例',   2, '', '/project/:id/cases',        'cases/CaseList',             8),
(19, 10, '测试计划',   2, '', '/project/:id/plans',        'execution/PlanList',         9),
(20, 10, '执行记录',   2, '', '/project/:id/executions',   'execution/ExecutionList',    10);
