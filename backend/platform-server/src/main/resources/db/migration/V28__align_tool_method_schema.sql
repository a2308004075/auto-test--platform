-- ============================================================
-- V28 tool_method 表结构对齐 ToolMethod.java 实体类
--
-- 问题根因：
--   1. ToolMethod 实体类包含 paramDefinitions 字段，但数据库列名为 parameters，
--      MyBatis-Plus 自动映射为 param_definitions，导致 SQL 报 Unknown column。
--   2. 实体类新增 testInput/testResult/updatedBy 字段，数据库缺少对应列。
--   3. tool_method.keyword 列为 NOT NULL，但实体类不再使用该字段，会导致 INSERT 失败。
--
-- 注意：user_role.sort_order 已由 V14 迁移添加，此处不再重复。
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- tool_method 表对齐 ToolMethod.java 实体类
-- ============================================================

-- 1. 重命名 parameters → param_definitions（实体类字段为 paramDefinitions）
ALTER TABLE `tool_method`
  CHANGE COLUMN `parameters` `param_definitions` JSON DEFAULT NULL COMMENT '参数定义（JSON 数组）';

-- 2. 添加实体类中新增的列
ALTER TABLE `tool_method`
  ADD COLUMN `test_input` TEXT DEFAULT NULL COMMENT '测试输入（JSON）' AFTER `is_active`,
  ADD COLUMN `test_result` TEXT DEFAULT NULL COMMENT '测试结果（JSON）' AFTER `test_input`,
  ADD COLUMN `updated_by` BIGINT DEFAULT NULL COMMENT '更新人 ID' AFTER `created_by`;

-- 3. 将 keyword 列改为可空（实体类不再使用此字段，NOT NULL 会导致 INSERT 失败）
ALTER TABLE `tool_method`
  MODIFY COLUMN `keyword` VARCHAR(20) DEFAULT NULL COMMENT '关键字标识（已废弃，保留兼容）';

-- 4. 添加 updated_by 索引和外键（与 keyword 表一致）
ALTER TABLE `tool_method`
  ADD KEY `idx_tool_method_updated_by` (`updated_by`),
  ADD CONSTRAINT `fk_tool_method_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`id`) ON DELETE SET NULL;
