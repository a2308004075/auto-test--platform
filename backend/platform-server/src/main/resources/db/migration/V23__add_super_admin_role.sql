-- ============================================================
-- V23: 引入超级管理员角色（SUPER_ADMIN）
-- admin 账号从 ADMIN 角色独立出来，使用 SUPER_ADMIN 角色，
-- 避免被 ADMIN 角色的权限配置干扰。
-- ============================================================

SET NAMES utf8mb4;

-- 1. 创建超级管理员角色（sort_order = -1 确保排在 ADMIN 之前）
INSERT INTO user_role (role_name, role_code, description, sort_order, is_active) VALUES
('超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有全部权限，不受 ADMIN 角色配置影响', -1, TRUE);

-- 2. 将 admin 用户的角色从 ADMIN 改为 SUPER_ADMIN
UPDATE `user` u
  JOIN user_role sa ON sa.role_code = 'SUPER_ADMIN'
  SET u.role_id = sa.id,
      u.display_name = '超级管理员'
  WHERE u.username = 'admin';

-- 3. 在 sys_dict 中同步创建 SUPER_ADMIN 字典条目
INSERT INTO sys_dict (dict_type, dict_type_name, dict_value, dict_value_name, sort_no, remark, is_active)
VALUES ('user_role', '用户角色', 'SUPER_ADMIN', '超级管理员', -1, '系统超级管理员，拥有全部权限', 1);
