-- =====================================================================
-- V33: 显示名同步——【自动测试套件】→【自动化套件】、【自动用例】→【自动化用例】
-- 仅更新中文显示名（菜单/权限/字典）与表/字段注释；
-- 表名、字段名、API 路径、权限编码等英文标识符保持不变，
-- 角色分配（role_permission 经 permission_id 关联）不受影响
-- =====================================================================

-- ── 1. 菜单显示名同步 ──
UPDATE `sys_menu` SET `name` = '自动化套件', `updated_at` = NOW() WHERE `id` = 17;
UPDATE `sys_menu` SET `name` = '自动化用例', `updated_at` = NOW() WHERE `id` = 18;
UPDATE `sys_menu` SET `name` = '新建自动化用例', `updated_at` = NOW() WHERE `id` = 32;
UPDATE `sys_menu` SET `name` = '新建自动化套件', `updated_at` = NOW() WHERE `id` = 36;

-- ── 2. 权限显示名/描述同步（'步骤配置'/'分组管理'等通用词保持） ──
UPDATE `permission` SET `permission_name` = '自动化套件', `description` = '自动化套件页面', `updated_at` = NOW() WHERE `id` = 24;
UPDATE `permission` SET `permission_name` = '自动化用例', `description` = '自动化用例页面', `updated_at` = NOW() WHERE `id` = 25;
UPDATE `permission` SET `permission_name` = '新建自动化套件', `description` = '新建自动化套件按钮', `updated_at` = NOW() WHERE `id` = 76;
UPDATE `permission` SET `permission_name` = '编辑自动化套件', `description` = '编辑自动化套件按钮', `updated_at` = NOW() WHERE `id` = 77;
UPDATE `permission` SET `permission_name` = '删除自动化套件', `description` = '删除自动化套件按钮', `updated_at` = NOW() WHERE `id` = 78;
UPDATE `permission` SET `permission_name` = '新建自动化用例', `description` = '新建自动化用例按钮', `updated_at` = NOW() WHERE `id` = 80;
UPDATE `permission` SET `permission_name` = '编辑自动化用例', `description` = '编辑自动化用例按钮', `updated_at` = NOW() WHERE `id` = 81;
UPDATE `permission` SET `permission_name` = '删除自动化用例', `description` = '删除自动化用例按钮', `updated_at` = NOW() WHERE `id` = 82;
UPDATE `permission` SET `permission_name` = '启停自动化用例', `description` = '禁用/启用自动化用例按钮', `updated_at` = NOW() WHERE `id` = 83;

-- ── 3. 字典显示名同步（dict_value 编码不变） ──
UPDATE `sys_dict` SET `dict_value_name` = '自动化用例关键字', `remark` = '自动化用例关键字', `updated_at` = NOW()
WHERE `dict_type` = 'keyword_type' AND `dict_value` = 'AUTO_CASE';

UPDATE `sys_dict` SET `dict_value_name` = '自动化用例', `remark` = '自动化用例', `updated_at` = NOW()
WHERE `dict_type` = 'case_type' AND `dict_value` = 'AUTO_CASE';

UPDATE `sys_dict` SET `dict_value_name` = '自动化用例', `remark` = '自动化用例', `updated_at` = NOW()
WHERE `dict_type` = 'defect_target_type' AND `dict_value` = 'AUTO_CASE';

-- ── 4. 表注释同步 ──
ALTER TABLE `auto_suite` COMMENT='自动化套件表';
ALTER TABLE `auto_suite_group` COMMENT='自动化套件分组表';
ALTER TABLE `auto_suite_case_lifecycle` COMMENT='自动化套件内自动化用例级生命周期表';
ALTER TABLE `auto_case` COMMENT='自动化用例源实体表';
ALTER TABLE `auto_case_group` COMMENT='自动化用例分组表';

-- ── 5. 字段注释同步（MODIFY 需完整列定义，与 V32 迁移后现状一致） ──
ALTER TABLE `auto_case`
  MODIFY COLUMN `auto_suite_id` bigint NOT NULL COMMENT '所属自动化套件 ID';

ALTER TABLE `auto_suite_case_lifecycle`
  MODIFY COLUMN `auto_suite_id` bigint NOT NULL COMMENT '所属自动化套件 ID',
  MODIFY COLUMN `auto_case_id` bigint NOT NULL COMMENT '所属自动化用例 ID';

ALTER TABLE `test_plan`
  MODIFY COLUMN `auto_suite_ids` json NOT NULL COMMENT '关联的自动化套件 ID 列表';

ALTER TABLE `test_result`
  MODIFY COLUMN `auto_case_id` bigint NOT NULL COMMENT '所属自动化用例 ID';

ALTER TABLE `requirement_case_relation`
  MODIFY COLUMN `case_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用例类型：MANUAL_CASE-手动用例，AUTO_CASE-自动化用例';
