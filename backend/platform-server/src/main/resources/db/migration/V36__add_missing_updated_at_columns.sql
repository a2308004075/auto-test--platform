-- =====================================================================
-- V36: 补齐缺失的 updated_at 列
-- requirement_case_relation (V29)、change_log (V25)、token_blacklist (V1)
-- 三张表的实体类继承 BaseEntity（含 updatedAt 字段），但建表时遗漏了该列，
-- 导致 MyBatis Plus 生成的 SELECT 语句报 Unknown column 'updated_at'
-- =====================================================================

ALTER TABLE `requirement_case_relation`
  ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

ALTER TABLE `change_log`
  ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

ALTER TABLE `token_blacklist`
  ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;
