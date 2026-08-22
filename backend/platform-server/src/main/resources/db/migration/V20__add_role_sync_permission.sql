-- ============================================================
-- V20 新增"同步权限"按钮权限
-- 在角色管理页面增加"同步"按钮，用于从 sys_menu 同步页面和按钮到 permission 表
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 新增 system:role:sync 按钮权限
-- ============================================================

INSERT INTO `permission` (`permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `description`, `control_mode`) VALUES
('同步权限', 'system:role:sync', 'BUTTON', 5, NULL, 7, '同步页面和按钮权限', 'display');

-- ============================================================
-- 2. 为 ADMIN 角色分配同步权限
-- ============================================================

INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `permission_code` = 'system:role:sync';
