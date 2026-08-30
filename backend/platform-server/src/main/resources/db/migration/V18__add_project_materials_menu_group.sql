-- 项目资料目录：项目源代码与接口文档归组为二级菜单
-- 1. sys_menu：新增【项目资料】目录（置于环境配置之后、关键字管理之前）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(90, 10, '项目资料', 1, '', '', NULL, NULL, 2, 1, NOW(), NOW());

-- 2. 测试代码库改名【项目源代码】，与接口文档一并移入【项目资料】目录（权限编码与按钮权限 parent_id 均不变）
UPDATE `sys_menu` SET `name` = '项目源代码', `parent_id` = 90, `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 82;  -- 测试代码库 -> 项目源代码
UPDATE `sys_menu` SET `parent_id` = 90, `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 12;  -- 接口文档

-- 3. permission：同步改名（沿用 V17 权限名称与菜单名称一致的约定，权限编码不变、角色分配不受影响）
UPDATE `permission` SET `permission_name` = '项目源代码', `updated_at` = NOW() WHERE `permission_code` = 'project:repositories';
