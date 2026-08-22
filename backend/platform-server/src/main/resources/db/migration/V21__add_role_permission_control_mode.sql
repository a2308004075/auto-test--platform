-- ============================================================
-- V21 角色权限控制模式：从全局改为按角色
-- 1. role_permission 表新增 control_mode 列
-- 2. 为现有 BUTTON 权限关联设置默认值 'enabled'（显示可点击）
-- 3. 为现有 MENU 权限关联设置 NULL（不适用控制模式）
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. role_permission 新增 control_mode 字段
-- ============================================================

ALTER TABLE `role_permission`
    ADD COLUMN `control_mode` VARCHAR(20) DEFAULT NULL
    COMMENT '按钮控制模式（按角色）：enabled-显示可点击，disabled-显示禁点击。MENU类型为NULL'
    AFTER `permission_id`;

-- ============================================================
-- 2. 为现有 BUTTON 权限关联填充 'enabled'（显示可点击）
-- ============================================================

UPDATE `role_permission` rp
    INNER JOIN `permission` p ON rp.permission_id = p.id
SET rp.control_mode = 'enabled'
WHERE p.type = 'BUTTON';

-- ============================================================
-- 3. 为现有 MENU 权限关联填充 NULL（不适用）
-- ============================================================

UPDATE `role_permission` rp
    INNER JOIN `permission` p ON rp.permission_id = p.id
SET rp.control_mode = NULL
WHERE p.type = 'MENU';
