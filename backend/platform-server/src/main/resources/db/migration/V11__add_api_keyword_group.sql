CREATE TABLE `api_keyword_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_keyword_group_project_name` (`project_id`,`name`),
  KEY `idx_api_keyword_group_project_id` (`project_id`),
  KEY `fk_api_keyword_group_created_by` (`created_by`),
  CONSTRAINT `fk_api_keyword_group_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_api_keyword_group_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口关键字分组表';

ALTER TABLE `api_keyword`
  ADD COLUMN `group_id` bigint DEFAULT NULL COMMENT '接口关键字分组 ID' AFTER `api_id`,
  ADD KEY `idx_api_keyword_group_id` (`group_id`),
  ADD CONSTRAINT `fk_api_keyword_group_id` FOREIGN KEY (`group_id`) REFERENCES `api_keyword_group` (`id`) ON DELETE SET NULL;
