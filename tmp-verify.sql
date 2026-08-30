SELECT id, HEX(name) AS menu82_hex FROM sys_menu WHERE id = 82;
SELECT permission_code, HEX(permission_name) AS perm_hex FROM permission WHERE permission_code = 'project:repositories';
SELECT version, success FROM flyway_schema_history WHERE version = '18';
