-- ============================================================
-- V15 在 sys_menu 中新增"角色管理"菜单项
-- 插入到系统管理目录下，排序位于用户列表之后
-- ============================================================

-- 将系统管理下 sort_no >= 3 的菜单后移一位，为角色管理腾出位置
UPDATE `sys_menu` SET `sort_no` = `sort_no` + 1
  WHERE `parent_id` = 1 AND `sort_no` >= 3;

-- 插入角色管理菜单
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `icon`, `route_path`, `sort_no`) VALUES
(1, '角色管理', 2, '', '/settings/roles', 3);
