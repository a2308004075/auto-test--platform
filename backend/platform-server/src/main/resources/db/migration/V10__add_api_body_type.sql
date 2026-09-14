ALTER TABLE `api`
    ADD COLUMN `body_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'raw' COMMENT '请求体格式：none/form_data/x_www_form_urlencoded/raw/binary/graphql',
    ADD COLUMN `raw_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'json' COMMENT 'raw 子类型：text/javascript/json/html/xml';
