-- =====================================================================
-- V38: 项目菜单层级二次重构
-- 按最新菜单树调整：
--   1. 去掉【关键字管理】目录，将其子菜单直接归入【自动化测试】；
--   2. 【自动化测试】【手动化测试】【测试管理】【缺陷管理】提升为项目菜单一级目录；
--   3. 【执行记录】改为【测试记录】，并归属到【测试管理】下。
-- =====================================================================

-- ── 1. 将关键字子菜单从“关键字管理”迁移到“自动化测试” ──
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 14; -- 接口关键字
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 15; -- 工具方法
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 16; -- Action关键字

-- ── 2. 停用并删除“关键字管理”目录（id=79）──
UPDATE `sys_menu` SET `is_active` = 0, `updated_at` = NOW() WHERE `id` = 79;
DELETE FROM `sys_menu` WHERE `id` = 79;

-- ── 3. 将“自动化测试”“手动化测试”“测试管理”“缺陷管理”提升为项目菜单一级目录并排序 ──
UPDATE `sys_menu` SET `parent_id` = 10, `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 119; -- 自动化测试
UPDATE `sys_menu` SET `parent_id` = 10, `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 120; -- 手动化测试
UPDATE `sys_menu` SET `parent_id` = 10, `sort_no` = 5, `updated_at` = NOW() WHERE `id` = 78;  -- 测试管理
UPDATE `sys_menu` SET `parent_id` = 10, `sort_no` = 6, `updated_at` = NOW() WHERE `id` = 108; -- 缺陷管理

-- ── 4. 调整“自动化测试”下的子菜单排序 ──
--    接口关键字/工具方法/Action关键字 已在步骤 1 中设置
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 18; -- 自动化用例
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 5, `updated_at` = NOW() WHERE `id` = 17; -- 自动化套件

-- ── 5. 将“执行记录”从“自动化测试”移到“测试管理”，并改名为“测试记录” ──
UPDATE `sys_menu` SET `parent_id` = 78, `name` = '测试记录', `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 20;

-- ── 6. 调整“测试管理”下的子菜单排序 ──
UPDATE `sys_menu` SET `parent_id` = 78, `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 19;  -- 测试计划
-- 测试记录（id=20）已在步骤 5 中设置

-- ── 7. 同步更新 permission 表中的菜单名称（保持 permission_code 不变）──
UPDATE `permission` SET `permission_name` = '测试记录' WHERE `permission_code` = 'project:executions';
