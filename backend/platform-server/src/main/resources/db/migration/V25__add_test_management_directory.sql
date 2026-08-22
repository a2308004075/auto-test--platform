-- ============================================================
-- V25 侧边栏重组：新增「测试管理」目录
-- 将项目菜单下 9 个测试相关页面归入测试管理目录
-- 目标结构：
--   项目菜单
--   ├── 仪表板
--   └── 测试管理（目录）
--       ├── 环境配置
--       ├── 接口管理
--       ├── 接口关键字
--       ├── 工具方法
--       ├── Action
--       ├── 测试用例
--       ├── 测试套件
--       ├── 测试计划
--       └── 执行记录
-- ============================================================

SET NAMES utf8mb4;

-- 1. 新增「测试管理」目录（menu_type=1）
INSERT INTO `sys_menu` (`name`, `parent_id`, `menu_type`, `icon`, `route_path`, `component`, `sort_no`)
VALUES ('测试管理', 10, 1, '', '', NULL, 0);

-- 2. 将 9 个页面菜单的 parent_id 改为「测试管理」目录，并按指定顺序重新排序
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 1 WHERE `id` = 13; -- 环境配置
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 2 WHERE `id` = 12; -- 接口管理
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 3 WHERE `id` = 14; -- 接口关键字
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 4 WHERE `id` = 15; -- 工具方法
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 5 WHERE `id` = 16; -- Action
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 6 WHERE `id` = 18; -- 测试用例
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 7 WHERE `id` = 17; -- 测试套件
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 8 WHERE `id` = 19; -- 测试计划
UPDATE `sys_menu` SET `parent_id` = LAST_INSERT_ID(), `sort_no` = 9 WHERE `id` = 20; -- 执行记录
