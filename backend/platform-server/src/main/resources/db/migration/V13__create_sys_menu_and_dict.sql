-- ============================================================
-- V13 新增系统管理模块：菜单管理 + 字典管理
-- sys_menu: 系统菜单树（驱动侧边栏动态导航）
-- sys_dict: 数据字典（键值对管理）
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 菜单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0                   COMMENT '父级菜单 ID（0 为顶级）',
  `name`        VARCHAR(50)  NOT NULL                             COMMENT '菜单名称',
  `menu_type`   TINYINT      NOT NULL DEFAULT 1                   COMMENT '菜单类型：1=目录 2=菜单 3=按钮',
  `icon`        VARCHAR(50)  DEFAULT NULL                         COMMENT '图标名称',
  `route_path`  VARCHAR(200) DEFAULT NULL                         COMMENT '路由路径',
  `sort_no`     INT          NOT NULL DEFAULT 0                   COMMENT '排序号（升序）',
  `is_active`   TINYINT   NOT NULL DEFAULT 1                   COMMENT '是否启用（1=启用 0=停用）',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  KEY `idx_sys_menu_sort_no` (`sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- ============================================================
-- 字典表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `dict_type`        VARCHAR(100) NOT NULL                             COMMENT '字典类型编码',
  `dict_type_name`   VARCHAR(100) NOT NULL                             COMMENT '字典类型名称',
  `dict_value`       VARCHAR(100) NOT NULL                             COMMENT '字典值',
  `dict_value_name`  VARCHAR(200) NOT NULL                             COMMENT '字典值名称',
  `sort_no`          INT          NOT NULL DEFAULT 0                   COMMENT '排序号',
  `remark`           VARCHAR(500) DEFAULT NULL                         COMMENT '备注',
  `is_active`        TINYINT   NOT NULL DEFAULT 1                   COMMENT '是否启用（1=启用 0=停用）',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_dict_type` (`dict_type`),
  KEY `idx_sys_dict_sort_no` (`sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';

-- ============================================================
-- 初始菜单数据（与现有侧边栏菜单对齐）
-- ============================================================

-- 系统管理（顶级目录）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `sort_no`) VALUES
(1, 0, '系统管理', 1, 'Setting', '', 1);

-- 系统管理 → 子菜单
INSERT INTO `sys_menu` (`parent_id`, `name`, `menu_type`, `icon`, `route_path`, `sort_no`) VALUES
(1, '个人资料', 2, '', '/settings/profile', 1),
(1, '用户列表', 2, '', '/settings/users', 2),
(1, '全局设置', 2, '', '/settings/global-config', 3),
(1, '菜单管理', 2, '', '/settings/menu', 4),
(1, '字典管理', 2, '', '/settings/dict', 5),
(1, '缓存管理', 2, '', '/settings/cache', 6);
