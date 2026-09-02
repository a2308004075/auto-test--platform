-- =====================================================================
-- V32: 自动测试套件 / 自动用例全面重命名
-- 包含：表与字段重命名、外键与索引重建（auto_ 前缀）、枚举值与存量数据迁移、
--       字典/菜单/权限编码同步
-- =====================================================================

-- ── 1. 删除旧外键约束（统一重建为 auto_ 前缀） ──
ALTER TABLE `test_suite`
  DROP FOREIGN KEY `fk_test_suite_created_by`,
  DROP FOREIGN KEY `fk_test_suite_group_id`,
  DROP FOREIGN KEY `fk_test_suite_project_id`;

ALTER TABLE `test_case`
  DROP FOREIGN KEY `fk_test_case_created_by`,
  DROP FOREIGN KEY `fk_test_case_group_id`,
  DROP FOREIGN KEY `fk_test_case_suite_id`;

ALTER TABLE `suite_case_lifecycle`
  DROP FOREIGN KEY `fk_suite_case_lifecycle_case_id`,
  DROP FOREIGN KEY `fk_suite_case_lifecycle_suite_id`;

ALTER TABLE `suite_group`
  DROP FOREIGN KEY `fk_suite_group_parent_id`,
  DROP FOREIGN KEY `fk_suite_group_project_id`;

ALTER TABLE `case_group`
  DROP FOREIGN KEY `fk_case_group_created_by`,
  DROP FOREIGN KEY `fk_case_group_project_id`;

ALTER TABLE `test_result`
  DROP FOREIGN KEY `fk_test_result_case_id`;

-- ── 2. 字段重命名（同步注释） ──
ALTER TABLE `test_case`
  CHANGE COLUMN `suite_id` `auto_suite_id` bigint NOT NULL COMMENT '所属自动测试套件 ID';

ALTER TABLE `suite_case_lifecycle`
  CHANGE COLUMN `suite_id` `auto_suite_id` bigint NOT NULL COMMENT '所属自动测试套件 ID',
  CHANGE COLUMN `case_id` `auto_case_id` bigint NOT NULL COMMENT '所属自动用例 ID';

ALTER TABLE `test_plan`
  CHANGE COLUMN `suite_ids` `auto_suite_ids` json NOT NULL COMMENT '关联的自动测试套件 ID 列表';

ALTER TABLE `test_result`
  CHANGE COLUMN `case_id` `auto_case_id` bigint NOT NULL COMMENT '所属自动用例 ID';

-- ── 3. 表重命名 ──
RENAME TABLE
  `test_suite` TO `auto_suite`,
  `suite_group` TO `auto_suite_group`,
  `suite_case_lifecycle` TO `auto_suite_case_lifecycle`,
  `test_case` TO `auto_case`,
  `case_group` TO `auto_case_group`;

-- ── 4. 索引重命名 + 表注释同步 ──
ALTER TABLE `auto_suite`
  RENAME INDEX `uk_test_suite_project_name` TO `uk_auto_suite_project_name`,
  RENAME INDEX `idx_test_suite_project_id` TO `idx_auto_suite_project_id`,
  RENAME INDEX `idx_test_suite_priority` TO `idx_auto_suite_priority`,
  RENAME INDEX `idx_test_suite_created_by` TO `idx_auto_suite_created_by`,
  RENAME INDEX `idx_test_suite_group_id` TO `idx_auto_suite_group_id`,
  COMMENT='自动测试套件表';

ALTER TABLE `auto_suite_group`
  RENAME INDEX `idx_suite_group_project_id` TO `idx_auto_suite_group_project_id`,
  RENAME INDEX `idx_suite_group_parent_id` TO `idx_auto_suite_group_parent_id`,
  COMMENT='自动测试套件分组表';

ALTER TABLE `auto_suite_case_lifecycle`
  RENAME INDEX `uk_suite_case_lifecycle_suite_case` TO `uk_auto_suite_case_lifecycle_suite_case`,
  RENAME INDEX `idx_suite_case_lifecycle_case_id` TO `idx_auto_suite_case_lifecycle_case_id`,
  COMMENT='自动测试套件内自动用例级生命周期表';

ALTER TABLE `auto_case`
  RENAME INDEX `uk_test_case_suite_name` TO `uk_auto_case_suite_name`,
  RENAME INDEX `idx_test_case_suite_id` TO `idx_auto_case_suite_id`,
  RENAME INDEX `idx_test_case_priority` TO `idx_auto_case_priority`,
  RENAME INDEX `idx_test_case_is_active` TO `idx_auto_case_is_active`,
  RENAME INDEX `idx_test_case_created_by` TO `idx_auto_case_created_by`,
  RENAME INDEX `idx_test_case_group_id` TO `idx_auto_case_group_id`,
  COMMENT='自动用例源实体表';

ALTER TABLE `auto_case_group`
  RENAME INDEX `idx_case_group_project_id` TO `idx_auto_case_group_project_id`,
  RENAME INDEX `idx_case_group_parent_id` TO `idx_auto_case_group_parent_id`,
  RENAME INDEX `fk_case_group_created_by` TO `idx_auto_case_group_created_by`,
  COMMENT='自动用例分组表';

ALTER TABLE `test_result`
  RENAME INDEX `idx_test_result_case_id` TO `idx_test_result_auto_case_id`;

-- ── 5. 重建外键约束（auto_ 前缀） ──
ALTER TABLE `auto_suite`
  ADD CONSTRAINT `fk_auto_suite_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_auto_suite_group_id` FOREIGN KEY (`group_id`) REFERENCES `auto_suite_group` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_auto_suite_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL;

ALTER TABLE `auto_suite_group`
  ADD CONSTRAINT `fk_auto_suite_group_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_auto_suite_group_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `auto_suite_group` (`id`) ON DELETE SET NULL;

ALTER TABLE `auto_case`
  ADD CONSTRAINT `fk_auto_case_suite_id` FOREIGN KEY (`auto_suite_id`) REFERENCES `auto_suite` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_auto_case_group_id` FOREIGN KEY (`group_id`) REFERENCES `auto_case_group` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_auto_case_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL;

ALTER TABLE `auto_case_group`
  ADD CONSTRAINT `fk_auto_case_group_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_auto_case_group_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL;

ALTER TABLE `auto_suite_case_lifecycle`
  ADD CONSTRAINT `fk_auto_suite_case_lifecycle_suite_id` FOREIGN KEY (`auto_suite_id`) REFERENCES `auto_suite` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_auto_suite_case_lifecycle_case_id` FOREIGN KEY (`auto_case_id`) REFERENCES `auto_case` (`id`) ON DELETE CASCADE;

ALTER TABLE `test_result`
  ADD CONSTRAINT `fk_test_result_auto_case_id` FOREIGN KEY (`auto_case_id`) REFERENCES `auto_case` (`id`) ON DELETE CASCADE;

-- ── 6. keyword 关键字类型枚举值 TEST_CASE → AUTO_CASE ──
-- 先扩展枚举容纳新旧值，迁移存量后再收窄
ALTER TABLE `keyword`
  MODIFY COLUMN `keyword_type` enum('API','TOOL','ACTION','TEST_CASE','AUTO_CASE') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关键字类型';

UPDATE `keyword` SET `keyword_type` = 'AUTO_CASE' WHERE `keyword_type` = 'TEST_CASE';

ALTER TABLE `keyword`
  MODIFY COLUMN `keyword_type` enum('API','TOOL','ACTION','AUTO_CASE') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关键字类型',
  MODIFY COLUMN `ref_id` bigint DEFAULT NULL COMMENT '指向源实体 ID（ApiEndpoint / ToolMethod / Action / AutoCase）';

-- ── 7. 需求/缺陷关联存量值迁移（TEST_CASE → AUTO_CASE） ──
UPDATE `requirement_case_relation` SET `case_type` = 'AUTO_CASE' WHERE `case_type` = 'TEST_CASE';
UPDATE `defect_relation` SET `target_type` = 'AUTO_CASE' WHERE `target_type` = 'TEST_CASE';

ALTER TABLE `requirement_case_relation`
  MODIFY COLUMN `case_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用例类型：MANUAL_CASE-手动用例，AUTO_CASE-自动用例';

ALTER TABLE `defect_relation`
  MODIFY COLUMN `target_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联目标类型：AUTO_CASE/MANUAL_CASE/TEST_PLAN/TEST_EXECUTION';

-- ── 8. 字典同步（keyword_type / case_type / defect_target_type） ──
UPDATE `sys_dict` SET `dict_value` = 'AUTO_CASE', `dict_value_name` = '自动用例关键字', `remark` = '自动用例关键字', `updated_at` = NOW()
WHERE `dict_type` = 'keyword_type' AND `dict_value` = 'TEST_CASE';

UPDATE `sys_dict` SET `dict_value` = 'AUTO_CASE', `remark` = '自动用例', `updated_at` = NOW()
WHERE `dict_type` = 'case_type' AND `dict_value` = 'TEST_CASE';

UPDATE `sys_dict` SET `dict_value` = 'AUTO_CASE', `remark` = '自动用例', `updated_at` = NOW()
WHERE `dict_type` = 'defect_target_type' AND `dict_value` = 'TEST_CASE';

-- ── 9. 菜单同步（页面路由/组件/权限编码 + 按钮名称） ──
-- 自动测试套件页面
UPDATE `sys_menu` SET `name` = '自动测试套件', `route_path` = '/project/:id/auto-suites', `component` = 'cases/AutoSuiteList', `permission_code` = 'project:auto-suites', `updated_at` = NOW() WHERE `id` = 17;
-- 自动用例页面
UPDATE `sys_menu` SET `name` = '自动用例', `route_path` = '/project/:id/auto-cases', `component` = 'cases/AutoCaseList', `permission_code` = 'project:auto-cases', `updated_at` = NOW() WHERE `id` = 18;
-- 自动用例页面按钮（通用词：保存/Toggle/删除/分组管理 保持）
UPDATE `sys_menu` SET `permission_code` = 'project:auto-case:edit', `updated_at` = NOW() WHERE `id` = 31;
UPDATE `sys_menu` SET `name` = '新建自动用例', `permission_code` = 'project:auto-case:add', `updated_at` = NOW() WHERE `id` = 32;
UPDATE `sys_menu` SET `permission_code` = 'project:auto-case:toggle', `updated_at` = NOW() WHERE `id` = 33;
UPDATE `sys_menu` SET `permission_code` = 'project:auto-case:delete', `updated_at` = NOW() WHERE `id` = 34;
UPDATE `sys_menu` SET `permission_code` = 'project:auto-case:group', `updated_at` = NOW() WHERE `id` = 80;
-- 自动测试套件页面按钮
UPDATE `sys_menu` SET `permission_code` = 'project:auto-suite:edit', `updated_at` = NOW() WHERE `id` = 35;
UPDATE `sys_menu` SET `name` = '新建自动测试套件', `permission_code` = 'project:auto-suite:add', `updated_at` = NOW() WHERE `id` = 36;
UPDATE `sys_menu` SET `permission_code` = 'project:auto-suite:steps', `updated_at` = NOW() WHERE `id` = 37;
UPDATE `sys_menu` SET `permission_code` = 'project:auto-suite:delete', `updated_at` = NOW() WHERE `id` = 38;
UPDATE `sys_menu` SET `permission_code` = 'project:auto-suite:group', `updated_at` = NOW() WHERE `id` = 88;

-- ── 10. 权限同步（role_permission 经 permission_id 关联，不受影响） ──
-- 页面权限
UPDATE `permission` SET `permission_name` = '自动测试套件', `permission_code` = 'project:auto-suites', `description` = '自动测试套件页面', `updated_at` = NOW() WHERE `id` = 24;
UPDATE `permission` SET `permission_name` = '自动用例', `permission_code` = 'project:auto-cases', `description` = '自动用例页面', `updated_at` = NOW() WHERE `id` = 25;
-- 自动测试套件按钮权限（'步骤配置'/'分组管理'通用词保持）
UPDATE `permission` SET `permission_name` = '新建自动测试套件', `permission_code` = 'project:auto-suite:add', `description` = '新建自动测试套件按钮', `updated_at` = NOW() WHERE `id` = 76;
UPDATE `permission` SET `permission_name` = '编辑自动测试套件', `permission_code` = 'project:auto-suite:edit', `description` = '编辑自动测试套件按钮', `updated_at` = NOW() WHERE `id` = 77;
UPDATE `permission` SET `permission_name` = '删除自动测试套件', `permission_code` = 'project:auto-suite:delete', `description` = '删除自动测试套件按钮', `updated_at` = NOW() WHERE `id` = 78;
UPDATE `permission` SET `permission_code` = 'project:auto-suite:steps', `updated_at` = NOW() WHERE `id` = 79;
UPDATE `permission` SET `permission_code` = 'project:auto-suite:group', `updated_at` = NOW() WHERE `id` = 98;
-- 自动用例按钮权限
UPDATE `permission` SET `permission_name` = '新建自动用例', `permission_code` = 'project:auto-case:add', `description` = '新建自动用例按钮', `updated_at` = NOW() WHERE `id` = 80;
UPDATE `permission` SET `permission_name` = '编辑自动用例', `permission_code` = 'project:auto-case:edit', `description` = '编辑自动用例按钮', `updated_at` = NOW() WHERE `id` = 81;
UPDATE `permission` SET `permission_name` = '删除自动用例', `permission_code` = 'project:auto-case:delete', `description` = '删除自动用例按钮', `updated_at` = NOW() WHERE `id` = 82;
UPDATE `permission` SET `permission_name` = '启停自动用例', `permission_code` = 'project:auto-case:toggle', `description` = '禁用/启用自动用例按钮', `updated_at` = NOW() WHERE `id` = 83;
UPDATE `permission` SET `permission_code` = 'project:auto-case:group', `updated_at` = NOW() WHERE `id` = 97;
