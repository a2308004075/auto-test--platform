-- 环境变量表新增数据类型字段
ALTER TABLE `environment_variable` ADD COLUMN `data_type` varchar(20) NOT NULL DEFAULT 'text' COMMENT '数据类型：text/number/json' AFTER `var_value`;
