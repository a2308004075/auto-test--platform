-- ============================================================
-- V5 修复 project 表 is_active 语义冲突
-- 问题：is_active 同时被 @TableLogic（软删除）和"启用/停用"复用
-- 方案：新增 deleted 列承担软删除，is_active 回归启用/停用语义
-- ============================================================

SET NAMES utf8mb4;

-- 新增 deleted 列用于软删除
ALTER TABLE `project`
  ADD COLUMN `deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '软删除标记（true=已删除）' AFTER `is_active`;

-- 将已停用的项目同步到 deleted 列（如果之前有通过 deleteProject 软删除的数据）
-- 注意：由于 @TableLogic 之前的行为是 is_active=false 表示已删除，
-- 我们需要区分"已停用"和"已删除"的项目。
-- 目前无法区分，因此将所有 is_active=false 的项目标记为已删除。
-- 后续如果需要"停用"功能，可以通过 toggleStatus 正确操作。
UPDATE `project` SET `deleted` = TRUE WHERE `is_active` = FALSE;

-- 将所有项目的 is_active 恢复为 true（启用状态）
-- 因为之前 is_active=false 的项目现在由 deleted=true 标记
UPDATE `project` SET `is_active` = TRUE WHERE `deleted` = FALSE;

-- 添加索引
ALTER TABLE `project`
  ADD KEY `idx_project_deleted` (`deleted`);
