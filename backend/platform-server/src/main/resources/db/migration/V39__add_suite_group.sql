-- ============================================================
-- V39 测试套件分组功能
--
-- 1. 新建 suite_group 表（树形分组，parent_id 自引用）
-- 2. test_suite 表新增 group_id 列
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 新建套件分组表
-- ============================================================

CREATE TABLE IF NOT EXISTS `suite_group` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`      BIGINT       NOT NULL               COMMENT '所属项目 ID',
  `parent_id`       BIGINT       DEFAULT NULL            COMMENT '父分组 ID（NULL 表示顶层分组）',
  `name`            VARCHAR(50)  NOT NULL                COMMENT '分组名称',
  `sort_no`         INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_suite_group_project_id` (`project_id`),
  KEY `idx_suite_group_parent_id` (`parent_id`),
  CONSTRAINT `fk_suite_group_project_id` FOREIGN KEY (`project_id`)
    REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_suite_group_parent_id` FOREIGN KEY (`parent_id`)
    REFERENCES `suite_group` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试套件分组表';

-- ============================================================
-- 2. test_suite 表新增 group_id 列
-- ============================================================

ALTER TABLE `test_suite`
  ADD COLUMN `group_id` BIGINT DEFAULT NULL COMMENT '所属分组 ID（NULL 表示未分组）' AFTER `priority`;

ALTER TABLE `test_suite`
  ADD KEY `idx_test_suite_group_id` (`group_id`),
  ADD CONSTRAINT `fk_test_suite_group_id` FOREIGN KEY (`group_id`)
    REFERENCES `suite_group` (`id`) ON DELETE SET NULL;
