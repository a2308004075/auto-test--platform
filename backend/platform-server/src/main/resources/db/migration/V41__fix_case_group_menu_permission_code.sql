-- ============================================================
-- V41 补充「测试用例分组管理」菜单项的 permission_code
--
-- 背景：V36 插入 sys_menu 时使用了旧的 perms 列，
-- 需补充 permission_code 列以与 V19 以后的权限模型对齐。
-- ============================================================

UPDATE `sys_menu`
   SET `permission_code` = 'project:case:group'
 WHERE `name` = '分组管理'
   AND `parent_id` = 18
   AND `menu_type` = 3;
