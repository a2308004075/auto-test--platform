-- ============================================================
-- V7 将所有 BOOLEAN 列改为 TINYINT
-- MySQL 中 BOOLEAN 本身就是 TINYINT 的别名，
-- 此迁移统一列定义为 TINYINT，DEFAULT 改为 0/1
-- 0 = false/停用，1 = true/启用
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- M1 认证与用户管理
-- ============================================================

ALTER TABLE `user_role`
  MODIFY COLUMN `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用，1-启用）';

ALTER TABLE `user`
  MODIFY COLUMN `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用，1-启用）';

-- ============================================================
-- M2 项目管理 / M3 环境配置
-- ============================================================

ALTER TABLE `project`
  MODIFY COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记（0-未删除，1-已删除）';

ALTER TABLE `environment`
  MODIFY COLUMN `is_current` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为当前激活环境（0-否，1-是）',
  MODIFY COLUMN `is_active`   TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用，1-启用）';

-- ============================================================
-- M4 接口文档
-- ============================================================

ALTER TABLE `api_module`
  MODIFY COLUMN `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为系统默认分组（0-否，1-是）';

ALTER TABLE `api`
  MODIFY COLUMN `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用，1-启用）';

-- ============================================================
-- M5/M6/M7 关键字管理
-- ============================================================

ALTER TABLE `keyword`
  MODIFY COLUMN `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用，1-启用）';

ALTER TABLE `tool_method`
  MODIFY COLUMN `is_builtin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置方法（0-否，1-是）',
  MODIFY COLUMN `is_active`  TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用，1-启用）';

-- ============================================================
-- M8 测试用例管理
-- ============================================================

ALTER TABLE `test_suite`
  MODIFY COLUMN `enable_once_setup_teardown`     TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用套件级·整体生命周期（0-否，1-是）',
  MODIFY COLUMN `enable_per_case_setup_teardown`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用套件级·每条生命周期（0-否，1-是）';

ALTER TABLE `test_case`
  MODIFY COLUMN `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用，1-启用）';

-- ============================================================
-- M9 测试执行与调度
-- ============================================================

ALTER TABLE `test_plan`
  MODIFY COLUMN `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-停用，1-启用）';
