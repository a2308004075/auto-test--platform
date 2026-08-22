/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 菜单管理服务
 */
package com.platform.sys.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.sys.dto.MenuCreateRequest;
import com.platform.sys.dto.MenuImportResult;
import com.platform.sys.dto.MenuListItem;
import com.platform.sys.dto.MenuTreeNode;
import com.platform.sys.entity.Menu;
import com.platform.sys.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuMapper menuMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取菜单树（仅启用状态的菜单）
     */
    public List<MenuTreeNode> tree() {
        return tree(null);
    }

    /**
     * 获取权限过滤后的菜单树
     *
     * <p>根据用户权限编码列表过滤菜单：
     * <ul>
     *     <li>permissionCodes 为 null 时返回全部启用菜单（向后兼容）</li>
     *     <li>permissionCodes 包含 "*" 时返回全部菜单（ADMIN 通配）</li>
     *     <li>permission_code 为 NULL 的菜单对所有已认证用户可见</li>
     *     <li>目录类型无可见子项时自动隐藏</li>
     * </ul>
     *
     * @param permissionCodes 用户权限编码列表，null 表示不过滤
     */
    public List<MenuTreeNode> tree(List<String> permissionCodes) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getIsActive, 1)
                .orderByAsc(Menu::getSortNo)
                .orderByAsc(Menu::getId);
        List<Menu> menus = menuMapper.selectList(wrapper);
        List<MenuTreeNode> nodes = menus.stream().map(this::toTreeNode).collect(Collectors.toList());

        if (permissionCodes == null) {
            return buildTree(nodes, 0L);
        }

        // 权限过滤：先构建完整树，再自底向上剪枝空目录
        Set<String> permSet = new HashSet<>(permissionCodes);
        boolean isAdmin = permSet.contains("*");
        List<MenuTreeNode> fullTree = buildTree(nodes, 0L);
        return pruneInvisibleBranches(fullTree, permSet, isAdmin);
    }

    /**
     * 获取所有菜单（扁平列表，含停用，供管理页面使用）
     */
    public List<MenuListItem> listAll() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getSortNo)
                .orderByAsc(Menu::getId);
        List<Menu> menus = menuMapper.selectList(wrapper);
        return menus.stream().map(this::toListItem).collect(Collectors.toList());
    }

    /**
     * 获取单个菜单
     */
    public MenuListItem get(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND, "菜单不存在");
        }
        return toListItem(menu);
    }

    /**
     * 新增菜单
     */
    @Transactional(rollbackFor = Exception.class)
    public MenuListItem add(MenuCreateRequest request) {
        Menu menu = new Menu();
        BeanUtils.copyProperties(request, menu);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getSortNo() == null) {
            menu.setSortNo(0);
        }
        menu.setIsActive(1);
        menuMapper.insert(menu);
        return toListItem(menu);
    }

    /**
     * 更新菜单
     */
    @Transactional(rollbackFor = Exception.class)
    public MenuListItem update(Long id, MenuCreateRequest request) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND, "菜单不存在");
        }
        BeanUtils.copyProperties(request, menu);
        menuMapper.updateById(menu);
        return toListItem(menu);
    }

    /**
     * 删除菜单（软删除）
     * 如果菜单有子菜单，则级联删除子菜单
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND, "菜单不存在");
        }
        // 级联删除子菜单
        List<Long> childIds = findChildIds(id);
        if (!childIds.isEmpty()) {
            menuMapper.deleteBatchIds(childIds);
        }
        menuMapper.deleteById(id);
    }

    /**
     * 切换菜单启用/停用状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND, "菜单不存在");
        }
        menu.setIsActive(menu.getIsActive() == 1 ? 0 : 1);
        menuMapper.updateById(menu);
    }

    // ===== Excel 导入导出 =====

    /**
     * 导出菜单列表到 Excel
     *
     * <p>导出所有启用状态的菜单（含顶级目录与子菜单），Excel 通过「上级菜单名称」列表达父子层级。
     */
    public void exportMenus(HttpServletResponse response) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getSortNo)
                .orderByAsc(Menu::getId);
        List<Menu> menus = menuMapper.selectList(wrapper);

        // 构建 id → name 映射，用于导出「上级菜单名称」列
        Map<Long, String> idToName = new HashMap<>();
        for (Menu menu : menus) {
            idToName.put(menu.getId(), menu.getName());
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode("菜单列表.xlsx", "UTF-8"));
            OutputStream out = response.getOutputStream();
            ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter(true);
            // 表头
            writer.writeCellValue(0, 0, "菜单名称");
            writer.writeCellValue(1, 0, "上级菜单名称");
            writer.writeCellValue(2, 0, "菜单类型");
            writer.writeCellValue(3, 0, "图标");
            writer.writeCellValue(4, 0, "路由路径");
            writer.writeCellValue(5, 0, "组件路径");
            writer.writeCellValue(6, 0, "权限编码");
            writer.writeCellValue(7, 0, "排序号");
            // 数据行
            int rowIndex = 1;
            for (Menu menu : menus) {
                String parentName = "";
                if (menu.getParentId() != null && menu.getParentId() > 0) {
                    parentName = idToName.getOrDefault(menu.getParentId(), "");
                }
                writer.writeCellValue(0, rowIndex, menu.getName() != null ? menu.getName() : "");
                writer.writeCellValue(1, rowIndex, parentName);
                writer.writeCellValue(2, rowIndex, menu.getMenuType() != null ? menu.getMenuType() : "");
                writer.writeCellValue(3, rowIndex, menu.getIcon() != null ? menu.getIcon() : "");
                writer.writeCellValue(4, rowIndex, menu.getRoutePath() != null ? menu.getRoutePath() : "");
                writer.writeCellValue(5, rowIndex, menu.getComponent() != null ? menu.getComponent() : "");
                writer.writeCellValue(6, rowIndex, menu.getPermissionCode() != null ? menu.getPermissionCode() : "");
                writer.writeCellValue(7, rowIndex, menu.getSortNo() != null ? menu.getSortNo() : 0);
                rowIndex++;
            }
            writer.flush(out, true);
            writer.close();
        } catch (IOException e) {
            log.error("导出菜单 Excel 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 从 Excel 导入菜单（增量更新）
     *
     * <p>导入策略：
     * <ul>
     *     <li>按「上级菜单名称」确定父级：为空表示顶级菜单（parent_id=0）</li>
     *     <li>按 (parentId, name) 匹配：已存在则更新，不存在则新增</li>
     *     <li>两轮处理：先处理顶级菜单，再处理子菜单，确保父级先就绪</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public MenuImportResult importMenus(MultipartFile file) {
        MenuImportResult result = new MenuImportResult();
        if (file == null || file.isEmpty()) {
            result.getErrors().add("文件为空");
            return result;
        }

        try {
            ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(file.getInputStream());
            List<List<Object>> rows = reader.read();
            reader.close();

            if (rows.size() <= 1) {
                result.getErrors().add("Excel 无数据行");
                return result;
            }

            // 顶级菜单名称 → ID 映射（包含数据库已有与本批次新增的）
            Map<String, Long> topMenuNameToId = new HashMap<>();
            // 预加载已有顶级菜单
            LambdaQueryWrapper<Menu> topWrapper = new LambdaQueryWrapper<>();
            topWrapper.eq(Menu::getParentId, 0);
            List<Menu> existingTopMenus = menuMapper.selectList(topWrapper);
            for (Menu top : existingTopMenus) {
                topMenuNameToId.put(top.getName(), top.getId());
            }

            // 收集数据行并区分顶级 / 子级
            List<List<Object>> topRows = new ArrayList<>();
            List<List<Object>> childRows = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }
                String parentName = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                if (parentName.isEmpty()) {
                    topRows.add(row);
                } else {
                    childRows.add(row);
                }
            }

            // 第一轮：处理顶级菜单
            for (List<Object> row : topRows) {
                try {
                    String name = String.valueOf(row.get(0)).trim();
                    Long menuId = importSingleMenu(row, 0L, result);
                    if (menuId != null) {
                        topMenuNameToId.put(name, menuId);
                    }
                } catch (Exception e) {
                    result.getErrors().add("顶级菜单行: " + e.getMessage());
                    result.setFailCount(result.getFailCount() + 1);
                }
            }

            // 第二轮：处理子菜单
            for (List<Object> row : childRows) {
                try {
                    String parentName = String.valueOf(row.get(1)).trim();
                    Long parentId = topMenuNameToId.get(parentName);
                    if (parentId == null) {
                        result.getErrors().add("菜单「" + String.valueOf(row.get(0)).trim()
                                + "」的上级菜单「" + parentName + "」不存在");
                        result.setFailCount(result.getFailCount() + 1);
                        continue;
                    }
                    importSingleMenu(row, parentId, result);
                } catch (Exception e) {
                    result.getErrors().add("子菜单行: " + e.getMessage());
                    result.setFailCount(result.getFailCount() + 1);
                }
            }
        } catch (IOException e) {
            log.error("导入菜单 Excel 失败", e);
            throw new BusinessException(ErrorCode.EXCEL_IMPORT_FAILED, "文件读取失败: " + e.getMessage());
        }

        log.info("菜单导入完成: 成功={}, 失败={}", result.getSuccessCount(), result.getFailCount());
        return result;
    }

    /**
     * 导入单行菜单（增量更新：按 parentId + name 匹配）
     *
     * @return 新增或更新后的菜单 ID，校验失败时返回 null
     */
    private Long importSingleMenu(List<Object> row, Long parentId, MenuImportResult result) {
        String name = row.size() > 0 ? String.valueOf(row.get(0)).trim() : "";
        if (name.isEmpty()) {
            result.getErrors().add("菜单名称不能为空");
            result.setFailCount(result.getFailCount() + 1);
            return null;
        }

        int menuType = row.size() > 2 ? parseIntSafe(row.get(2)) : 1;
        if (menuType < 1 || menuType > 3) {
            result.getErrors().add("菜单「" + name + "」的类型必须为 1(目录)/2(菜单)/3(按钮)");
            result.setFailCount(result.getFailCount() + 1);
            return null;
        }
        String icon = row.size() > 3 ? String.valueOf(row.get(3)).trim() : "";
        String routePath = row.size() > 4 ? String.valueOf(row.get(4)).trim() : "";
        String component = row.size() > 5 ? String.valueOf(row.get(5)).trim() : "";
        String permissionCode = row.size() > 6 ? String.valueOf(row.get(6)).trim() : "";
        int sortNo = row.size() > 7 ? parseIntSafe(row.get(7)) : 0;

        // 按 (parentId, name) 查找是否已存在
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getParentId, parentId)
                .eq(Menu::getName, name);
        Menu existing = menuMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setMenuType(menuType);
            existing.setIcon(icon.isEmpty() ? null : icon);
            existing.setRoutePath(routePath.isEmpty() ? null : routePath);
            existing.setComponent(component.isEmpty() ? null : component);
            existing.setPermissionCode(permissionCode.isEmpty() ? null : permissionCode);
            existing.setSortNo(sortNo);
            menuMapper.updateById(existing);
            result.setSuccessCount(result.getSuccessCount() + 1);
            return existing.getId();
        } else {
            Menu menu = new Menu();
            menu.setParentId(parentId);
            menu.setName(name);
            menu.setMenuType(menuType);
            menu.setIcon(icon.isEmpty() ? null : icon);
            menu.setRoutePath(routePath.isEmpty() ? null : routePath);
            menu.setComponent(component.isEmpty() ? null : component);
            menu.setPermissionCode(permissionCode.isEmpty() ? null : permissionCode);
            menu.setSortNo(sortNo);
            menu.setIsActive(1);
            menuMapper.insert(menu);
            result.setSuccessCount(result.getSuccessCount() + 1);
            return menu.getId();
        }
    }

    // ===== 私有方法 =====

    private int parseIntSafe(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<Long> findChildIds(Long parentId) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getParentId, parentId).select(Menu::getId);
        List<Menu> children = menuMapper.selectList(wrapper);
        List<Long> ids = new ArrayList<>();
        for (Menu child : children) {
            ids.add(child.getId());
            ids.addAll(findChildIds(child.getId()));
        }
        return ids;
    }

    private List<MenuTreeNode> buildTree(List<MenuTreeNode> nodes, Long parentId) {
        Map<Long, List<MenuTreeNode>> grouped = nodes.stream()
                .collect(Collectors.groupingBy(MenuTreeNode::getParentId));
        return buildChildren(grouped, parentId);
    }

    private List<MenuTreeNode> buildChildren(Map<Long, List<MenuTreeNode>> grouped, Long parentId) {
        List<MenuTreeNode> children = grouped.getOrDefault(parentId, new ArrayList<>());
        // 显式按 sortNo → id 升序排序，确保菜单显示顺序与数据库 sort_no 一致
        children.sort(Comparator.comparingInt((MenuTreeNode n) -> n.getSortNo() == null ? 0 : n.getSortNo())
                .thenComparingLong(MenuTreeNode::getId));
        for (MenuTreeNode child : children) {
            child.setChildren(buildChildren(grouped, child.getId()));
        }
        return children;
    }

    /**
     * 自底向上剪枝：移除无权限且无可见子项的目录节点
     *
     * @return true 表示该节点可见（应保留），false 表示应剪枝
     */
    private List<MenuTreeNode> pruneInvisibleBranches(List<MenuTreeNode> nodes,
                                                       Set<String> permSet,
                                                       boolean isAdmin) {
        List<MenuTreeNode> result = new ArrayList<>();
        for (MenuTreeNode node : nodes) {
            // 递归处理子节点
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                node.setChildren(pruneInvisibleBranches(node.getChildren(), permSet, isAdmin));
            }
            // 判断当前节点是否可见
            boolean visible = isNodeVisible(node, permSet, isAdmin);
            // 目录类型：自身可见 + 至少有一个可见子项才保留
            if (node.getMenuType() != null && node.getMenuType() == 1) {
                if (visible && node.getChildren() != null && !node.getChildren().isEmpty()) {
                    result.add(node);
                }
            } else if (visible) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * 判断单个菜单节点是否对当前用户可见
     */
    private boolean isNodeVisible(MenuTreeNode node, Set<String> permSet, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        String permCode = node.getPermissionCode();
        // 未关联权限编码的菜单对所有已认证用户可见
        if (permCode == null || permCode.isEmpty()) {
            return true;
        }
        return permSet.contains(permCode);
    }

    private MenuTreeNode toTreeNode(Menu menu) {
        MenuTreeNode node = new MenuTreeNode();
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setName(menu.getName());
        node.setMenuType(menu.getMenuType());
        node.setIcon(menu.getIcon());
        node.setRoutePath(menu.getRoutePath());
        node.setComponent(menu.getComponent());
        node.setSortNo(menu.getSortNo());
        node.setIsActive(menu.getIsActive());
        node.setPermissionCode(menu.getPermissionCode());
        return node;
    }

    private MenuListItem toListItem(Menu menu) {
        MenuListItem item = new MenuListItem();
        item.setId(menu.getId());
        item.setParentId(menu.getParentId());
        item.setName(menu.getName());
        item.setMenuType(menu.getMenuType());
        item.setIcon(menu.getIcon());
        item.setRoutePath(menu.getRoutePath());
        item.setComponent(menu.getComponent());
        item.setSortNo(menu.getSortNo());
        item.setIsActive(menu.getIsActive());
        item.setPermissionCode(menu.getPermissionCode());
        if (menu.getCreatedAt() != null) {
            item.setCreatedAt(menu.getCreatedAt().format(DT_FMT));
        }
        if (menu.getUpdatedAt() != null) {
            item.setUpdatedAt(menu.getUpdatedAt().format(DT_FMT));
        }
        return item;
    }
}
