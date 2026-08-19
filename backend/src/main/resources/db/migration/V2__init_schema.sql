-- ============================================================
-- V2 初始化业务库表结构
-- 基于 SRS v1.3 / LLD v1.0 核心数据模型
-- 字符集：utf8mb4，主键：UUID CHAR(36)
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- M1 认证与用户管理
-- ============================================================

CREATE TABLE IF NOT EXISTS `user` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `username`        VARCHAR(50)   NOT NULL                    COMMENT '账号（登录名）',
  `password_hash`   VARCHAR(255)  NOT NULL                    COMMENT 'bcrypt 哈希密码',
  `display_name`    VARCHAR(50)   NOT NULL                    COMMENT '用户姓名（显示名）',
  `role`            ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER' COMMENT '角色',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `last_login_at`   DATETIME      DEFAULT NULL                COMMENT '最近登录时间',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role` (`role`),
  KEY `idx_user_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `global_settings` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `config_key`      VARCHAR(100)  NOT NULL                    COMMENT '配置键',
  `config_value`    JSON          NOT NULL                    COMMENT '配置值',
  `description`     VARCHAR(255)  DEFAULT NULL                COMMENT '配置说明',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_global_settings_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全局配置表';

CREATE TABLE IF NOT EXISTS `token_blacklist` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `token_jti`       VARCHAR(100)  NOT NULL                    COMMENT 'Token 唯一标识（JWT ID）',
  `user_id`         CHAR(36)      NOT NULL                    COMMENT '用户 ID',
  `expires_at`      DATETIME      NOT NULL                    COMMENT 'Token 原始过期时间（用于定期清理）',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_blacklist_jti` (`token_jti`),
  KEY `idx_token_blacklist_expires_at` (`expires_at`),
  KEY `idx_token_blacklist_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token 黑名单';

-- ============================================================
-- M2 项目管理 / M3 环境配置
-- ============================================================

CREATE TABLE IF NOT EXISTS `project` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `name`            VARCHAR(50)   NOT NULL                    COMMENT '项目名称',
  `description`     VARCHAR(500)  DEFAULT NULL                COMMENT '项目描述',
  `source_path`     VARCHAR(500)  DEFAULT NULL                COMMENT '项目源码路径（引擎运行时读取）',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用（软删除）',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_name` (`name`),
  KEY `idx_project_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

CREATE TABLE IF NOT EXISTS `environment` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `project_id`      CHAR(36)      NOT NULL                    COMMENT '所属项目 ID',
  `name`            VARCHAR(50)   NOT NULL                    COMMENT '环境名称，如 test/staging/prod',
  `description`     VARCHAR(255)  DEFAULT NULL                COMMENT '环境描述',
  `config`          JSON          NOT NULL                    COMMENT '环境配置 JSON（host、认证、wss、nacos 等）',
  `is_current`      BOOLEAN       NOT NULL DEFAULT FALSE      COMMENT '是否为当前激活环境',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_environment_project_name` (`project_id`, `name`),
  KEY `idx_environment_project_id` (`project_id`),
  KEY `idx_environment_is_current` (`is_current`),
  CONSTRAINT `fk_environment_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='环境配置表';

-- ============================================================
-- M4 接口文档
-- ============================================================

CREATE TABLE IF NOT EXISTS `api_module` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `project_id`      CHAR(36)      NOT NULL                    COMMENT '所属项目 ID',
  `name`            VARCHAR(100)  NOT NULL                    COMMENT '分组名称',
  `service_prefix`  VARCHAR(200)  DEFAULT NULL                COMMENT '服务前缀，如 /users',
  `description`     VARCHAR(500)  DEFAULT NULL                COMMENT '分组描述',
  `source_type`     ENUM('SWAGGER_IMPORT','MANUAL') NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
  `swagger_file`    VARCHAR(500)  DEFAULT NULL                COMMENT '导入的 Swagger 文件路径',
  `is_system`       BOOLEAN       NOT NULL DEFAULT FALSE      COMMENT '是否为系统默认分组（全部/未分组）',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_module_project_name` (`project_id`, `name`),
  KEY `idx_api_module_project_id` (`project_id`),
  CONSTRAINT `fk_api_module_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口分组表';

CREATE TABLE IF NOT EXISTS `api_endpoint` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `module_id`       CHAR(36)      NOT NULL                    COMMENT '所属接口分组 ID',
  `name`            VARCHAR(100)  NOT NULL                    COMMENT '接口名称',
  `path`            VARCHAR(500)  NOT NULL                    COMMENT '请求路径',
  `method`          ENUM('GET','POST','PUT','PATCH','DELETE') NOT NULL COMMENT 'HTTP 方法',
  `description`     VARCHAR(1000) DEFAULT NULL                COMMENT '接口描述',
  `parameters`      JSON          DEFAULT NULL                COMMENT '请求参数定义 [{name, in, type, required, description}]',
  `request_body`    JSON          DEFAULT NULL                COMMENT '请求体 Schema',
  `responses`       JSON          DEFAULT NULL                COMMENT '响应定义 {statusCode: schema}',
  `content_type`    VARCHAR(100)  NOT NULL DEFAULT 'application/json' COMMENT '默认 Content-Type',
  `source_type`     ENUM('SWAGGER_IMPORT','MANUAL') NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_endpoint_module_path_method` (`module_id`, `path`, `method`),
  KEY `idx_api_endpoint_module_id` (`module_id`),
  KEY `idx_api_endpoint_method` (`method`),
  KEY `idx_api_endpoint_is_active` (`is_active`),
  CONSTRAINT `fk_api_endpoint_module_id` FOREIGN KEY (`module_id`) REFERENCES `api_module` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口定义表';

-- ============================================================
-- M5/M6/M7 关键字管理（Keyword 统一实体 + 源实体）
-- ============================================================

CREATE TABLE IF NOT EXISTS `keyword` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `project_id`      CHAR(36)      NOT NULL                    COMMENT '所属项目 ID',
  `keyword_type`    ENUM('API','TOOL','ACTION','TEST_CASE') NOT NULL COMMENT '关键字类型',
  `ref_id`          CHAR(36)      DEFAULT NULL                COMMENT '指向源实体 ID（ApiEndpoint / ToolMethod / Action / TestCase）',
  `name`            VARCHAR(100)  NOT NULL                    COMMENT '关键字名称',
  `description`     VARCHAR(1000) DEFAULT NULL                COMMENT '关键字描述',
  `input_params`    JSON          DEFAULT NULL                COMMENT '输入参数定义',
  `output_params`   JSON          DEFAULT NULL                COMMENT '输出参数定义',
  `config`          JSON          DEFAULT NULL                COMMENT '类型特定配置（测试数据、预期响应等）',
  `category`        VARCHAR(100)  DEFAULT NULL                COMMENT '分类',
  `tags`            JSON          DEFAULT NULL                COMMENT '标签列表',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `created_by`      CHAR(36)      DEFAULT NULL                COMMENT '创建人 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword_project_name` (`project_id`, `name`),
  KEY `idx_keyword_project_id` (`project_id`),
  KEY `idx_keyword_type` (`keyword_type`),
  KEY `idx_keyword_ref_id` (`ref_id`),
  KEY `idx_keyword_category` (`category`),
  KEY `idx_keyword_created_by` (`created_by`),
  CONSTRAINT `fk_keyword_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_keyword_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关键字统一表';

CREATE TABLE IF NOT EXISTS `api_keyword` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `keyword_id`      CHAR(36)      NOT NULL                    COMMENT '关键字元数据 ID',
  `endpoint_id`     CHAR(36)      NOT NULL                    COMMENT '绑定的接口 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_keyword_keyword_id` (`keyword_id`),
  KEY `idx_api_keyword_endpoint_id` (`endpoint_id`),
  CONSTRAINT `fk_api_keyword_keyword_id` FOREIGN KEY (`keyword_id`) REFERENCES `keyword` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_api_keyword_endpoint_id` FOREIGN KEY (`endpoint_id`) REFERENCES `api_endpoint` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口关键字源实体表';

CREATE TABLE IF NOT EXISTS `tool_method` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `project_id`      CHAR(36)      NOT NULL                    COMMENT '所属项目 ID',
  `name`            VARCHAR(100)  NOT NULL                    COMMENT '方法名称',
  `keyword`         VARCHAR(20)   NOT NULL                    COMMENT '关键字标识（Action 节点引用）',
  `category`        VARCHAR(100)  DEFAULT NULL                COMMENT '分类',
  `description`     VARCHAR(1000) DEFAULT NULL                COMMENT '方法描述',
  `code`            TEXT          NOT NULL                    COMMENT '实现代码',
  `parameters`      JSON          DEFAULT NULL                COMMENT '参数定义',
  `return_type`     VARCHAR(100)  DEFAULT NULL                COMMENT '返回类型',
  `return_description` VARCHAR(255) DEFAULT NULL             COMMENT '返回值说明',
  `is_builtin`      BOOLEAN       NOT NULL DEFAULT FALSE      COMMENT '是否内置方法',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `created_by`      CHAR(36)      DEFAULT NULL                COMMENT '创建人 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_method_project_keyword` (`project_id`, `keyword`),
  KEY `idx_tool_method_project_id` (`project_id`),
  KEY `idx_tool_method_created_by` (`created_by`),
  CONSTRAINT `fk_tool_method_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tool_method_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具方法关键字源实体表';

CREATE TABLE IF NOT EXISTS `action` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `project_id`      CHAR(36)      NOT NULL                    COMMENT '所属项目 ID',
  `nodes`           JSON          NOT NULL                    COMMENT '内部节点树（流程图拓扑序列化）',
  `created_by`      CHAR(36)      DEFAULT NULL                COMMENT '创建人 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_action_project_id` (`project_id`),
  KEY `idx_action_created_by` (`created_by`),
  CONSTRAINT `fk_action_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_action_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Action 关键字源实体表';

-- ============================================================
-- M8 测试用例管理
-- ============================================================

CREATE TABLE IF NOT EXISTS `test_suite` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `project_id`      CHAR(36)      NOT NULL                    COMMENT '所属项目 ID',
  `name`            VARCHAR(100)  NOT NULL                    COMMENT '套件名称',
  `description`     VARCHAR(1000) DEFAULT NULL                COMMENT '套件描述',
  `tags`            JSON          DEFAULT NULL                COMMENT '标签列表',
  `priority`        ENUM('P0','P1','P2','P3') DEFAULT 'P2'    COMMENT '优先级',
  `once_setup_steps`     JSON     DEFAULT NULL                COMMENT '套件级·整体 Setup 步骤树',
  `once_teardown_steps`  JSON     DEFAULT NULL                COMMENT '套件级·整体 Teardown 步骤树',
  `enable_once_setup_teardown` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用套件级·整体生命周期',
  `per_case_setup_steps`    JSON  DEFAULT NULL                COMMENT '套件级·每条 Setup 步骤树',
  `per_case_teardown_steps` JSON  DEFAULT NULL                COMMENT '套件级·每条 Teardown 步骤树',
  `enable_per_case_setup_teardown` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用套件级·每条生命周期',
  `created_by`      CHAR(36)      DEFAULT NULL                COMMENT '创建人 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_suite_project_name` (`project_id`, `name`),
  KEY `idx_test_suite_project_id` (`project_id`),
  KEY `idx_test_suite_priority` (`priority`),
  KEY `idx_test_suite_created_by` (`created_by`),
  CONSTRAINT `fk_test_suite_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_test_suite_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试套件表';

CREATE TABLE IF NOT EXISTS `test_case` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `suite_id`        CHAR(36)      NOT NULL                    COMMENT '所属测试套件 ID',
  `name`            VARCHAR(100)  NOT NULL                    COMMENT '用例名称',
  `description`     VARCHAR(1000) DEFAULT NULL                COMMENT '用例描述',
  `preconditions`   TEXT          DEFAULT NULL                COMMENT '前置条件',
  `setup_steps`     JSON          DEFAULT NULL                COMMENT '用例级 Setup 步骤树',
  `teardown_steps`  JSON          DEFAULT NULL                COMMENT '用例级 Teardown 步骤树',
  `steps`           JSON          NOT NULL                    COMMENT '用例步骤树',
  `priority`        ENUM('P0','P1','P2','P3') DEFAULT 'P2'    COMMENT '优先级',
  `timeout`         INT           NOT NULL DEFAULT 30         COMMENT '超时秒数',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `created_by`      CHAR(36)      DEFAULT NULL                COMMENT '创建人 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_case_suite_name` (`suite_id`, `name`),
  KEY `idx_test_case_suite_id` (`suite_id`),
  KEY `idx_test_case_priority` (`priority`),
  KEY `idx_test_case_is_active` (`is_active`),
  KEY `idx_test_case_created_by` (`created_by`),
  CONSTRAINT `fk_test_case_suite_id` FOREIGN KEY (`suite_id`) REFERENCES `test_suite` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_test_case_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例源实体表';

CREATE TABLE IF NOT EXISTS `suite_case_lifecycle` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `suite_id`        CHAR(36)      NOT NULL                    COMMENT '所属测试套件 ID',
  `case_id`         CHAR(36)      NOT NULL                    COMMENT '所属测试用例 ID',
  `setup_steps`     JSON          DEFAULT NULL                COMMENT '套件内该用例差异化 Setup 步骤树',
  `teardown_steps`  JSON          DEFAULT NULL                COMMENT '套件内该用例差异化 Teardown 步骤树',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_suite_case_lifecycle_suite_case` (`suite_id`, `case_id`),
  KEY `idx_suite_case_lifecycle_case_id` (`case_id`),
  CONSTRAINT `fk_suite_case_lifecycle_suite_id` FOREIGN KEY (`suite_id`) REFERENCES `test_suite` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_suite_case_lifecycle_case_id` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套件内用例级生命周期表';

-- ============================================================
-- M9 测试执行与调度
-- ============================================================

CREATE TABLE IF NOT EXISTS `test_plan` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `project_id`      CHAR(36)      NOT NULL                    COMMENT '所属项目 ID',
  `name`            VARCHAR(100)  NOT NULL                    COMMENT '计划名称',
  `description`     VARCHAR(1000) DEFAULT NULL                COMMENT '计划描述',
  `suite_ids`       JSON          NOT NULL                    COMMENT '关联的测试套件 ID 列表',
  `environment_id`  CHAR(36)      DEFAULT NULL                COMMENT '默认执行环境 ID',
  `schedule_cron`   VARCHAR(100)  DEFAULT NULL                COMMENT '定时执行 cron 表达式',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `created_by`      CHAR(36)      DEFAULT NULL                COMMENT '创建人 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_plan_project_name` (`project_id`, `name`),
  KEY `idx_test_plan_project_id` (`project_id`),
  KEY `idx_test_plan_environment_id` (`environment_id`),
  KEY `idx_test_plan_created_by` (`created_by`),
  CONSTRAINT `fk_test_plan_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_test_plan_environment_id` FOREIGN KEY (`environment_id`) REFERENCES `environment` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_test_plan_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试计划表';

CREATE TABLE IF NOT EXISTS `test_execution` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `plan_id`         CHAR(36)      NOT NULL                    COMMENT '所属测试计划 ID',
  `environment_id`  CHAR(36)      DEFAULT NULL                COMMENT '执行环境 ID',
  `trigger_type`    ENUM('MANUAL','SCHEDULED','CI') NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式',
  `status`          ENUM('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '执行状态',
  `total_cases`     INT           NOT NULL DEFAULT 0          COMMENT '总用例数',
  `passed_cases`    INT           NOT NULL DEFAULT 0          COMMENT '通过用例数',
  `failed_cases`    INT           NOT NULL DEFAULT 0          COMMENT '失败用例数',
  `skipped_cases`   INT           NOT NULL DEFAULT 0          COMMENT '跳过用例数',
  `duration_ms`     INT           DEFAULT NULL                COMMENT '总耗时（毫秒）',
  `started_at`      DATETIME      DEFAULT NULL                COMMENT '开始执行时间',
  `finished_at`     DATETIME      DEFAULT NULL                COMMENT '结束执行时间',
  `triggered_by`    CHAR(36)      DEFAULT NULL                COMMENT '触发人 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_test_execution_plan_id` (`plan_id`),
  KEY `idx_test_execution_status` (`status`),
  KEY `idx_test_execution_environment_id` (`environment_id`),
  KEY `idx_test_execution_triggered_by` (`triggered_by`),
  KEY `idx_test_execution_created_at` (`created_at`),
  CONSTRAINT `fk_test_execution_plan_id` FOREIGN KEY (`plan_id`) REFERENCES `test_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_test_execution_environment_id` FOREIGN KEY (`environment_id`) REFERENCES `environment` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_test_execution_triggered_by` FOREIGN KEY (`triggered_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试执行记录表';

CREATE TABLE IF NOT EXISTS `test_result` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `execution_id`    CHAR(36)      NOT NULL                    COMMENT '所属执行记录 ID',
  `case_id`         CHAR(36)      NOT NULL                    COMMENT '所属测试用例 ID',
  `status`          ENUM('PASSED','FAILED','SKIPPED','ERROR') NOT NULL COMMENT '用例执行结果',
  `actual_result`   TEXT          DEFAULT NULL                COMMENT '实际结果摘要',
  `expected_result` TEXT          DEFAULT NULL                COMMENT '预期结果摘要',
  `error_message`   TEXT          DEFAULT NULL                COMMENT '错误信息',
  `logs`            JSON          DEFAULT NULL                COMMENT '执行日志（每步骤 req/res 详情）',
  `duration_ms`     INT           DEFAULT NULL                COMMENT '执行耗时（毫秒）',
  `started_at`      DATETIME      DEFAULT NULL                COMMENT '开始执行时间',
  `finished_at`     DATETIME      DEFAULT NULL                COMMENT '结束执行时间',
  PRIMARY KEY (`id`),
  KEY `idx_test_result_execution_id` (`execution_id`),
  KEY `idx_test_result_case_id` (`case_id`),
  KEY `idx_test_result_status` (`status`),
  CONSTRAINT `fk_test_result_execution_id` FOREIGN KEY (`execution_id`) REFERENCES `test_execution` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_test_result_case_id` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试结果明细表';
