-- ============================================================
-- V12 为 user 表添加 bio（个人简介）字段
-- 对齐原型 docs/ui/settings/profile.html 中的"个人简介" textarea
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE `user` ADD COLUMN `bio` TEXT DEFAULT NULL COMMENT '个人简介（选填）' AFTER `display_name`;
