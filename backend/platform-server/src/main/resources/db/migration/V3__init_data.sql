-- V3: 初始全局配置数据
-- admin 用户由 DataInitializer 在应用启动时创建（需要 BCrypt 编码）

-- 初始化用户角色（id 由数据库自增分配）
INSERT INTO user_role (role_name, role_code, description, is_active) VALUES
('管理员', 'ADMIN', '系统管理员，拥有全部权限', TRUE),
('测试人员', 'TESTER', '测试人员，仅可查看和执行', TRUE);

-- 初始化全局配置（id 由数据库自增分配）
INSERT INTO global_settings (config_key, config_value, description) VALUES
('login.max_attempts', '5', '最大登录失败尝试次数'),
('login.lockout_minutes', '30', '账户锁定时长（分钟）'),
('session.timeout_minutes', '120', '会话超时时间（分钟）'),
('password.min_length', '6', '密码最小长度');
