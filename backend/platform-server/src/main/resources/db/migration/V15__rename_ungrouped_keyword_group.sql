-- 修正系统默认分组名称：未分类 -> 未分组
UPDATE `api_keyword_group`
SET `name` = '未分组', `description` = '未分组的接口关键字'
WHERE `is_system` = 1 AND `name` = '未分类';
