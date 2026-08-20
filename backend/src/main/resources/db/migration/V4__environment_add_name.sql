-- V4: 补充字段
-- environment 表需要 name 字段用于环境标识（如"开发环境"、"测试环境"）
ALTER TABLE `environment` ADD COLUMN `name` VARCHAR(100) NOT NULL DEFAULT '' AFTER `project_id`;
