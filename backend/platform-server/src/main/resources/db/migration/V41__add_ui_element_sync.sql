-- 界面元素模块：新增"同步"按钮权限（用于仓库右键菜单重新解析界面元素）

-- 1. sys_menu：【界面元素】菜单下新增【同步】按钮权限（置于导入之后、删除之前）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(121, 91, '同步', 3, NULL, NULL, NULL, 'project:ui:sync', 2, 1, NOW(), NOW());

-- 调整【删除】按钮排序（从 2 顺延到 3）
UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 93;

-- 2. permission：与 sys_menu 同步（沿用 V1 初始化模式，挂 parent=100 即界面元素页面）
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`) VALUES
(129, '同步', 'project:ui:sync', 'BUTTON', 100, NULL, 3, 1, '同步界面元素按钮', 'display', NOW(), NOW());

-- 3. role_permission：ADMIN 分配同步按钮权限
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`) VALUES
(1, 129, 'enabled', NOW());
