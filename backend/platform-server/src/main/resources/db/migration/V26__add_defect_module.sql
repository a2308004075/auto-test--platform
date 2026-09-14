-- =====================================================================
-- V26: 新增缺陷管理模块
-- 包含：缺陷分组、缺陷主表、工时、层级、关联、附件、变更记录、字典、菜单权限
-- =====================================================================

-- 1. 创建缺陷分组表
CREATE TABLE `defect_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分组 ID（null=根分组）',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分组描述',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统默认分组：0-否，1-是',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_defect_group_project_id` (`project_id`),
  KEY `idx_defect_group_parent_id` (`parent_id`),
  CONSTRAINT `fk_defect_group_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缺陷分组表';

-- 2. 创建缺陷主表
CREATE TABLE `defect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `group_id` bigint DEFAULT NULL COMMENT '所属分组 ID',
  `defect_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '缺陷编号',
  `title` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '缺陷标题',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT '内容（富文本 HTML）',
  `assignee_id` bigint DEFAULT NULL COMMENT '负责人 ID',
  `due_date` date DEFAULT NULL COMMENT '计划完成时间',
  `found_version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发现的版本',
  `module_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属模块',
  `severity` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '严重级别',
  `source` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缺陷根源',
  `environment_id` bigint DEFAULT NULL COMMENT '环境 ID',
  `reason_description` text COLLATE utf8mb4_unicode_ci COMMENT '原因描述',
  `responsible_id` bigint DEFAULT NULL COMMENT '责任人 ID',
  `reopen_count` int NOT NULL DEFAULT '0' COMMENT '重新打开次数',
  `fixed_version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改的版本',
  `plan_test_date` date DEFAULT NULL COMMENT '计划提测时间',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NEW' COMMENT '状态：NEW-新建,PENDING-待验证,COMPLETED-已完成,REOPENED-重新打开,CLOSED-已关闭',
  `parent_id` bigint DEFAULT NULL COMMENT '父缺陷 ID（层级关系）',
  `estimated_hours` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总估算工时',
  `actual_hours` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总实际工时',
  `remaining_hours` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总剩余工时',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_defect_project_no` (`project_id`,`defect_no`),
  KEY `idx_defect_group_id` (`group_id`),
  KEY `idx_defect_status` (`status`),
  KEY `idx_defect_assignee_id` (`assignee_id`),
  KEY `idx_defect_responsible_id` (`responsible_id`),
  KEY `idx_defect_parent_id` (`parent_id`),
  KEY `idx_defect_environment_id` (`environment_id`),
  KEY `idx_defect_created_by` (`created_by`),
  CONSTRAINT `fk_defect_group_id` FOREIGN KEY (`group_id`) REFERENCES `defect_group` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_defect_assignee_id` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_defect_responsible_id` FOREIGN KEY (`responsible_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_defect_environment_id` FOREIGN KEY (`environment_id`) REFERENCES `environment` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_defect_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `defect` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_defect_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_defect_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缺陷表';

-- 3. 创建缺陷工时记录表
CREATE TABLE `defect_work_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `defect_id` bigint NOT NULL COMMENT '缺陷 ID',
  `user_id` bigint DEFAULT NULL COMMENT '记录人 ID',
  `log_date` date DEFAULT NULL COMMENT '工作日期',
  `hours` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '工时（小时）',
  `work_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工时类型：ESTIMATE/ACTUAL/REMAINING',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_defect_work_log_defect_id` (`defect_id`),
  KEY `idx_defect_work_log_user_id` (`user_id`),
  CONSTRAINT `fk_defect_work_log_defect_id` FOREIGN KEY (`defect_id`) REFERENCES `defect` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_defect_work_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缺陷工时记录表';

-- 4. 创建缺陷关联表
CREATE TABLE `defect_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `defect_id` bigint NOT NULL COMMENT '缺陷 ID',
  `relation_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RELATED' COMMENT '关联类型：RELATED/BLOCK/DUPLICATE/CHILD',
  `target_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联目标类型：TEST_CASE/MANUAL_CASE/TEST_PLAN/TEST_EXECUTION',
  `target_id` bigint NOT NULL COMMENT '关联目标 ID',
  `target_title` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联目标标题快照',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_defect_relation_defect_id` (`defect_id`),
  KEY `idx_defect_relation_target` (`target_type`,`target_id`),
  CONSTRAINT `fk_defect_relation_defect_id` FOREIGN KEY (`defect_id`) REFERENCES `defect` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_defect_relation_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缺陷关联表';

-- 5. 创建缺陷附件表
CREATE TABLE `defect_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `defect_id` bigint NOT NULL COMMENT '缺陷 ID',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名称',
  `file_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件访问 URL',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `created_by` bigint DEFAULT NULL COMMENT '上传人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_defect_attachment_defect_id` (`defect_id`),
  CONSTRAINT `fk_defect_attachment_defect_id` FOREIGN KEY (`defect_id`) REFERENCES `defect` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_defect_attachment_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缺陷附件表';

-- 6. 创建缺陷变更记录表
CREATE TABLE `defect_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `defect_id` bigint NOT NULL COMMENT '缺陷 ID',
  `field_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变更字段',
  `old_value` text COLLATE utf8mb4_unicode_ci COMMENT '变更前值',
  `new_value` text COLLATE utf8mb4_unicode_ci COMMENT '变更后值',
  `changed_by` bigint DEFAULT NULL COMMENT '变更人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  PRIMARY KEY (`id`),
  KEY `idx_defect_history_defect_id` (`defect_id`),
  KEY `idx_defect_history_created_at` (`created_at`),
  CONSTRAINT `fk_defect_history_defect_id` FOREIGN KEY (`defect_id`) REFERENCES `defect` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_defect_history_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缺陷变更记录表';

-- 7. 新增字典
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('defect_status', '缺陷状态', 'NEW', '新建', 1, '新建缺陷', 1, NOW(), NOW()),
('defect_status', '缺陷状态', 'PENDING', '待验证', 2, '待验证', 1, NOW(), NOW()),
('defect_status', '缺陷状态', 'COMPLETED', '已完成', 3, '已完成', 1, NOW(), NOW()),
('defect_status', '缺陷状态', 'REOPENED', '重新打开', 4, '重新打开', 1, NOW(), NOW()),
('defect_status', '缺陷状态', 'CLOSED', '已关闭', 5, '已关闭', 1, NOW(), NOW()),
('defect_severity', '严重级别', '致命', '致命', 1, '致命缺陷', 1, NOW(), NOW()),
('defect_severity', '严重级别', '严重', '严重', 2, '严重缺陷', 1, NOW(), NOW()),
('defect_severity', '严重级别', '一般', '一般', 3, '一般缺陷', 1, NOW(), NOW()),
('defect_severity', '严重级别', '提示', '提示', 4, '提示缺陷', 1, NOW(), NOW()),
('defect_source', '缺陷根源', '开发修改引入', '开发修改引入', 1, '开发修改引入', 1, NOW(), NOW()),
('defect_source', '缺陷根源', '需求遗漏', '需求遗漏', 2, '需求遗漏', 1, NOW(), NOW()),
('defect_source', '缺陷根源', '环境问题', '环境问题', 3, '环境问题', 1, NOW(), NOW()),
('defect_source', '缺陷根源', '测试遗漏', '测试遗漏', 4, '测试遗漏', 1, NOW(), NOW()),
('defect_source', '缺陷根源', '其他', '其他', 5, '其他', 1, NOW(), NOW()),
('defect_relation_type', '缺陷关联类型', 'RELATED', '关联', 1, '一般关联', 1, NOW(), NOW()),
('defect_relation_type', '缺陷关联类型', 'BLOCK', '阻塞', 2, '阻塞', 1, NOW(), NOW()),
('defect_relation_type', '缺陷关联类型', 'DUPLICATE', '重复', 3, '重复', 1, NOW(), NOW()),
('defect_relation_type', '缺陷关联类型', 'CHILD', '子缺陷', 4, '子缺陷', 1, NOW(), NOW());

-- 8. 新增菜单
-- 缺陷管理页面菜单（放在测试计划与执行记录之间，sort_no=5）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`)
VALUES (108, 78, '缺陷管理', 2, '', '/project/:id/defects', 'defect/DefectList', 'project:defects', 5, 1, NOW(), NOW());

-- 缺陷管理按钮权限菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`)
VALUES
(109, 108, '新建缺陷', 3, NULL, NULL, NULL, 'project:defect:add', 0, 1, NOW(), NOW()),
(110, 108, '编辑缺陷', 3, NULL, NULL, NULL, 'project:defect:edit', 0, 1, NOW(), NOW()),
(111, 108, '删除缺陷', 3, NULL, NULL, NULL, 'project:defect:delete', 0, 1, NOW(), NOW()),
(112, 108, '状态流转', 3, NULL, NULL, NULL, 'project:defect:status', 0, 1, NOW(), NOW()),
(113, 108, '分组管理', 3, NULL, NULL, NULL, 'project:defect:group', 0, 1, NOW(), NOW());

-- 9. 新增权限
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`)
VALUES
(117, '缺陷管理', 'project:defects', 'MENU', 17, NULL, 12, 1, '缺陷管理页面', 'display', NOW(), NOW()),
(118, '新建缺陷', 'project:defect:add', 'BUTTON', 117, NULL, 1, 1, '新建缺陷按钮', 'display', NOW(), NOW()),
(119, '编辑缺陷', 'project:defect:edit', 'BUTTON', 117, NULL, 2, 1, '编辑缺陷按钮', 'display', NOW(), NOW()),
(120, '删除缺陷', 'project:defect:delete', 'BUTTON', 117, NULL, 3, 1, '删除缺陷按钮', 'display', NOW(), NOW()),
(121, '状态流转', 'project:defect:status', 'BUTTON', 117, NULL, 4, 1, '缺陷状态流转按钮', 'display', NOW(), NOW()),
(122, '分组管理', 'project:defect:group', 'BUTTON', 117, NULL, 5, 1, '缺陷分组管理按钮', 'display', NOW(), NOW());

-- 10. 分配角色权限
-- ADMIN 角色（role_id=1）：菜单 + 按钮
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`)
VALUES
(1, 117, NULL, NOW()),
(1, 118, 'enabled', NOW()),
(1, 119, 'enabled', NOW()),
(1, 120, 'enabled', NOW()),
(1, 121, 'enabled', NOW()),
(1, 122, 'enabled', NOW());

-- TESTER 角色（role_id=2）：菜单 + 按钮
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`)
VALUES
(2, 117, NULL, NOW()),
(2, 118, 'enabled', NOW()),
(2, 119, 'enabled', NOW()),
(2, 120, 'enabled', NOW()),
(2, 121, 'enabled', NOW()),
(2, 122, 'enabled', NOW());

-- 11. 调整测试管理子菜单排序
UPDATE `sys_menu` SET `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 101;
UPDATE `sys_menu` SET `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 18;
UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 17;
UPDATE `sys_menu` SET `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 19;
-- 缺陷管理 sort_no=5
UPDATE `sys_menu` SET `sort_no` = 6, `updated_at` = NOW() WHERE `id` = 20;
