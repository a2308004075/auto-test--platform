-- 项目级全局变量表：不绑定环境，整个项目任何地方可引用
-- 环境变量优先级高于全局变量（同名时环境变量覆盖全局）
CREATE TABLE `project_variable` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `var_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变量名',
  `var_value` text COLLATE utf8mb4_unicode_ci COMMENT '变量值',
  `data_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'text' COMMENT '数据类型：text/number/json/script',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变量描述',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_var_project_id` (`project_id`),
  CONSTRAINT `fk_project_var_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目全局变量表';
