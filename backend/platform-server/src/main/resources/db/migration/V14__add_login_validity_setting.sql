-- 新增全局配置项：用户登录有效时长（天），对所有用户统一生效，默认 5 天
INSERT INTO `global_settings` (`config_key`, `description`, `config_value`)
VALUES ('session.login_validity_days', '用户登录有效时长（天）', '5');
