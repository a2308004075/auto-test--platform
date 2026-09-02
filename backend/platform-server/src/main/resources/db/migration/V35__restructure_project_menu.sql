-- =====================================================================
-- V35: 项目菜单层级重构
-- 按图调整左侧菜单树：
--   1. 项目菜单下新增/恢复【项目资料】分组，归拢需求文档、项目文档、
--      源代码、接口文档、界面元素；
--   2. 测试管理下新增【自动化测试】【手动化测试】两个分组，
--      将关键字管理、自动化用例、自动化套件、执行记录归入自动化测试，
--      将手动化用例归入手动化测试。
-- =====================================================================

-- ── 1. 新增两个测试分组目录（目录类型 menu_type=1，无路由与权限编码） ──
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(119, 78, '自动化测试', 1, '', '', NULL, NULL, 1, 1, NOW(), NOW()),
(120, 78, '手动化测试', 1, '', '', NULL, NULL, 2, 1, NOW(), NOW());

-- ── 2. 确保【项目资料】目录存在（由 V18 创建，若历史环境缺失则补录） ──
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(90, 10, '项目资料', 1, '', '', NULL, NULL, 2, 1, NOW(), NOW());

-- ── 3. 调整项目菜单下的一级目录排序 ──
UPDATE `sys_menu` SET `sort_no` = 0, `updated_at` = NOW() WHERE `id` = 11;  -- 项目概览
UPDATE `sys_menu` SET `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 13;  -- 环境配置
UPDATE `sys_menu` SET `parent_id` = 10, `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 90;  -- 项目资料
UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 78;  -- 测试管理

-- ── 4. 将各资料页面归入【项目资料】(id=90) ──
UPDATE `sys_menu` SET `parent_id` = 90, `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 94;  -- 需求文档（V21）
UPDATE `sys_menu` SET `parent_id` = 90, `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 114; -- 项目文档（V30）
UPDATE `sys_menu` SET `parent_id` = 90, `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 82;  -- 源代码（V16/V18）
UPDATE `sys_menu` SET `parent_id` = 90, `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 12;  -- 接口文档
UPDATE `sys_menu` SET `parent_id` = 90, `sort_no` = 5, `updated_at` = NOW() WHERE `id` = 91;  -- 界面元素（V20）

-- ── 5. 将自动化相关页面归入【自动化测试】(id=119) ──
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 79;  -- 关键字管理
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 18;  -- 自动化用例
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 17;  -- 自动化套件
UPDATE `sys_menu` SET `parent_id` = 119, `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 20;  -- 执行记录

-- ── 6. 关键字管理下子菜单顺序保持不变（已在 V1 中设置） ──
-- 14 接口关键字 / 15 工具方法 / 16 Action关键字

-- ── 7. 将手动化用例归入【手动化测试】(id=120) ──
UPDATE `sys_menu` SET `parent_id` = 120, `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 101; -- 手动化用例（V24/V34）

-- ── 8. 调整测试管理下剩余子菜单排序 ──
UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 19;  -- 测试计划
UPDATE `sys_menu` SET `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 108; -- 缺陷管理（V26）
