-- V27: 修正 V24、V26 与历史迁移的菜单/权限 ID 冲突
--
-- 背景：
--   V16 已使用 sys_menu 82-87 与 permission 91-96；
--   V17 已使用 sys_menu 88-89 与 permission 97-99；
--   V24（手动用例）重复使用了上述 ID；
--   V26（缺陷管理）重复使用了 88-93 / 97-102 区间。
--
-- 本迁移将 V24、V26 的数据迁移到新的独立 ID 区间，
-- 并保持父子关系、角色授权、菜单排序一致。

-- 关闭外键检查，允许跨表修改主键 ID
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 清理可能已存在的新 ID 重复记录（保证后续 UPDATE 主键不冲突）
-- ============================================================
DELETE FROM `role_permission` WHERE `permission_id` IN (110, 111, 112, 113, 114, 115, 117, 118, 119, 120, 121, 122);
DELETE FROM `permission` WHERE `id` IN (110, 111, 112, 113, 114, 115, 117, 118, 119, 120, 121, 122);
DELETE FROM `sys_menu` WHERE `id` IN (101, 102, 103, 104, 105, 106, 108, 109, 110, 111, 112, 113);

-- ============================================================
-- 2. 修正手动用例模块（V24）：82-87 / 91-96 -> 101-106 / 110-115
-- ============================================================

-- 2.1 sys_menu：迁移菜单 ID，并同步更新按钮的 parent_id
UPDATE `sys_menu` SET `id` = 101 WHERE `id` = 82;
UPDATE `sys_menu` SET `id` = 102, `parent_id` = 101 WHERE `id` = 83;
UPDATE `sys_menu` SET `id` = 103, `parent_id` = 101 WHERE `id` = 84;
UPDATE `sys_menu` SET `id` = 104, `parent_id` = 101 WHERE `id` = 85;
UPDATE `sys_menu` SET `id` = 105, `parent_id` = 101 WHERE `id` = 86;
UPDATE `sys_menu` SET `id` = 106, `parent_id` = 101 WHERE `id` = 87;

-- 2.2 permission：迁移权限 ID，并同步更新按钮权限的 parent_id
UPDATE `permission` SET `id` = 110 WHERE `id` = 91;
UPDATE `permission` SET `id` = 111, `parent_id` = 110 WHERE `id` = 92;
UPDATE `permission` SET `id` = 112, `parent_id` = 110 WHERE `id` = 93;
UPDATE `permission` SET `id` = 113, `parent_id` = 110 WHERE `id` = 94;
UPDATE `permission` SET `id` = 114, `parent_id` = 110 WHERE `id` = 95;
UPDATE `permission` SET `id` = 115, `parent_id` = 110 WHERE `id` = 96;

-- 2.3 role_permission：同步更新 permission_id
UPDATE `role_permission` SET `permission_id` = 110 WHERE `permission_id` = 91;
UPDATE `role_permission` SET `permission_id` = 111 WHERE `permission_id` = 92;
UPDATE `role_permission` SET `permission_id` = 112 WHERE `permission_id` = 93;
UPDATE `role_permission` SET `permission_id` = 113 WHERE `permission_id` = 94;
UPDATE `role_permission` SET `permission_id` = 114 WHERE `permission_id` = 95;
UPDATE `role_permission` SET `permission_id` = 115 WHERE `permission_id` = 96;

-- ============================================================
-- 3. 修正缺陷管理模块（V26）：88-93 / 97-102 -> 108-113 / 117-122
-- ============================================================

-- 3.1 sys_menu：迁移菜单 ID，并同步更新按钮的 parent_id
UPDATE `sys_menu` SET `id` = 108 WHERE `id` = 88;
UPDATE `sys_menu` SET `id` = 109, `parent_id` = 108 WHERE `id` = 89;
UPDATE `sys_menu` SET `id` = 110, `parent_id` = 108 WHERE `id` = 90;
UPDATE `sys_menu` SET `id` = 111, `parent_id` = 108 WHERE `id` = 91;
UPDATE `sys_menu` SET `id` = 112, `parent_id` = 108 WHERE `id` = 92;
UPDATE `sys_menu` SET `id` = 113, `parent_id` = 108 WHERE `id` = 93;

-- 3.2 permission：迁移权限 ID，并同步更新按钮权限的 parent_id
UPDATE `permission` SET `id` = 117 WHERE `id` = 97;
UPDATE `permission` SET `id` = 118, `parent_id` = 117 WHERE `id` = 98;
UPDATE `permission` SET `id` = 119, `parent_id` = 117 WHERE `id` = 99;
UPDATE `permission` SET `id` = 120, `parent_id` = 117 WHERE `id` = 100;
UPDATE `permission` SET `id` = 121, `parent_id` = 117 WHERE `id` = 101;
UPDATE `permission` SET `id` = 122, `parent_id` = 117 WHERE `id` = 102;

-- 3.3 role_permission：同步更新 permission_id
UPDATE `role_permission` SET `permission_id` = 117 WHERE `permission_id` = 97;
UPDATE `role_permission` SET `permission_id` = 118 WHERE `permission_id` = 98;
UPDATE `role_permission` SET `permission_id` = 119 WHERE `permission_id` = 99;
UPDATE `role_permission` SET `permission_id` = 120 WHERE `permission_id` = 100;
UPDATE `role_permission` SET `permission_id` = 121 WHERE `permission_id` = 101;
UPDATE `role_permission` SET `permission_id` = 122 WHERE `permission_id` = 102;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 4. 修正测试管理子菜单排序（V26 原引用 V24 旧 ID 82，现指向 101）
-- ============================================================
UPDATE `sys_menu` SET `sort_no` = 1, `updated_at` = NOW() WHERE `id` = 101;
UPDATE `sys_menu` SET `sort_no` = 2, `updated_at` = NOW() WHERE `id` = 18;
UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 17;
UPDATE `sys_menu` SET `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 19;
UPDATE `sys_menu` SET `sort_no` = 5, `updated_at` = NOW() WHERE `id` = 108;
UPDATE `sys_menu` SET `sort_no` = 6, `updated_at` = NOW() WHERE `id` = 20;
