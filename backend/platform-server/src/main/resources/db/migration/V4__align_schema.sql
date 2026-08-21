-- ============================================================
-- V4 对齐实体类与数据库 schema
-- 1. api_endpoint → api（表名+列名对齐实体 Api.java）
-- 2. api_keyword 增加 project_id/test_data/response_assertion，endpoint_id → api_id
-- 3. api_module 增加 parent_id（树形分组）
-- 4. keyword 增加 updated_by 列
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 重命名 api_endpoint → api，对齐列名
-- ============================================================
RENAME TABLE `api_endpoint` TO `api`;

-- 对齐 api 表列名与实体
ALTER TABLE `api`
  CHANGE COLUMN `method` `http_method` ENUM('GET','POST','PUT','PATCH','DELETE') NOT NULL COMMENT 'HTTP 方法（GET/POST/PUT/DELETE/PATCH）',
  CHANGE COLUMN `parameters` `request_params` JSON DEFAULT NULL COMMENT '请求参数（JSON 数组）',
  CHANGE COLUMN `responses` `response_body` JSON DEFAULT NULL COMMENT '响应体 Schema（JSON）',
  ADD COLUMN `project_id` BIGINT(20) DEFAULT NULL COMMENT '所属项目 ID' AFTER `id`,
  ADD COLUMN `service` VARCHAR(100) DEFAULT NULL COMMENT '服务名称' AFTER `name`,
  ADD COLUMN `headers` JSON DEFAULT NULL COMMENT '请求头（JSON 数组）' AFTER `request_body`,
  ADD COLUMN `swagger_operation_id` VARCHAR(200) DEFAULT NULL COMMENT 'Swagger 操作 ID（用于增量同步）' AFTER `source_type`;

-- 从 api_module 回填 project_id
UPDATE `api` a JOIN `api_module` m ON a.`module_id` = m.`id` SET a.`project_id` = m.`project_id`;

-- 更新 api 表索引
ALTER TABLE `api`
  DROP INDEX `uk_api_endpoint_module_path_method`,
  DROP INDEX `idx_api_endpoint_module_id`,
  DROP INDEX `idx_api_endpoint_method`,
  DROP INDEX `idx_api_endpoint_is_active`,
  ADD UNIQUE KEY `uk_api_module_path_method` (`module_id`, `path`, `http_method`),
  ADD KEY `idx_api_project_id` (`project_id`),
  ADD KEY `idx_api_http_method` (`http_method`),
  ADD KEY `idx_api_is_active` (`is_active`);

-- 添加 FK: api.project_id → project.id
ALTER TABLE `api`
  ADD CONSTRAINT `fk_api_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE;

-- ============================================================
-- 2. 对齐 api_keyword 表（endpoint_id → api_id，增加 test_data/response_assertion/project_id）
-- ============================================================
ALTER TABLE `api_keyword`
  DROP FOREIGN KEY `fk_api_keyword_endpoint_id`,
  DROP INDEX `idx_api_keyword_endpoint_id`;

ALTER TABLE `api_keyword`
  CHANGE COLUMN `endpoint_id` `api_id` BIGINT(20) NOT NULL COMMENT '绑定的接口 ID',
  ADD COLUMN `project_id` BIGINT(20) DEFAULT NULL COMMENT '所属项目 ID' AFTER `keyword_id`,
  ADD COLUMN `test_data` JSON DEFAULT NULL COMMENT '测试数据（JSON 格式）' AFTER `api_id`,
  ADD COLUMN `response_assertion` JSON DEFAULT NULL COMMENT '响应断言配置（JSON 格式）' AFTER `test_data`,
  ADD KEY `idx_api_keyword_api_id` (`api_id`),
  ADD KEY `idx_api_keyword_project_id` (`project_id`),
  ADD CONSTRAINT `fk_api_keyword_api_id` FOREIGN KEY (`api_id`) REFERENCES `api` (`id`) ON DELETE CASCADE;

-- 从 keyword 回填 api_keyword.project_id
UPDATE `api_keyword` ak JOIN `keyword` k ON ak.`keyword_id` = k.`id` SET ak.`project_id` = k.`project_id`;

-- ============================================================
-- 3. api_module 增加 parent_id（树形分组）
-- ============================================================
ALTER TABLE `api_module`
  ADD COLUMN `parent_id` BIGINT(20) DEFAULT NULL COMMENT '父分组 ID（null=根分组）' AFTER `project_id`,
  ADD KEY `idx_api_module_parent_id` (`parent_id`);

-- ============================================================
-- 4. keyword 增加 updated_by 列
-- ============================================================
ALTER TABLE `keyword`
  ADD COLUMN `updated_by` BIGINT(20) DEFAULT NULL COMMENT '更新人 ID' AFTER `created_by`,
  ADD KEY `idx_keyword_updated_by` (`updated_by`),
  ADD CONSTRAINT `fk_keyword_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`id`) ON DELETE SET NULL;
