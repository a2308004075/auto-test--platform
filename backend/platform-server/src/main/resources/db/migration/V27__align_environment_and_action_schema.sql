-- ============================================================
-- V27 environment 表和 action 表结构对齐实体类
--
-- 问题根因：
--   1. Environment 实体类使用显式数据库连接字段（host/port/database_name/
--      username/password/config_json），但 V2 建表时使用 config JSON 列，
--      导致 MyBatis-Plus 生成的 SQL 引用了数据库中不存在的列。
--   2. Action 实体类包含 name/description/input_params/output_params/updated_by
--      字段，但 V2 建表时遗漏了这些列。
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. environment 表对齐 Environment.java 实体类
-- ============================================================

-- 1.1 添加新列
ALTER TABLE `environment`
  ADD COLUMN `host` VARCHAR(200) DEFAULT NULL COMMENT '数据库主机地址' AFTER `name`,
  ADD COLUMN `port` INT DEFAULT NULL COMMENT '数据库端口' AFTER `host`,
  ADD COLUMN `database_name` VARCHAR(100) DEFAULT NULL COMMENT '数据库名称' AFTER `port`,
  ADD COLUMN `username` VARCHAR(100) DEFAULT NULL COMMENT '数据库用户名' AFTER `database_name`,
  ADD COLUMN `password` VARCHAR(200) DEFAULT NULL COMMENT '数据库密码' AFTER `username`,
  ADD COLUMN `config_json` TEXT DEFAULT NULL COMMENT '额外配置（JSON 格式）' AFTER `password`;

-- 1.2 迁移旧数据：将 config JSON 列的数据迁移到 config_json
UPDATE `environment` SET `config_json` = `config` WHERE `config` IS NOT NULL;

-- 1.3 删除旧列（config 为 NOT NULL，不删除会导致 INSERT 失败）
ALTER TABLE `environment`
  DROP COLUMN `config`,
  DROP COLUMN `description`;

-- ============================================================
-- 2. action 表对齐 Action.java 实体类
-- 实体包含 name/description/input_params/output_params/updated_by
-- 这些列在 V2 建表时遗漏，V8 仅补充了 is_active
-- ============================================================

ALTER TABLE `action`
  ADD COLUMN `name` VARCHAR(100) DEFAULT NULL COMMENT 'Action 名称' AFTER `project_id`,
  ADD COLUMN `description` VARCHAR(1000) DEFAULT NULL COMMENT '描述' AFTER `name`,
  ADD COLUMN `input_params` JSON DEFAULT NULL COMMENT '输入参数定义（JSON）' AFTER `is_active`,
  ADD COLUMN `output_params` JSON DEFAULT NULL COMMENT '输出参数定义（JSON）' AFTER `input_params`,
  ADD COLUMN `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID' AFTER `created_by`;

-- 添加 updated_by 索引和外键（与 keyword 表一致）
ALTER TABLE `action`
  ADD KEY `idx_action_updated_by` (`updated_by`),
  ADD CONSTRAINT `fk_action_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`id`) ON DELETE SET NULL;
