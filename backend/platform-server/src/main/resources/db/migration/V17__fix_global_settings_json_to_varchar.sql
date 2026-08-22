-- ============================================================
-- V17 修复 global_settings.config_value JSON 引号问题
-- 原因：config_value 为 JSON 类型，SELECT 返回带引号的 JSON 文本
--   如 '""' → 返回 ""，'"tls"' → 返回 "tls"
-- 方案：将列类型从 JSON 改为 VARCHAR，并用 JSON_UNQUOTE 清洗数据
-- ============================================================

SET NAMES utf8mb4;

-- 1. 添加临时 VARCHAR 列
ALTER TABLE `global_settings` ADD COLUMN `config_value_text` VARCHAR(2000) DEFAULT '' COMMENT '配置值（临时）';

-- 2. 用 JSON_UNQUOTE 将 JSON 值转换为纯文本
UPDATE `global_settings` SET `config_value_text` = JSON_UNQUOTE(`config_value`);

-- 3. 删除原 JSON 列
ALTER TABLE `global_settings` DROP COLUMN `config_value`;

-- 4. 重命名临时列为 config_value 并设置约束
ALTER TABLE `global_settings` CHANGE `config_value_text` `config_value` VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '配置值';
