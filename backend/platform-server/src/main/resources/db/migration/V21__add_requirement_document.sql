-- 需求文档模块：版本管理与需求条目管理
-- 1. 需求版本表：项目下的需求版本（每个版本包含多个需求条目）
CREATE TABLE `requirement_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `version_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版本号',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '版本描述',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PLANNING' COMMENT '状态：PLANNING-规划中，IN_PROGRESS-进行中，COMPLETED-已完成',
  `start_date` date DEFAULT NULL COMMENT '计划开始日期',
  `end_date` date DEFAULT NULL COMMENT '计划结束日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_requirement_version_project_id` (`project_id`),
  CONSTRAINT `fk_requirement_version_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求版本表';

-- 2. 需求条目表：版本下的需求条目
CREATE TABLE `requirement_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `version_id` bigint NOT NULL COMMENT '所属版本 ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '需求标题',
  `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '需求描述',
  `req_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FEATURE' COMMENT '需求类型：FEATURE-功能，IMPROVEMENT-优化，BUG-Bug',
  `priority` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级：HIGH-高，MEDIUM-中，LOW-低',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理，IN_PROGRESS-进行中，COMPLETED-已完成',
  `assignee` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人',
  `deadline` date DEFAULT NULL COMMENT '截止日期',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_requirement_item_version_id` (`version_id`),
  CONSTRAINT `fk_requirement_item_version_id` FOREIGN KEY (`version_id`) REFERENCES `requirement_version` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求条目表';

-- 3. sys_menu：【项目资料】目录下新增【需求文档】菜单（置于源代码之前）及按钮权限
-- 现有菜单 sort_no 调整：源代码 1→2，接口文档 2→3，界面元素 3→4
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(94, 90, '需求文档', 2, '', '/project/:id/requirements', 'requirement/RequirementList', 'project:requirements', 1, 1, NOW(), NOW()),
(95, 94, '新建版本', 3, NULL, NULL, NULL, 'project:req:version:create', 1, 1, NOW(), NOW()),
(96, 94, '删除版本', 3, NULL, NULL, NULL, 'project:req:version:delete', 2, 1, NOW(), NOW()),
(97, 94, '新建需求', 3, NULL, NULL, NULL, 'project:req:item:create', 3, 1, NOW(), NOW()),
(98, 94, '编辑需求', 3, NULL, NULL, NULL, 'project:req:item:edit', 4, 1, NOW(), NOW()),
(99, 94, '删除需求', 3, NULL, NULL, NULL, 'project:req:item:delete', 5, 1, NOW(), NOW()),
(100, 94, '编辑版本', 3, NULL, NULL, NULL, 'project:req:version:edit', 6, 1, NOW(), NOW());

UPDATE `sys_menu` SET `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 82;
UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 12;
UPDATE `sys_menu` SET `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 91;

-- 4. permission：与 sys_menu 同步（沿用 V1 初始化模式，项目级页面挂 parent=17）
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`) VALUES
(103, '需求文档', 'project:requirements', 'MENU', 17, NULL, 12, 1, '需求文档页面', 'display', NOW(), NOW()),
(104, '新建版本', 'project:req:version:create', 'BUTTON', 103, NULL, 1, 1, '新建版本按钮', 'display', NOW(), NOW()),
(105, '删除版本', 'project:req:version:delete', 'BUTTON', 103, NULL, 2, 1, '删除版本按钮', 'display', NOW(), NOW()),
(106, '编辑版本', 'project:req:version:edit', 'BUTTON', 103, NULL, 3, 1, '编辑版本按钮', 'display', NOW(), NOW()),
(107, '新建需求', 'project:req:item:create', 'BUTTON', 103, NULL, 4, 1, '新建需求按钮', 'display', NOW(), NOW()),
(108, '编辑需求', 'project:req:item:edit', 'BUTTON', 103, NULL, 5, 1, '编辑需求按钮', 'display', NOW(), NOW()),
(109, '删除需求', 'project:req:item:delete', 'BUTTON', 103, NULL, 6, 1, '删除需求按钮', 'display', NOW(), NOW());

-- 5. role_permission：ADMIN 分配全部权限，TESTER 仅菜单可见（与现有项目级菜单的分配模式一致）
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`) VALUES
(1, 103, NULL, NOW()),
(1, 104, 'enabled', NOW()),
(1, 105, 'enabled', NOW()),
(1, 106, 'enabled', NOW()),
(1, 107, 'enabled', NOW()),
(1, 108, 'enabled', NOW()),
(1, 109, 'enabled', NOW()),
(2, 103, NULL, NOW());
