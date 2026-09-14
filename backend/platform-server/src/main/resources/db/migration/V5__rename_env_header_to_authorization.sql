-- 将固定变量 header 重命名为 authorization，数据类型统一为 text
UPDATE `environment_variable`
SET `var_key` = 'authorization',
    `data_type` = 'text'
WHERE `var_key` = 'header';
