-- 测试代码库模块：Git 仓库登记与拉取历史
-- 1. 仓库表：项目下登记的 Git 仓库（地址/分支/认证凭证）
CREATE TABLE `code_repository` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '仓库名称',
  `git_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Git 仓库地址（http(s):// 或 git@）',
  `branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拉取分支（NULL=仓库默认分支）',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库描述',
  `auth_username` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认证用户名（私有仓库，兼容 Token 用户名）',
  `auth_password` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认证密码/Token（AES 加密，enc: 前缀）',
  `local_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '本地代码目录（相对存储根目录）',
  `last_pull_at` datetime DEFAULT NULL COMMENT '最近一次拉取时间',
  `last_pull_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近一次拉取状态：RUNNING/SUCCESS/FAILED',
  `last_commit_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近一次拉取成功后的 HEAD commit ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_repository_project_name` (`project_id`,`name`),
  KEY `idx_code_repository_project_id` (`project_id`),
  CONSTRAINT `fk_code_repository_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试代码仓库表';

-- 2. 拉取历史表：每次 clone/pull 的执行记录
CREATE TABLE `code_repository_pull_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `repository_id` bigint NOT NULL COMMENT '所属仓库 ID',
  `pull_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '拉取类型：CLONE-首次克隆，PULL-增量更新',
  `branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拉取分支',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '拉取状态：RUNNING-拉取中，SUCCESS-成功，FAILED-失败',
  `commit_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拉取成功后的 HEAD commit ID',
  `message` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果信息（成功为概要，失败为原因）',
  `duration_ms` bigint DEFAULT NULL COMMENT '拉取耗时（毫秒）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_code_repo_pull_log_repository_id` (`repository_id`),
  CONSTRAINT `fk_code_repo_pull_log_repository_id` FOREIGN KEY (`repository_id`) REFERENCES `code_repository` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码仓库拉取历史表';

-- 3. sys_menu：新增【测试代码库】菜单（置于环境配置与接口文档之间）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(82, 10, '测试代码库', 2, '', '/project/:id/repositories', 'repository/RepositoryList', 'project:repositories', 2, 1, NOW(), NOW()),
(83, 82, '新建仓库', 3, NULL, NULL, NULL, 'project:repo:add', 0, 1, NOW(), NOW()),
(84, 82, '拉取', 3, NULL, NULL, NULL, 'project:repo:pull', 0, 1, NOW(), NOW()),
(85, 82, '编辑', 3, NULL, NULL, NULL, 'project:repo:edit', 0, 1, NOW(), NOW()),
(86, 82, '删除', 3, NULL, NULL, NULL, 'project:repo:delete', 0, 1, NOW(), NOW()),
(87, 82, '拉取记录', 3, NULL, NULL, NULL, 'project:repo:logs', 0, 1, NOW(), NOW());

-- 4. 调整同级菜单排序：接口文档/关键字管理/测试管理依次后移，保证新菜单位于环境配置与接口文档之间
UPDATE `sys_menu` SET `sort_no` = 3, `updated_at` = NOW() WHERE `id` = 12;  -- 接口文档 2 -> 3
UPDATE `sys_menu` SET `sort_no` = 4, `updated_at` = NOW() WHERE `id` = 79;  -- 关键字管理 3 -> 4
UPDATE `sys_menu` SET `sort_no` = 5, `updated_at` = NOW() WHERE `id` = 78;  -- 测试管理 4 -> 5

-- 5. permission：与 sys_menu 同步（沿用 V1 初始化模式）
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`) VALUES
(91, '测试代码库', 'project:repositories', 'MENU', 17, NULL, 2, 1, '测试代码库页面', 'display', NOW(), NOW()),
(92, '新建仓库', 'project:repo:add', 'BUTTON', 91, NULL, 1, 1, '新建仓库按钮', 'display', NOW(), NOW()),
(93, '拉取仓库', 'project:repo:pull', 'BUTTON', 91, NULL, 2, 1, '拉取代码按钮', 'display', NOW(), NOW()),
(94, '编辑仓库', 'project:repo:edit', 'BUTTON', 91, NULL, 3, 1, '编辑仓库按钮', 'display', NOW(), NOW()),
(95, '删除仓库', 'project:repo:delete', 'BUTTON', 91, NULL, 4, 1, '删除仓库按钮', 'display', NOW(), NOW()),
(96, '拉取记录', 'project:repo:logs', 'BUTTON', 91, NULL, 5, 1, '查看拉取记录按钮', 'display', NOW(), NOW());

-- 6. role_permission：ADMIN 分配全部权限，TESTER 仅菜单可见（与现有项目级菜单的分配模式一致）
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`) VALUES
(1, 91, NULL, NOW()),
(1, 92, 'enabled', NOW()),
(1, 93, 'enabled', NOW()),
(1, 94, 'enabled', NOW()),
(1, 95, 'enabled', NOW()),
(1, 96, 'enabled', NOW()),
(2, 91, NULL, NOW());

-- 7. sys_dict：新增拉取状态字典
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('repository_pull_status', '仓库拉取状态', 'RUNNING', '拉取中', 1, '正在拉取代码', 1, NOW(), NOW()),
('repository_pull_status', '仓库拉取状态', 'SUCCESS', '成功', 2, '拉取成功', 1, NOW(), NOW()),
('repository_pull_status', '仓库拉取状态', 'FAILED', '失败', 3, '拉取失败', 1, NOW(), NOW());
