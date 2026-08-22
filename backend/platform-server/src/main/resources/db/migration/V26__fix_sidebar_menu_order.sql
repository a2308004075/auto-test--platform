-- ============================================================
-- V26 菜单排序修正：仪表板应排在测试管理目录前面
-- 当前：测试管理 sort_no=0，仪表板 sort_no=1（顺序反了）
-- 修正：仪表板 sort_no=0，测试管理 sort_no=1
-- ============================================================

SET NAMES utf8mb4;

-- 仪表板（id=11）排序号改为 0（排最前）
UPDATE `sys_menu` SET `sort_no` = 0 WHERE `id` = 11;

-- 测试管理目录排序号改为 1（排在仪表板之后）
UPDATE `sys_menu` SET `sort_no` = 1 WHERE `name` = '测试管理' AND `parent_id` = 10 AND `menu_type` = 1;
