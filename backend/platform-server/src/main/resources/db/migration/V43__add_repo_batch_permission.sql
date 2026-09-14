-- V43: 源代码模块新增「批量操作」按钮权限码 project:repo:batch
-- 对齐接口文档 project:api:batch 权限模式（sys_menu + permission + role_permission）

-- 1. sys_menu：批量操作按钮（parent=82 源代码，menu_type=3 按钮，sort_no=7 紧随分组管理(6)之后）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(123, 82, '批量操作', 3, NULL, NULL, NULL, 'project:repo:batch', 7, 1, NOW(), NOW());

-- 2. permission：BUTTON 类型（parent=91 源代码根权限）
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`) VALUES
(131, '批量操作', 'project:repo:batch', 'BUTTON', 91, NULL, 7, 1, '源代码批量操作按钮', 'display', NOW(), NOW());

-- 3. role_permission：为 ADMIN 角色授予批量操作权限（enabled）
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`) VALUES
(1, 131, 'enabled', NOW());
