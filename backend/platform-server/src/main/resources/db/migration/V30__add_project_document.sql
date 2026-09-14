-- V30: 新增项目文档模块
-- 包含：项目文档分组表、项目文档表、菜单与按钮权限
-- =====================================================================

-- ── 1. 创建项目文档分组表 ──
CREATE TABLE `project_doc_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分组 ID（null=根分组）',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分组描述',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统默认分组（0-否，1-是）',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_doc_group_project_id` (`project_id`),
  KEY `idx_project_doc_group_parent_id` (`parent_id`),
  CONSTRAINT `fk_project_doc_group_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_doc_group_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目文档分组表';

-- ── 2. 创建项目文档表 ──
-- 文件本体存于本地磁盘（doc.storage-path），DB 仅存元数据
CREATE TABLE `project_doc` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `group_id` bigint DEFAULT NULL COMMENT '所属分组 ID（null=未分组）',
  `doc_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档显示名',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `stored_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '磁盘存储文件名（UUID+扩展名）',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小（字节）',
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件 MIME 类型',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文档描述',
  `created_by` bigint DEFAULT NULL COMMENT '上传人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_doc_project_id` (`project_id`),
  KEY `idx_project_doc_group_id` (`group_id`),
  CONSTRAINT `fk_project_doc_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_doc_group_id` FOREIGN KEY (`group_id`) REFERENCES `project_doc_group` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_project_doc_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目文档表';

-- ── 3. sys_menu：【项目资料】目录下新增【项目文档】菜单（置于需求文档之后）及按钮权限 ──
-- 现有菜单 sort_no 调整：源代码 2→3，接口文档 3→4，界面元素 4→5
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(114, 90, '项目文档', 2, '', '/project/:id/docs', 'projectdoc/ProjectDocList', 'project:docs', 2, 1, NOW(), NOW()),
(115, 114, '上传文档', 3, NULL, NULL, NULL, 'project:doc:upload', 1, 1, NOW(), NOW()),
(116, 114, '编辑文档', 3, NULL, NULL, NULL, 'project:doc:edit', 2, 1, NOW(), NOW()),
(117, 114, '删除文档', 3, NULL, NULL, NULL, 'project:doc:delete', 3, 1, NOW(), NOW()),
(118, 114, '分组管理', 3, NULL, NULL, NULL, 'project:doc:group', 4, 1, NOW(), NOW());

UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 82;
UPDATE `sys_menu` SET `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 12;
UPDATE `sys_menu` SET `sort_no` = 5, `updated_at` = NOW() WHERE `id` = 91;

-- ── 4. permission：与 sys_menu 同步（沿用 V1 初始化模式，项目级页面挂 parent=17） ──
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`) VALUES
(124, '项目文档', 'project:docs', 'MENU', 17, NULL, 13, 1, '项目文档页面', 'display', NOW(), NOW()),
(125, '上传文档', 'project:doc:upload', 'BUTTON', 124, NULL, 1, 1, '上传文档按钮', 'display', NOW(), NOW()),
(126, '编辑文档', 'project:doc:edit', 'BUTTON', 124, NULL, 2, 1, '编辑文档按钮', 'display', NOW(), NOW()),
(127, '删除文档', 'project:doc:delete', 'BUTTON', 124, NULL, 3, 1, '删除文档按钮', 'display', NOW(), NOW()),
(128, '分组管理', 'project:doc:group', 'BUTTON', 124, NULL, 4, 1, '项目文档分组管理按钮', 'display', NOW(), NOW());

-- ── 5. role_permission：ADMIN 分配全部权限，TESTER 仅菜单可见（与 V21 需求文档一致） ──
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`) VALUES
(1, 124, NULL, NOW()),
(1, 125, 'enabled', NOW()),
(1, 126, 'enabled', NOW()),
(1, 127, 'enabled', NOW()),
(1, 128, 'enabled', NOW()),
(2, 124, NULL, NOW());
