-- V3: 初始全局配置数据
-- admin 用户由 DataInitializer 在应用启动时创建（需要 BCrypt 编码）

INSERT INTO global_settings (id, config_key, config_value, description) VALUES
('00000000-0000-0000-0000-000000000010', 'login.max_attempts', '5', '最大登录失败尝试次数'),
('00000000-0000-0000-0000-000000000011', 'login.lockout_minutes', '30', '账户锁定时长（分钟）'),
('00000000-0000-0000-0000-000000000012', 'session.timeout_minutes', '120', '会话超时时间（分钟）'),
('00000000-0000-0000-0000-000000000013', 'password.min_length', '6', '密码最小长度');
