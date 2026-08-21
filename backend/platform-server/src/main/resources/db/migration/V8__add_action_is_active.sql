-- ============================================================
-- V8 为 action 表补充 is_active 列
-- Action 实体已使用 @TableLogic(isActive) 进行逻辑删除，
-- 但 V2 建表时遗漏该列，导致按 project_id 统计 action 数量时 SQL 报错。
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE `action`
  ADD COLUMN `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用/已删除，1-启用/未删除）' AFTER `nodes`,
  ADD KEY `idx_action_is_active` (`is_active`);
