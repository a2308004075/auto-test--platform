-- V37 测试计划分组 + 触发方式字段
-- 1) 新建 plan_group 表
CREATE TABLE IF NOT EXISTS `plan_group` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `project_id`  BIGINT       NOT NULL               COMMENT '所属项目 ID',
  `name`        VARCHAR(100) NOT NULL               COMMENT '分组名称',
  `description` VARCHAR(500) DEFAULT NULL            COMMENT '分组描述',
  `parent_id`   BIGINT       DEFAULT NULL            COMMENT '父分组 ID（NULL=顶级）',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序权重',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_group_project_id` (`project_id`),
  KEY `idx_plan_group_parent_id` (`parent_id`),
  CONSTRAINT `fk_plan_group_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_group_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `plan_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试计划分组表';

-- 2) test_plan 新增 group_id 和 trigger_type
ALTER TABLE `test_plan`
  ADD COLUMN `group_id`     BIGINT       DEFAULT NULL COMMENT '所属分组 ID' AFTER `description`,
  ADD COLUMN `trigger_type` VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式: MANUAL/SCHEDULED/CI' AFTER `schedule_cron`,
  ADD KEY `idx_test_plan_group_id` (`group_id`),
  ADD CONSTRAINT `fk_test_plan_group_id` FOREIGN KEY (`group_id`) REFERENCES `plan_group` (`id`) ON DELETE SET NULL;
