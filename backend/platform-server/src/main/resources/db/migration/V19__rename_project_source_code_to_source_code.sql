-- 修正源代码菜单显示名称
-- 兼容部分环境在 V18 执行后菜单名被手动改为"项目源代码"的情况，统一恢复为"源代码"
UPDATE `sys_menu` SET `name` = '源代码', `updated_at` = NOW() WHERE `id` = 82;

-- 同步修正权限名称，保持与菜单名称一致
UPDATE `permission` SET `permission_name` = '源代码', `updated_at` = NOW() WHERE `permission_code` = 'project:repositories';
