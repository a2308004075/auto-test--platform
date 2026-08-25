-- Content-Type 字典：接口文档 Content-Type 下拉选项
INSERT INTO `sys_dict` (`dict_type`, `dict_type_name`, `dict_value`, `dict_value_name`, `sort_no`, `remark`, `is_active`, `created_at`, `updated_at`) VALUES
('content_type', 'Content-Type', 'application/json', 'application/json', 1, 'JSON 请求体', 1, NOW(), NOW()),
('content_type', 'Content-Type', 'application/x-www-form-urlencoded', 'application/x-www-form-urlencoded', 2, '表单提交', 1, NOW(), NOW()),
('content_type', 'Content-Type', 'multipart/form-data', 'multipart/form-data', 3, '文件上传', 1, NOW(), NOW()),
('content_type', 'Content-Type', 'text/plain', 'text/plain', 4, '纯文本', 1, NOW(), NOW()),
('content_type', 'Content-Type', 'application/xml', 'application/xml', 5, 'XML 请求体', 1, NOW(), NOW());
