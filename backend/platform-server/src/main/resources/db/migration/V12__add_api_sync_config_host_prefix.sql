-- Swagger 同步配置增加导入附加默认 host 前缀
ALTER TABLE `api_sync_config`
  ADD COLUMN `host_prefix` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '导入附加默认 host 前缀（如 ${host}，支持环境变量占位符）';
