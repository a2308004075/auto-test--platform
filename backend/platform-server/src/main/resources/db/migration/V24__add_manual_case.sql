-- =====================================================================
-- V24: 新增手动用例模块 + 优先级改造（P0/P1/P2/P3 �?�?�?低）
-- =====================================================================

-- ── 1. 自动用例优先级数据迁�?──
UPDATE `test_case` SET `priority` = '�? WHERE `priority` IN ('P0', 'P1');
UPDATE `test_case` SET `priority` = '�? WHERE `priority` = 'P2';
UPDATE `test_case` SET `priority` = '�? WHERE `priority` = 'P3';

-- �?priority 列从 ENUM 改为 VARCHAR，支持新的优先级�?
ALTER TABLE `test_case` MODIFY COLUMN `priority` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '�? COMMENT '优先级：�?�?�?;

-- ── 2. 创建手动用例分组�?──
CREATE TABLE `manual_case_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '所属项�?ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分�?ID（null=根分组）',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分组描述',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统默认分组�?-否，1-是）',
  `created_by` bigint DEFAULT NULL COMMENT '创建�?ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_manual_case_group_project_id` (`project_id`),
  KEY `idx_manual_case_group_parent_id` (`parent_id`),
  CONSTRAINT `fk_manual_case_group_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手动用例分组�?;

-- ── 3. 创建手动用例�?──
CREATE TABLE `manual_case` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '所属项�?ID',
  `group_id` bigint DEFAULT NULL COMMENT '所属分�?ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用例标题',
  `preconditions` text COLLATE utf8mb4_unicode_ci COMMENT '前置条件',
  `operation_steps` text COLLATE utf8mb4_unicode_ci COMMENT '操作步骤',
  `expected_result` text COLLATE utf8mb4_unicode_ci COMMENT '预期结果',
  `case_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT '用例类型：NORMAL-正常，EXCEPTION-异常',
  `priority` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '�? COMMENT '优先级：�?�?�?,
  `run_in_test_env` tinyint NOT NULL DEFAULT '1' COMMENT '测试环境是否执行�?-是，0-否）',
  `run_in_prod_env` tinyint NOT NULL DEFAULT '0' COMMENT '生产环境是否执行�?-是，0-否）',
  `case_status` tinyint NOT NULL DEFAULT '1' COMMENT '用例状态（1-使用�?-废弃�?,
  `created_by` bigint DEFAULT NULL COMMENT '创建�?ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_manual_case_project_id` (`project_id`),
  KEY `idx_manual_case_group_id` (`group_id`),
  KEY `idx_manual_case_priority` (`priority`),
  KEY `idx_manual_case_case_status` (`case_status`),
  KEY `idx_manual_case_created_by` (`created_by`),
  CONSTRAINT `fk_manual_case_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_manual_case_group_id` FOREIGN KEY (`group_id`) REFERENCES `manual_case_group` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手动用例�?;

-- ── 4. 更新字典：优先级 P0/P1/P2/P3 �?�?�?�?──
DELETE FROM `sys_dict` WHERE `dict_type` = 'priority';
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('priority', '优先�?, '�?, '�?, 1, '高优先级', 1, NOW(), NOW()),
('priority', '优先�?, '�?, '�?, 2, '中优先级', 1, NOW(), NOW()),
('priority', '优先�?, '�?, '�?, 3, '低优先级', 1, NOW(), NOW());

-- ── 5. 新增菜单 ──
-- 手动用例页面菜单（放在自动用例前面，sort_no=1�?
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`)
VALUES (101, 78, '手动用例', 2, '', '/project/:id/manual-cases', 'manualcase/ManualCaseList', 'project:manual-cases', 1, 1, NOW(), NOW());

-- 手动用例按钮权限菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`)
VALUES
(102, 101, '新建用例', 3, NULL, NULL, NULL, 'project:manual-case:add', 0, 1, NOW(), NOW()),
(103, 101, '编辑用例', 3, NULL, NULL, NULL, 'project:manual-case:edit', 0, 1, NOW(), NOW()),
(104, 101, '删除用例', 3, NULL, NULL, NULL, 'project:manual-case:delete', 0, 1, NOW(), NOW()),
(105, 101, '启停用例', 3, NULL, NULL, NULL, 'project:manual-case:toggle', 0, 1, NOW(), NOW()),
(106, 101, '分组管理', 3, NULL, NULL, NULL, 'project:manual-case:group', 0, 1, NOW(), NOW());

-- ── 6. 新增权限 ──
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`)
VALUES
(110, '手动用例', 'project:manual-cases', 'MENU', 17, NULL, 11, 1, '手动用例页面', 'display', NOW(), NOW()),
(111, '新建用例', 'project:manual-case:add', 'BUTTON', 110, NULL, 1, 1, '新建手动用例按钮', 'display', NOW(), NOW()),
(112, '编辑用例', 'project:manual-case:edit', 'BUTTON', 110, NULL, 2, 1, '编辑手动用例按钮', 'display', NOW(), NOW()),
(113, '删除用例', 'project:manual-case:delete', 'BUTTON', 110, NULL, 3, 1, '删除手动用例按钮', 'display', NOW(), NOW()),
(114, '启停用例', 'project:manual-case:toggle', 'BUTTON', 110, NULL, 4, 1, '启停手动用例按钮', 'display', NOW(), NOW()),
(115, '分组管理', 'project:manual-case:group', 'BUTTON', 110, NULL, 5, 1, '手动用例分组管理按钮', 'display', NOW(), NOW());

-- ── 7. 分配角色权限 ──
-- ADMIN 角色（role_id=1）：菜单 + 按钮
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`)
VALUES
(1, 110, NULL, NOW()),
(1, 111, 'enabled', NOW()),
(1, 112, 'enabled', NOW()),
(1, 113, 'enabled', NOW()),
(1, 114, 'enabled', NOW()),
(1, 115, 'enabled', NOW());

-- TESTER 角色（role_id=2）：仅菜�?
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`)
VALUES
(2, 110, NULL, NOW());

-- ── 8. 调整测试管理子菜单排序（手动用例 sort_no=1，其余顺延）──
UPDATE `sys_menu` SET `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 18;
UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 17;
UPDATE `sys_menu` SET `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 19;
UPDATE `sys_menu` SET `sort_no` = 5, `updated_at` = NOW() WHERE `id` = 20;
