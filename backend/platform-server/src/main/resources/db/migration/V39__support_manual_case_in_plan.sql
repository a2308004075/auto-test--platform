-- =====================================================================
-- V39: 测试计划支持手动化用例组成
--   1. test_plan 新增 manual_case_ids 字段；
--   2. test_result 支持记录手动化用例执行结果。
-- =====================================================================

-- ── 1. test_plan 新增关联手动化用例 ID 列表（JSON 数组）──
ALTER TABLE `test_plan`
  ADD COLUMN `manual_case_ids` json DEFAULT NULL COMMENT '关联的手动化用例 ID 列表（JSON 数组）' AFTER `auto_suite_ids`;

-- ── 2. test_result 支持手动化用例 ──
-- 2.1 删除原外键约束，以便将 auto_case_id 改为可空
ALTER TABLE `test_result` DROP FOREIGN KEY `fk_test_result_auto_case_id`;

-- 2.2 将 auto_case_id 改为可空
ALTER TABLE `test_result`
  MODIFY COLUMN `auto_case_id` bigint DEFAULT NULL COMMENT '所属自动化用例 ID（case_type=AUTO 时有值）';

-- 2.3 新增 manual_case_id 与 case_type 字段
ALTER TABLE `test_result`
  ADD COLUMN `manual_case_id` bigint DEFAULT NULL COMMENT '所属手动化用例 ID（case_type=MANUAL 时有值）' AFTER `auto_case_id`,
  ADD COLUMN `case_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AUTO' COMMENT '用例类型：AUTO-自动化用例，MANUAL-手动化用例' AFTER `manual_case_id`;

-- 2.4 为存量数据设置 case_type（auto_case_id 有值的为 AUTO）
UPDATE `test_result` SET `case_type` = 'AUTO' WHERE `auto_case_id` IS NOT NULL;

-- 2.5 重建外键约束
ALTER TABLE `test_result`
  ADD CONSTRAINT `fk_test_result_auto_case_id` FOREIGN KEY (`auto_case_id`) REFERENCES `auto_case` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_test_result_manual_case_id` FOREIGN KEY (`manual_case_id`) REFERENCES `manual_case` (`id`) ON DELETE SET NULL;

-- 2.6 新增索引
ALTER TABLE `test_result`
  ADD KEY `idx_test_result_manual_case_id` (`manual_case_id`),
  ADD KEY `idx_test_result_case_type` (`case_type`);

-- ── 3. 扩展 test_execution 状态枚举，支持“等待手动结果”──
ALTER TABLE `test_execution`
  MODIFY COLUMN `status` enum('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED','QUEUED','WAITING_MANUAL') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '执行状态';

-- ── 4. 更新数据字典：执行状态新增“等待手动结果”──
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('execution_status', '执行状态', 'WAITING_MANUAL', '等待手动结果', 6, '自动化部分已完成，等待手动化用例标记结果', 1, NOW(), NOW());

-- ── 5. 更新数据字典：测试结果状态新增“待执行”──
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('test_result_status', '测试结果状态', 'PENDING', '待执行', 5, '手动化用例初始状态，等待人工标记', 1, NOW(), NOW());
