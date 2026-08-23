-- ============================================================
-- V35 环境配置重构：数据库连接模型 → 键值变量模型
--
-- 1. environment 表：删除数据库连接列，新增 description 列
-- 2. 新建 environment_variable 表（键值对存储）
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. environment 表精简
-- ============================================================

-- 1.1 新增 description 列
ALTER TABLE `environment`
  ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '环境描述' AFTER `name`;

-- 1.2 删除数据库连接相关列
ALTER TABLE `environment`
  DROP COLUMN `host`,
  DROP COLUMN `port`,
  DROP COLUMN `database_name`,
  DROP COLUMN `username`,
  DROP COLUMN `password`,
  DROP COLUMN `config_json`;

-- ============================================================
-- 2. 新建环境变量表（键值对）
-- ============================================================

CREATE TABLE IF NOT EXISTS `environment_variable` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `environment_id`  BIGINT       NOT NULL               COMMENT '所属环境 ID',
  `var_key`         VARCHAR(100) NOT NULL               COMMENT '变量名',
  `var_value`       TEXT         DEFAULT NULL            COMMENT '变量值',
  `description`     VARCHAR(500) DEFAULT NULL            COMMENT '变量描述',
  `sort_no`         INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_env_var_env_id` (`environment_id`),
  CONSTRAINT `fk_env_var_environment_id` FOREIGN KEY (`environment_id`)
    REFERENCES `environment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='环境变量表';
