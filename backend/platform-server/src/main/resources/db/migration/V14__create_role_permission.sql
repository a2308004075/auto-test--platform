-- ============================================================
-- V14 角色管理权限表
-- 新建 permission（权限树）和 role_permission（角色-权限关联）表
-- 为 user_role 表新增 sort_order 排序字段
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 为 user_role 新增排序字段
-- ============================================================

ALTER TABLE `user_role`
  ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号（升序）' AFTER `description`;

-- ============================================================
-- 权限表（菜单 + 按钮统一管理）
-- ============================================================

CREATE TABLE IF NOT EXISTS `permission` (
  `id`              BIGINT(20)    NOT NULL AUTO_INCREMENT      COMMENT '自增主键',
  `permission_name` VARCHAR(100)  NOT NULL                    COMMENT '权限名称（显示名）',
  `permission_code` VARCHAR(100)  NOT NULL                    COMMENT '权限编码（如 system:role:add）',
  `type`            VARCHAR(10)   NOT NULL DEFAULT 'MENU'     COMMENT '权限类型：MENU-菜单/页面，BUTTON-按钮',
  `parent_id`       BIGINT(20)    NOT NULL DEFAULT 0          COMMENT '父权限 ID（0 为顶级）',
  `path`            VARCHAR(200)  DEFAULT NULL                COMMENT '前端路由路径（MENU 类型使用）',
  `sort_order`      INT           NOT NULL DEFAULT 0          COMMENT '排序号（升序）',
  `is_active`       TINYINT       NOT NULL DEFAULT 1          COMMENT '是否启用（0-停用，1-启用）',
  `description`     VARCHAR(255)  DEFAULT NULL                COMMENT '权限描述',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ============================================================
-- 角色-权限关联表
-- ============================================================

CREATE TABLE IF NOT EXISTS `role_permission` (
  `id`             BIGINT(20)    NOT NULL AUTO_INCREMENT      COMMENT '自增主键',
  `role_id`        BIGINT(20)    NOT NULL                    COMMENT '角色 ID（关联 user_role.id）',
  `permission_id`  BIGINT(20)    NOT NULL                    COMMENT '权限 ID（关联 permission.id）',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_permission_role_id` (`role_id`),
  KEY `idx_role_permission_permission_id` (`permission_id`),
  CONSTRAINT `fk_role_permission_role_id` FOREIGN KEY (`role_id`) REFERENCES `user_role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_permission_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ============================================================
-- 初始化权限种子数据（使用显式 ID，便于 parent_id 引用）
-- ============================================================

INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `description`) VALUES
-- 顶级菜单
(1,  '首页',       'home',           'MENU', 0,  '/home',                  0,  '项目首页'),
(2,  '系统管理',    'system',         'MENU', 0,  '/settings',             10, '系统管理根菜单'),
(17, '项目内页面',  'project',        'MENU', 0,  NULL,                    20, '项目内功能页面'),
-- 系统管理子菜单
(3,  '个人资料',    'system:profile', 'MENU', 2,  '/settings/profile',     1,  '个人资料页面'),
(4,  '用户列表',    'system:user',    'MENU', 2,  '/settings/users',       2,  '用户管理页面'),
(5,  '角色管理',    'system:role',    'MENU', 2,  '/settings/roles',       3,  '角色管理页面'),
(6,  '全局设置',    'system:config',  'MENU', 2,  '/settings/global-config', 4, '全局配置页面'),
-- 用户管理按钮
(7,  '新建用户',    'system:user:add',           'BUTTON', 4, NULL, 1, '创建用户按钮'),
(8,  '编辑用户',    'system:user:edit',          'BUTTON', 4, NULL, 2, '编辑用户按钮'),
(9,  '删除用户',    'system:user:delete',        'BUTTON', 4, NULL, 3, '删除用户按钮'),
(10, '重置密码',    'system:user:reset-password','BUTTON', 4, NULL, 4, '重置密码按钮'),
-- 角色管理按钮
(11, '新建角色',    'system:role:add',       'BUTTON', 5, NULL, 1, '创建角色按钮'),
(12, '编辑角色',    'system:role:edit',      'BUTTON', 5, NULL, 2, '编辑角色按钮'),
(13, '删除角色',    'system:role:delete',    'BUTTON', 5, NULL, 3, '删除角色按钮'),
(14, '分配权限',    'system:role:permission','BUTTON', 5, NULL, 4, '分配权限按钮'),
(15, '导入角色',    'system:role:import',    'BUTTON', 5, NULL, 5, 'Excel 导入按钮'),
(16, '导出角色',    'system:role:export',    'BUTTON', 5, NULL, 6, 'Excel 导出按钮'),
-- 项目内页面菜单
(18, '仪表板',      'project:dashboard',   'MENU', 17, NULL, 1,  '项目仪表板'),
(19, '接口管理',    'project:apis',        'MENU', 17, NULL, 2,  '接口管理页面'),
(20, '环境配置',    'project:environments','MENU', 17, NULL, 3,  '环境配置页面'),
(21, '接口关键字',  'project:keywords',    'MENU', 17, NULL, 4,  '接口关键字页面'),
(22, '工具方法',    'project:tools',       'MENU', 17, NULL, 5,  '工具方法页面'),
(23, 'Action',     'project:actions',     'MENU', 17, NULL, 6,  'Action 页面'),
(24, '测试套件',    'project:suites',      'MENU', 17, NULL, 7,  '测试套件页面'),
(25, '测试用例',    'project:cases',       'MENU', 17, NULL, 8,  '测试用例页面'),
(26, '测试计划',    'project:plans',       'MENU', 17, NULL, 9,  '测试计划页面'),
(27, '执行记录',    'project:executions',  'MENU', 17, NULL, 10, '执行记录页面');

-- ============================================================
-- 初始化角色权限关联
-- ADMIN（role_id=1）：全部 27 条权限
-- TESTER（role_id=2）：首页 + 个人资料 + 全部项目页面
-- ============================================================

-- ADMIN 全部权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission`;

-- TESTER 有限权限（首页、个人资料、项目内全部页面）
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(2, 1),   -- 首页
(2, 3),   -- 个人资料
(2, 17),  -- 项目内页面（父菜单）
(2, 18),  -- 仪表板
(2, 19),  -- 接口管理
(2, 20),  -- 环境配置
(2, 21),  -- 接口关键字
(2, 22),  -- 工具方法
(2, 23),  -- Action
(2, 24),  -- 测试套件
(2, 25),  -- 测试用例
(2, 26),  -- 测试计划
(2, 27);  -- 执行记录
