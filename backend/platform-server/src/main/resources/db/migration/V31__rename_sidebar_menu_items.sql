-- ============================================================
-- V31 侧边栏菜单项重命名
-- 接口管理 → 接口文档
-- 工具方法 → 工具方法关键字
-- Action → Action关键字
-- ============================================================

SET NAMES utf8mb4;

-- 接口管理 → 接口文档
UPDATE `sys_menu` SET `name` = '接口文档' WHERE `id` = 12;

-- 工具方法 → 工具方法关键字
UPDATE `sys_menu` SET `name` = '工具方法关键字' WHERE `id` = 15;

-- Action → Action关键字
UPDATE `sys_menu` SET `name` = 'Action关键字' WHERE `id` = 16;
