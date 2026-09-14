-- 接口关键字分组升级：支持树形结构、系统分组与描述
ALTER TABLE `api_keyword_group`
  ADD COLUMN `parent_id` bigint DEFAULT NULL COMMENT '父分组 ID（null=根分组）' AFTER `project_id`,
  ADD COLUMN `is_system` tinyint NOT NULL DEFAULT 0 COMMENT '是否系统默认分组（0-否，1-是）' AFTER `parent_id`,
  ADD COLUMN `description` varchar(500) DEFAULT NULL COMMENT '分组描述' AFTER `name`,
  ADD KEY `idx_api_keyword_group_parent_id` (`parent_id`);

-- 为已有项目自动补齐系统分组（全部/未分组），已存在同名分组则忽略
INSERT IGNORE INTO `api_keyword_group` (`project_id`, `parent_id`, `is_system`, `name`, `description`, `created_by`)
SELECT `id`, NULL, 1, '全部', '系统默认分组，包含所有接口关键字', NULL
FROM `project`;

INSERT IGNORE INTO `api_keyword_group` (`project_id`, `parent_id`, `is_system`, `name`, `description`, `created_by`)
SELECT `id`, NULL, 1, '未分类', '未分类的接口关键字', NULL
FROM `project`;
