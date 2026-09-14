-- 删除 environment 表的 is_current 列和索引（激活功能已移除）
ALTER TABLE `environment` DROP INDEX `idx_environment_is_current`;
ALTER TABLE `environment` DROP COLUMN `is_current`;
