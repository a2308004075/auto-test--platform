-- ============================================================
-- V36 测试用例分组管理 + 用例标签字段
--
-- 1. 新建 case_group 表（树形分组，参照 api_module 模式）
-- 2. test_case 表新增 group_id、tags 列
-- 3. sys_menu 新增用例分组管理按钮权限
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 新建 case_group 表
-- ============================================================

CREATE TABLE IF NOT EXISTS `case_group` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '自增主键',
  `project_id`      BIGINT       NOT NULL                     COMMENT '所属项目 ID',
  `parent_id`       BIGINT       DEFAULT NULL                 COMMENT '父分组 ID（null=根分组）',
  `name`            VARCHAR(100) NOT NULL                     COMMENT '分组名称',
  `description`     VARCHAR(500) DEFAULT NULL                 COMMENT '分组描述',
  `is_system`       TINYINT      NOT NULL DEFAULT 0           COMMENT '是否系统默认分组（0-否，1-是）',
  `created_by`      BIGINT       DEFAULT NULL                 COMMENT '创建人 ID',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_case_group_project_id` (`project_id`),
  KEY `idx_case_group_parent_id` (`parent_id`),
  CONSTRAINT `fk_case_group_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_case_group_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例分组表';

-- ============================================================
-- 2. test_case 表新增 group_id、tags 列
-- ============================================================

ALTER TABLE `test_case`
  ADD COLUMN `group_id` BIGINT DEFAULT NULL COMMENT '所属分组 ID' AFTER `suite_id`,
  ADD COLUMN `tags` JSON DEFAULT NULL COMMENT '标签列表' AFTER `priority`;

ALTER TABLE `test_case`
  ADD KEY `idx_test_case_group_id` (`group_id`),
  ADD CONSTRAINT `fk_test_case_group_id` FOREIGN KEY (`group_id`) REFERENCES `case_group` (`id`) ON DELETE SET NULL;

-- ============================================================
-- 3. sys_menu 新增用例分组管理按钮权限
-- ============================================================

-- 找到「测试用例」菜单项（id=18），在其下新增按钮权限
INSERT INTO `sys_menu` (`name`, `parent_id`, `menu_type`, `perms`, `sort_no`)
VALUES ('分组管理', 18, 3, 'project:case:group', 5);
