-- =====================================================================
-- V29: 新增需求条目与用例互相关联功能
-- 包含：需求-用例关联表、case_type/defect_target_type/requirement_item_status 字典
-- =====================================================================

-- ── 1. 创建需求-用例关联表 ──
CREATE TABLE `requirement_case_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `requirement_item_id` bigint NOT NULL COMMENT '需求条目 ID',
  `case_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用例类型：MANUAL_CASE-手动用例，TEST_CASE-自动用例',
  `case_id` bigint NOT NULL COMMENT '用例 ID',
  `case_title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用例标题快照',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_req_case` (`requirement_item_id`,`case_type`,`case_id`),
  KEY `idx_req_case_relation_case` (`case_type`,`case_id`),
  CONSTRAINT `fk_req_case_relation_item` FOREIGN KEY (`requirement_item_id`) REFERENCES `requirement_item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_req_case_relation_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求-用例关联表';

-- ── 2. 新增字典 ──
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('case_type', '用例类型', 'MANUAL_CASE', '手动用例', 1, '手动编写的测试用例', 1, NOW(), NOW()),
('case_type', '用例类型', 'TEST_CASE', '自动用例', 2, '自动化测试用例', 1, NOW(), NOW()),
('defect_target_type', '缺陷关联目标类型', 'TEST_CASE', '自动用例', 1, '自动化测试用例', 1, NOW(), NOW()),
('defect_target_type', '缺陷关联目标类型', 'MANUAL_CASE', '手动用例', 2, '手动编写的测试用例', 1, NOW(), NOW()),
('defect_target_type', '缺陷关联目标类型', 'TEST_PLAN', '测试计划', 3, '测试计划', 1, NOW(), NOW()),
('defect_target_type', '缺陷关联目标类型', 'TEST_EXECUTION', '执行记录', 4, '测试执行记录', 1, NOW(), NOW()),
('requirement_item_status', '需求条目状态', 'PENDING', '待处理', 1, '待处理', 1, NOW(), NOW()),
('requirement_item_status', '需求条目状态', 'IN_PROGRESS', '进行中', 2, '进行中', 1, NOW(), NOW()),
('requirement_item_status', '需求条目状态', 'COMPLETED', '已完成', 3, '已完成', 1, NOW(), NOW());
