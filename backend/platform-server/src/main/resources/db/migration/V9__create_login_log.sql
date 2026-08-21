-- ============================================================
-- V9 创建 login_log 登录日志表
-- 用于记录用户登录成功/失败历史，供个人资料页面"登录记录"展示。
-- 该表为纯日志表，不使用逻辑删除，不继承 BaseEntity 的 updated_at。
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `login_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `user_id`     BIGINT       NULL                                 COMMENT '用户 ID（登录失败且用户不存在时为 NULL）',
  `username`    VARCHAR(50)  NOT NULL                             COMMENT '登录时输入的用户名',
  `status`      VARCHAR(16)  NOT NULL                             COMMENT '登录状态：SUCCESS / FAILED',
  `ip`          VARCHAR(45)  NULL                                 COMMENT '客户端 IP 地址',
  `user_agent`  VARCHAR(512) NULL                                 COMMENT '完整 User-Agent',
  `browser`     VARCHAR(64)  NULL                                 COMMENT '解析后的浏览器名称',
  `os`          VARCHAR(64)  NULL                                 COMMENT '解析后的操作系统',
  `message`     VARCHAR(200) NULL                                 COMMENT '附加信息（如失败原因）',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_log_user_id` (`user_id`),
  KEY `idx_login_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志';
