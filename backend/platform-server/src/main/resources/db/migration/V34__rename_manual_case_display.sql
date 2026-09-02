-- =====================================================================
-- V34: 显示名同步——【手动用例】→【手动化用例】
-- 仅更新中文显示名（菜单/权限/字典）与表注释；
-- 表名、字段名、API 路径、权限编码等英文标识符保持不变，
-- 角色分配（role_permission 经 permission_id 关联）不受影响
-- =====================================================================

-- ── 1. 菜单显示名同步 ──
UPDATE `sys_menu` SET `name` = '手动化用例', `updated_at` = NOW() WHERE `id` = 101;

-- ── 2. 权限显示名/描述同步 ──
UPDATE `permission` SET `permission_name` = '手动化用例', `description` = '手动化用例页面', `updated_at` = NOW() WHERE `id` = 110;
UPDATE `permission` SET `description` = '新建手动化用例按钮', `updated_at` = NOW() WHERE `id` = 111;
UPDATE `permission` SET `description` = '编辑手动化用例按钮', `updated_at` = NOW() WHERE `id` = 112;
UPDATE `permission` SET `description` = '删除手动化用例按钮', `updated_at` = NOW() WHERE `id` = 113;
UPDATE `permission` SET `description` = '启停手动化用例按钮', `updated_at` = NOW() WHERE `id` = 114;
UPDATE `permission` SET `description` = '手动化用例分组管理按钮', `updated_at` = NOW() WHERE `id` = 115;

-- ── 3. 字典显示名同步（dict_value 编码不变） ──
UPDATE `sys_dict` SET `dict_value_name` = '手动化用例', `remark` = '手动化用例', `updated_at` = NOW()
WHERE `dict_type` = 'case_type' AND `dict_value` = 'MANUAL_CASE';

UPDATE `sys_dict` SET `dict_value_name` = '手动化用例', `remark` = '手动化用例', `updated_at` = NOW()
WHERE `dict_type` = 'defect_target_type' AND `dict_value` = 'MANUAL_CASE';

-- ── 4. 表注释同步 ──
ALTER TABLE `manual_case` COMMENT='手动化用例表';
ALTER TABLE `manual_case_group` COMMENT='手动化用例分组表';

-- ── 5. 字段注释同步（涉及"手动用例"的列） ──
ALTER TABLE `change_log`
  MODIFY COLUMN `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型：REQUIREMENT_ITEM-需求条目，MANUAL_CASE-手动化用例';

ALTER TABLE `comment`
  MODIFY COLUMN `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型：REQUIREMENT_ITEM-需求条目，MANUAL_CASE-手动化用例';

ALTER TABLE `requirement_case_relation`
  MODIFY COLUMN `case_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用例类型：MANUAL_CASE-手动化用例，AUTO_CASE-自动化用例';
