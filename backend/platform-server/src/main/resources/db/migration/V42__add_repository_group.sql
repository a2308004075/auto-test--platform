-- =====================================================================
-- V42: 源代码模块新增分组功能
-- 包含：仓库分组表、code_repository 表增加 group_id 列、
--       存量项目初始化系统分组（全部/未分组）、存量仓库回填分组、
--       菜单与按钮权限（project:repo:group）
-- =====================================================================

-- ── 1. 创建仓库分组表（对齐 api_module 结构，去掉服务前缀/Swagger 字段） ──
CREATE TABLE `code_repository_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分组 ID（null=根分组）',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分组描述',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统默认分组（0-否，1-是）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_repo_group_project_name` (`project_id`,`name`),
  KEY `idx_code_repo_group_project_id` (`project_id`),
  KEY `idx_code_repo_group_parent_id` (`parent_id`),
  CONSTRAINT `fk_code_repo_group_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码仓库分组表';

-- ── 2. 为每个已有项目初始化系统分组（全部/未分组，含软删项目，与 ProjectService 文案一致） ──
INSERT INTO `code_repository_group` (`project_id`, `parent_id`, `name`, `description`, `is_system`, `created_at`, `updated_at`)
SELECT `id`, NULL, '全部', '系统默认分组，包含所有仓库', 1, NOW(), NOW() FROM `project`;

INSERT INTO `code_repository_group` (`project_id`, `parent_id`, `name`, `description`, `is_system`, `created_at`, `updated_at`)
SELECT `id`, NULL, '未分组', '未分组的仓库', 1, NOW(), NOW() FROM `project`;

-- ── 3. code_repository 表新增 group_id 列（先可空回填，后改为 NOT NULL） ──
ALTER TABLE `code_repository`
  ADD COLUMN `group_id` bigint DEFAULT NULL COMMENT '所属分组 ID' AFTER `project_id`;

-- 存量仓库回填：归属各项目的「未分组」系统分组
UPDATE `code_repository` r
INNER JOIN `code_repository_group` g
  ON g.`project_id` = r.`project_id` AND g.`name` = '未分组' AND g.`is_system` = 1
SET r.`group_id` = g.`id`;

-- 回填完成后收紧为 NOT NULL（对齐接口文档 api.module_id 模式）
ALTER TABLE `code_repository`
  MODIFY COLUMN `group_id` bigint NOT NULL COMMENT '所属分组 ID',
  ADD KEY `idx_code_repository_group_id` (`group_id`),
  ADD CONSTRAINT `fk_code_repository_group_id` FOREIGN KEY (`group_id`) REFERENCES `code_repository_group` (`id`) ON DELETE CASCADE;

-- ── 4. sys_menu：【源代码】菜单（id=82）下新增【分组管理】按钮权限 ──
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(122, 82, '分组管理', 3, NULL, NULL, NULL, 'project:repo:group', 6, 1, NOW(), NOW());

-- ── 5. permission：与 sys_menu 同步（挂 parent=91 即源代码页面权限） ──
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`) VALUES
(130, '分组管理', 'project:repo:group', 'BUTTON', 91, NULL, 6, 1, '源代码分组管理按钮', 'display', NOW(), NOW());

-- ── 6. role_permission：ADMIN 分配分组管理权限 ──
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`) VALUES
(1, 130, 'enabled', NOW());
