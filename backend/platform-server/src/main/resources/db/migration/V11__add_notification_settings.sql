-- ============================================================
-- V11 补全全局配置项：保留策略 + 通知配置（SMTP/Webhook）
-- 对齐 docs/ui/settings/global-config.html 原型设计
-- ============================================================

SET NAMES utf8mb4;

INSERT INTO `global_settings` (`config_key`, `config_value`, `description`) VALUES
('log.retention_days',           '30',    '日志保留天数'),
('report.retention_days',        '90',    '报告保留天数'),
('notification.smtp.host',       '""',    'SMTP 服务器地址'),
('notification.smtp.port',       '587',   'SMTP 端口'),
('notification.smtp.username',   '""',    'SMTP 账号'),
('notification.smtp.password',   '""',    'SMTP 密码'),
('notification.smtp.encryption', '"tls"', 'SMTP 加密方式（tls/ssl/none）'),
('notification.webhook.url',     '""',    'Webhook 通知 URL'),
('notification.webhook.secret',  '""',    'Webhook 密钥')
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;
