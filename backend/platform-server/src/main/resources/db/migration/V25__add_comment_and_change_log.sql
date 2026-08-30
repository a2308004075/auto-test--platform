-- =====================================================================
-- V25: 新增评论与变更记录功能（需求条目、手动用例）
-- =====================================================================

-- ── 1. 评论表 ──
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型：REQUIREMENT_ITEM-需求条目，MANUAL_CASE-手动用例',
  `biz_id` bigint NOT NULL COMMENT '业务对象 ID',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论 ID（null=一级评论）',
  `created_by` bigint DEFAULT NULL COMMENT '评论人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_comment_biz` (`biz_type`, `biz_id`),
  KEY `idx_comment_biz_created_at` (`biz_type`, `biz_id`, `created_at`),
  KEY `idx_comment_parent_id` (`parent_id`),
  CONSTRAINT `fk_comment_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ── 2. 变更记录表 ──
CREATE TABLE `change_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型：REQUIREMENT_ITEM-需求条目，MANUAL_CASE-手动用例',
  `biz_id` bigint NOT NULL COMMENT '业务对象 ID',
  `field_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变更字段名',
  `old_value` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变更前值',
  `new_value` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变更后值',
  `created_by` bigint DEFAULT NULL COMMENT '操作人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_change_log_biz` (`biz_type`, `biz_id`),
  KEY `idx_change_log_biz_field` (`biz_type`, `biz_id`, `field_name`, `created_at`),
  KEY `idx_change_log_created_at` (`created_at`),
  CONSTRAINT `fk_change_log_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='变更记录表';
