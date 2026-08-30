-- =====================================================================
-- V28: 新增通用任务模块
-- 包含：task 表、字典数据、权限配置
-- =====================================================================

-- ── 1. 创建通用任务表 ──
CREATE TABLE `task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `task_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务类型：REQUIREMENT_REVIEW-需求评审,CASE_REVIEW-用例评审,REQUIREMENT_MODIFY-需求修改,CASE_MODIFY-用例修改,CASE_EXECUTION-用例执行,DEFECT_HANDLING-缺陷处理',
  `title` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务标题',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '任务描述',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理,IN_PROGRESS-进行中,COMPLETED-已完成,CANCELLED-已取消',
  `priority` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '中' COMMENT '优先级：高/中/低',
  `assignee_id` bigint DEFAULT NULL COMMENT '负责人 ID',
  `biz_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务类型',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务 ID',
  `due_date` date DEFAULT NULL COMMENT '截止日期',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_project_id` (`project_id`),
  KEY `idx_task_assignee_id` (`assignee_id`),
  KEY `idx_task_status` (`status`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_task_assignee_status` (`assignee_id`, `status`),
  CONSTRAINT `fk_task_assignee_id` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_task_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用任务表';

-- ── 2. 新增字典：任务类型 ──
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('task_type', '任务类型', 'REQUIREMENT_REVIEW', '需求评审', 1, '需求评审任务', 1, NOW(), NOW()),
('task_type', '任务类型', 'CASE_REVIEW', '用例评审', 2, '用例评审任务', 1, NOW(), NOW()),
('task_type', '任务类型', 'REQUIREMENT_MODIFY', '需求修改', 3, '需求修改任务', 1, NOW(), NOW()),
('task_type', '任务类型', 'CASE_MODIFY', '用例修改', 4, '用例修改任务', 1, NOW(), NOW()),
('task_type', '任务类型', 'CASE_EXECUTION', '用例执行', 5, '用例执行任务', 1, NOW(), NOW()),
('task_type', '任务类型', 'DEFECT_HANDLING', '缺陷处理', 6, '缺陷处理任务', 1, NOW(), NOW());

-- ── 3. 新增字典：任务状态 ──
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('task_status', '任务状态', 'PENDING', '待处理', 1, '待处理', 1, NOW(), NOW()),
('task_status', '任务状态', 'IN_PROGRESS', '进行中', 2, '进行中', 1, NOW(), NOW()),
('task_status', '任务状态', 'COMPLETED', '已完成', 3, '已完成', 1, NOW(), NOW()),
('task_status', '任务状态', 'CANCELLED', '已取消', 4, '已取消', 1, NOW(), NOW());

-- ── 4. 新增字典：任务优先级 ──
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('task_priority', '任务优先级', '高', '高', 1, '高优先级', 1, NOW(), NOW()),
('task_priority', '任务优先级', '中', '中', 2, '中优先级', 1, NOW(), NOW()),
('task_priority', '任务优先级', '低', '低', 3, '低优先级', 1, NOW(), NOW());

-- ── 5. 新增权限 ──
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`)
VALUES (123, '我的任务', 'my-tasks', 'MENU', 1, NULL, 20, 1, '我的任务页面', 'display', NOW(), NOW());

-- ── 6. 分配角色权限 ──
-- ADMIN 角色（role_id=1）
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`)
VALUES (1, 123, NULL, NOW());

-- TESTER 角色（role_id=2）
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`)
VALUES (2, 123, NULL, NOW());
