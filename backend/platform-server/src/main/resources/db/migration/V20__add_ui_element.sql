-- 界面元素模块：前端源码交互元素解析与 XPath 管理
-- 1. 界面元素表：从已拉取仓库的前端源码（.vue/.html）解析出的交互元素及其 XPath
CREATE TABLE `ui_element` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `repository_id` bigint NOT NULL COMMENT '来源仓库 ID（code_repository.id）',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '源码文件相对路径（相对仓库根目录，/ 分隔）',
  `element_tag` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '元素标签名（小写）',
  `element_id` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '元素 id 属性值',
  `element_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '元素 name 属性值',
  `element_class` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '元素 class 属性值',
  `element_text` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '元素文本内容（截断）',
  `element_placeholder` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '元素 placeholder 属性值',
  `element_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '元素 type 属性值（input 等使用）',
  `smart_xpath` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '智能 XPath（语义化定位，无法语义定位时为绝对路径）',
  `absolute_xpath` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '绝对 XPath（文档根完整路径）',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '元素在文件内的出现顺序号（从 1 开始）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ui_element_project_id` (`project_id`),
  KEY `idx_ui_element_repository_id` (`repository_id`),
  KEY `idx_ui_element_repo_file` (`repository_id`,`file_path`),
  CONSTRAINT `fk_ui_element_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ui_element_repository_id` FOREIGN KEY (`repository_id`) REFERENCES `code_repository` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='界面元素表';

-- 2. sys_menu：【项目资料】目录下新增【界面元素】菜单（置于源代码、接口文档之后）及按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_type`, `icon`, `route_path`, `component`, `permission_code`, `sort_no`, `is_active`, `created_at`, `updated_at`) VALUES
(91, 90, '界面元素', 2, '', '/project/:id/ui-elements', 'uielement/UiElementList', 'project:ui-elements', 3, 1, NOW(), NOW()),
(92, 91, '导入界面元素', 3, NULL, NULL, NULL, 'project:ui:import', 1, 1, NOW(), NOW()),
(93, 91, '删除', 3, NULL, NULL, NULL, 'project:ui:delete', 2, 1, NOW(), NOW());

-- 3. permission：与 sys_menu 同步（沿用 V1 初始化模式，项目级页面挂 parent=17）
INSERT INTO `permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `sort_order`, `is_active`, `description`, `control_mode`, `created_at`, `updated_at`) VALUES
(100, '界面元素', 'project:ui-elements', 'MENU', 17, NULL, 11, 1, '界面元素页面', 'display', NOW(), NOW()),
(101, '导入界面元素', 'project:ui:import', 'BUTTON', 100, NULL, 1, 1, '导入界面元素按钮', 'display', NOW(), NOW()),
(102, '删除', 'project:ui:delete', 'BUTTON', 100, NULL, 2, 1, '删除界面元素按钮', 'display', NOW(), NOW());

-- 4. role_permission：ADMIN 分配全部权限，TESTER 仅菜单可见（与现有项目级菜单的分配模式一致）
INSERT INTO `role_permission` (`role_id`, `permission_id`, `control_mode`, `created_at`) VALUES
(1, 100, NULL, NOW()),
(1, 101, 'enabled', NOW()),
(1, 102, 'enabled', NOW()),
(2, 100, NULL, NOW());
