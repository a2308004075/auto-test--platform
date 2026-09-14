-- URL 同步配置表：保存用户配置的 OpenAPI/Swagger 文档同步地址
CREATE TABLE `api_sync_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'OpenAPI/Swagger 文档 URL（doc.html 或 JSON 端点）',
  `module_id` bigint NOT NULL COMMENT '目标分组 ID',
  `headers` text COLLATE utf8mb4_unicode_ci COMMENT '自定义请求头（多行 Key: Value 文本）',
  `last_sync_at` datetime DEFAULT NULL COMMENT '最后一次同步时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_api_sync_config_project_id` (`project_id`),
  CONSTRAINT `fk_api_sync_config_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='URL 同步配置表';
