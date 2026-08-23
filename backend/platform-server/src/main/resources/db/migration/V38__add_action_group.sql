-- ============================================================
-- V38 Action 关键字分组管理
--
-- 1. 新建 action_group 表（树形分组，参照 case_group 表结构 V36）
-- 2. action 表新增 group_id 列
-- 3. sys_menu 新增 Action 分组管理按钮权限
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 新建 action_group 表
-- ============================================================

CREATE TABLE IF NOT EXISTS `action_group` (
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
  KEY `idx_action_group_project_id` (`project_id`),
  KEY `idx_action_group_parent_id` (`parent_id`),
  CONSTRAINT `fk_action_group_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_action_group_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Action 关键字分组表';

-- ============================================================
-- 2. action 表新增 group_id 列
-- ============================================================

ALTER TABLE `action`
  ADD COLUMN `group_id` BIGINT DEFAULT NULL COMMENT '所属分组 ID' AFTER `description`;

ALTER TABLE `action`
  ADD KEY `idx_action_group_id` (`group_id`),
  ADD CONSTRAINT `fk_action_group_id` FOREIGN KEY (`group_id`) REFERENCES `action_group` (`id`) ON DELETE SET NULL;

-- ============================================================
-- 3. sys_menu 新增 Action 分组管理按钮权限
-- ============================================================

-- 找到「Action」菜单项（id=16），在其下新增按钮权限
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `route_path`, `component`, `sort_no`, `permission_code`)
VALUES (16, '分组管理', 3, NULL, NULL, 5, 'project:action:group');
