-- =====================================================================
-- V40: 测试计划区分"手动测试计划"与"自动测试计划"
--   1. test_plan 新增 plan_type 字段（MANUAL / AUTO），存量默认 AUTO；
--   2. sys_dict 新增 plan_type 数据字典。
-- =====================================================================

-- ── 1. test_plan 新增 plan_type 列 ──
ALTER TABLE `test_plan`
  ADD COLUMN `plan_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AUTO' COMMENT '计划类型：MANUAL-手动测试计划，AUTO-自动测试计划' AFTER `description`;

-- 存量数据默认为自动测试计划
UPDATE `test_plan` SET `plan_type` = 'AUTO';

-- ── 2. 数据字典：plan_type ──
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('plan_type', '计划类型', 'AUTO', '自动测试计划', 1, '关联自动化套件', 1, NOW(), NOW()),
('plan_type', '计划类型', 'MANUAL', '手动测试计划', 2, '关联手动化用例', 1, NOW(), NOW());
