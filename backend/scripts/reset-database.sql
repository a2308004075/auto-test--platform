-- ============================================================
-- 开发环境数据库重置脚本
-- 用于修复 Flyway 迁移历史与物理表状态不一致导致的启动失败
-- 例如：flyway_schema_history 被清空/删除，但业务表仍存在
-- ============================================================

-- 删除已存在的数据库（开发环境安全操作）
DROP DATABASE IF EXISTS `auto_test_platform`;

-- 重新创建数据库
CREATE DATABASE `auto_test_platform`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
