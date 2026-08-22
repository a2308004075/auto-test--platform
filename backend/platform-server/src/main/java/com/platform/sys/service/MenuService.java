/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 菜单管理服务
 */
package com.platform.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.sys.dto.MenuCreateRequest;
import com.platform.sys.dto.MenuListItem;
import com.platform.sys.dto.MenuTreeNode;
import com.platform.sys.entity.Menu;
import com.platform.sys.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getIsActive, 1)
                .orderByAsc(Menu::getSortNo)
                .orderByAsc(Menu::getId);
        List<Menu> menus = menuMapper.selectList(wrapper);
        List<MenuTreeNode> nodes = menus.stream().map(this::toTreeNode).collect(Collectors.toList());
        return buildTree(nodes, 0L);
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

    // ===== 私有方法 =====

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
        for (MenuTreeNode child : children) {
            child.setChildren(buildChildren(grouped, child.getId()));
        }
        return children;
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
        if (menu.getCreatedAt() != null) {
            item.setCreatedAt(menu.getCreatedAt().format(DT_FMT));
        }
        if (menu.getUpdatedAt() != null) {
            item.setUpdatedAt(menu.getUpdatedAt().format(DT_FMT));
        }
        return item;
    }
}
