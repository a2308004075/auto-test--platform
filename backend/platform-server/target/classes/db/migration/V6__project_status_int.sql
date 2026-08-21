-- ============================================================
-- V6 将 project 表 is_active (BOOLEAN) 改为 status (TINYINT)
-- 0 = 停用，1 = 启用
-- ============================================================

SET NAMES utf8mb4;

-- 新增 status 列
ALTER TABLE `project`
  ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用' AFTER `source_path`;

-- 迁移现有数据：is_active TRUE → 1, FALSE → 0
UPDATE `project` SET `status` = CASE WHEN `is_active` = TRUE THEN 1 ELSE 0 END;

-- 删除旧列
ALTER TABLE `project`
  DROP INDEX `idx_project_is_active`,
  DROP COLUMN `is_active`;

-- 为新列添加索引
ALTER TABLE `project`
  ADD KEY `idx_project_status` (`status`);
