-- ============================================================
-- V10 将"普通用户"角色重命名为"测试人员"
-- role_code: USER → TESTER
-- ============================================================

UPDATE `user_role`
SET `role_name` = '测试人员',
    `role_code` = 'TESTER',
    `description` = '测试人员，仅可查看和执行'
WHERE `role_code` = 'USER';
