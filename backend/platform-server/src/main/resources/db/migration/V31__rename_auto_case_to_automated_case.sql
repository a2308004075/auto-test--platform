-- 测试管理目录：【自动用例】菜单更名为【自动化用例】
UPDATE `sys_menu` SET `name` = '自动化用例', `updated_at` = NOW() WHERE `id` = 18;

-- 同步更新权限名称，保持与菜单名称一致（权限编码不变、角色分配不受影响）
UPDATE `permission` SET `permission_name` = '自动化用例', `updated_at` = NOW() WHERE `permission_code` = 'project:cases';
