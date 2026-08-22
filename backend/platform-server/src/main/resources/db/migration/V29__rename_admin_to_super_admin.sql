-- ============================================================
-- V29: 将内置管理员账号从 admin 重命名为 superAdmin
-- 重命名后 admin 将不再是系统保留账号，可供普通用户注册使用。
-- ============================================================

SET NAMES utf8mb4;

-- 1. 将 admin 用户的账号重命名为 superAdmin
UPDATE `user` SET username = 'superAdmin' WHERE username = 'admin';
