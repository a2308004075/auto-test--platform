-- 分组与查询功能对齐接口文档：套件分组描述字段 + 用例/套件/计划分组管理权限补齐
-- 1. suite_group：新增描述字段（与 case_group/plan_group 的 description 对齐）
ALTER TABLE `suite_group`
  ADD COLUMN `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分组描述' AFTER `name`;

-- 2. permission：补齐用例/套件/计划的分组管理按钮权限（沿用 V1 初始化模式，parent 为对应页面菜单）
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`) VALUES
(97, '分组管理', 'project:case:group', 'BUTTON', 25, NULL, 5, 1, '新建/编辑分组按钮', 'display', NOW(), NOW()),
(98, '分组管理', 'project:suite:group', 'BUTTON', 24, NULL, 5, 1, '新建/编辑分组按钮', 'display', NOW(), NOW()),
(99, '分组管理', 'project:plan:group', 'BUTTON', 26, NULL, 5, 1, '新建/编辑分组按钮', 'display', NOW(), NOW());

-- 3. sys_menu：与 permission 同步（套件/计划页面的分组管理按钮，用例已有 id=80 无需重复）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(88, 17, '分组管理', 3, NULL, NULL, NULL, 'project:suite:group', 5, 1, NOW(), NOW()),
(89, 19, '分组管理', 3, NULL, NULL, NULL, 'project:plan:group', 5, 1, NOW(), NOW());

-- 4. role_permission：ADMIN 分配分组管理按钮权限（与 V16 项目级按钮分配模式一致）
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`) VALUES
(1, 97, 'enabled', NOW()),
(1, 98, 'enabled', NOW()),
(1, 99, 'enabled', NOW());
