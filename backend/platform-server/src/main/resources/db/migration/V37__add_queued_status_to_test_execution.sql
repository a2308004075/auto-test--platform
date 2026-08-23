-- V37: 为 test_execution.status ENUM 添加 QUEUED 状态（并发排队机制所需）
ALTER TABLE `test_execution` MODIFY COLUMN `status` ENUM('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED','QUEUED') NOT NULL DEFAULT 'PENDING' COMMENT '执行状态';
