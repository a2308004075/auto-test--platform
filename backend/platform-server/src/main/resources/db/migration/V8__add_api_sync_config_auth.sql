-- Swagger 同步配置增加认证账号密码
ALTER TABLE `api_sync_config`
  ADD COLUMN `auth_username` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认证账号（Basic Auth）',
  ADD COLUMN `auth_password` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认证密码（Basic Auth）';
