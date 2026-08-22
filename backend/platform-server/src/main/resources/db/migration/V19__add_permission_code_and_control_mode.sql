-- ============================================================
-- V19 权限控制增强：菜单关联权限 + 按钮控制模式
-- 1. sys_menu 表新增 permission_code 列（关联 permission.permission_code）
-- 2. permission 表新增 control_mode 列（按钮权限控制模式：display/click）
-- 3. 为现有菜单填充 permission_code 映射
-- 4. 为现有按钮权限填充 control_mode 默认值
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. sys_menu 新增 permission_code 字段
-- ============================================================

ALTER TABLE `sys_menu`
    ADD COLUMN `permission_code` VARCHAR(100) DEFAULT NULL COMMENT '关联权限编码（对应 permission.permission_code）' AFTER `component`;

-- ============================================================
-- 2. permission 新增 control_mode 字段
-- ============================================================

ALTER TABLE `permission`
    ADD COLUMN `control_mode` VARCHAR(20) DEFAULT 'display' COMMENT '按钮控制模式：display-无权限时隐藏按钮，click-无权限时禁用按钮' AFTER `description`;

-- ============================================================
-- 3. 为现有菜单填充 permission_code（与 permission 表映射）
-- ============================================================

-- 首页
UPDATE `sys_menu` SET `permission_code` = 'home' WHERE `id` = 1;

-- 系统管理子菜单
UPDATE `sys_menu` SET `permission_code` = 'system:profile' WHERE `id` = 3;
UPDATE `sys_menu` SET `permission_code` = 'system:user' WHERE `id` = 4;
UPDATE `sys_menu` SET `permission_code` = 'system:role' WHERE `id` = 5;
UPDATE `sys_menu` SET `permission_code` = 'system:config' WHERE `id` = 6;

-- 项目内页面
UPDATE `sys_menu` SET `permission_code` = 'project:dashboard' WHERE `id` = 11;
UPDATE `sys_menu` SET `permission_code` = 'project:apis' WHERE `id` = 12;
UPDATE `sys_menu` SET `permission_code` = 'project:environments' WHERE `id` = 13;
UPDATE `sys_menu` SET `permission_code` = 'project:keywords' WHERE `id` = 14;
UPDATE `sys_menu` SET `permission_code` = 'project:tools' WHERE `id` = 15;
UPDATE `sys_menu` SET `permission_code` = 'project:actions' WHERE `id` = 16;
UPDATE `sys_menu` SET `permission_code` = 'project:suites' WHERE `id` = 17;
UPDATE `sys_menu` SET `permission_code` = 'project:cases' WHERE `id` = 18;
UPDATE `sys_menu` SET `permission_code` = 'project:plans' WHERE `id` = 19;
UPDATE `sys_menu` SET `permission_code` = 'project:executions' WHERE `id` = 20;

-- 注：菜单管理(id=7)、字典管理(id=8)、缓存管理(id=9) 暂无对应 permission 记录，
-- permission_code 保持 NULL，即对所有已认证用户可见

-- ============================================================
-- 4. 为现有 BUTTON 权限填充 control_mode 默认值（display）
-- ============================================================

UPDATE `permission` SET `control_mode` = 'display' WHERE `type` = 'BUTTON';
