package com.tjxt.tjauth.Controller;

import cn.hutool.core.collection.CollectionUtil;
import com.tjxt.tjauth.Entity.Menu;
import com.tjxt.tjauth.Model.Dto.MenuDTO;
import com.tjxt.tjauth.Model.Vo.MenuOptionVO;
import com.tjxt.tjauth.Service.IMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * 菜单管理相关接口。
 * 职责：
 *   - 查询菜单（按父 id、按主键、整树、当前用户可见树）
 *   - 新增 / 更新 / 删除菜单
 *   - 绑定与解绑角色-菜单关系
 * 说明：
 *   - 整树查询以 parentId = 0 为根，逐层组装并按 priority 升序排序
 *   - 权限控制使用 Spring Security 的方法级注解 @PreAuthorize，权限标识形如 menu:xxx
 *   - 当前用户菜单树仅依赖 SecurityContext 中的身份，无需额外参数
 */
@RestController
@RequestMapping("/v2/menus")
@Tag(name = "菜单管理")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuService menuService;

    /**
     * 根据父菜单 id 查询其直接子菜单。
     */
    @Operation(summary = "根据父菜单id查询子菜单")
    @PreAuthorize("hasAuthority('menu:view')")
    @GetMapping("/parent/{pid}")
    public List<MenuOptionVO> listMenusByParent(
            @Parameter(description = "父菜单id，传0查询一级菜单", example = "0")
            @PathVariable("pid") Long pid) {
        List<Menu> list = menuService.lambdaQuery()
                .eq(Menu::getParentId, pid)
                .list();
        if (CollectionUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(MenuOptionVO::new)
                .collect(Collectors.toList());
    }

    /**
     * 根据主键查询单个菜单。
     */
    @Operation(summary = "根据id查询菜单")
    @PreAuthorize("hasAuthority('menu:view')")
    @GetMapping("{id}")
    public MenuOptionVO getMenuById(
            @Parameter(description = "菜单id", example = "1")
            @PathVariable("id") Long id) {
        Menu menu = menuService.getById(id);
        return menu == null ? null : new MenuOptionVO(menu);
    }

    /**
     * 查询全部菜单并组装为多级树结构。
     */
    @Operation(summary = "查询菜单，按照多级菜单组成树结构")
    @PreAuthorize("hasAuthority('menu:view')")
    @GetMapping
    public List<MenuOptionVO> listMenuTree() {
        List<Menu> menus = menuService.list();
        return convert2MenuDTOs(menus);
    }

    /**
     * 查询当前登录用户可见的菜单树。
     */
    @Operation(summary = "查询我的菜单，按照多级菜单组成树结构")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("me")
    public List<MenuOptionVO> listMenuTreeByUser() {
        List<Menu> menus = menuService.listMenuByUser();
        return convert2MenuDTOs(menus);
    }

    /**
     * 新增菜单。
     */
    @Operation(summary = "新增菜单")
    @PreAuthorize("hasAuthority('menu:add')")
    @PostMapping
    public void saveMenu(@RequestBody @Valid MenuDTO menuDTO) {
        Menu menu = new Menu(menuDTO);
        menuService.saveMenu(menu);
    }

    /**
     * 根据主键更新菜单。
     */
    @Operation(summary = "更新菜单")
    @PreAuthorize("hasAuthority('menu:edit')")
    @PutMapping("{id}")
    public void updateMenu(
            @RequestBody @Valid MenuDTO menuDTO,
            @Parameter(description = "菜单id", example = "1")
            @PathVariable("id") Long id) {
        menuDTO.setId(id);
        menuService.updateById(new Menu(menuDTO));
    }

    /**
     * 根据主键删除菜单。
     */
    @Operation(summary = "根据id删除菜单")
    @PreAuthorize("hasAuthority('menu:delete')")
    @DeleteMapping("{id}")
    public void deleteMenu(
            @Parameter(description = "菜单id", example = "1")
            @PathVariable("id") Long id) {
        menuService.deleteMenu(id);
    }

    /**
     * 绑定角色与菜单权限。
     */
    @Operation(summary = "绑定角色与菜单权限")
    @PreAuthorize("hasAuthority('role:bind')")
    @PostMapping("/role/{roleId}")
    public void bindRoleMenus(
            @Parameter(description = "角色id", example = "1")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "菜单id集合")
            @RequestBody List<Long> menuIds) {
        menuService.bindRoleMenus(roleId, menuIds);
    }

    /**
     * 解除角色与菜单的绑定关系。
     */
    @Operation(summary = "解除角色的菜单权限")
    @PreAuthorize("hasAuthority('role:unbind')")
    @DeleteMapping("/role/{roleId}")
    public void deleteRoleMenus(
            @Parameter(description = "角色id", example = "1")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "菜单id集合")
            @RequestBody List<Long> menuIds) {
        menuService.deleteRoleMenus(roleId, menuIds);
    }

    /**
     * 将菜单集合按 parentId 分组并组装为两级树，各层级按 priority 升序排序。
     */
    private List<MenuOptionVO> convert2MenuDTOs(List<Menu> menus) {
        if (CollectionUtil.isEmpty(menus)) {
            return Collections.emptyList();
        }
        Map<Long, List<MenuOptionVO>> menuMap = menus.stream()
                .map(MenuOptionVO::new)
                .collect(Collectors.groupingBy(MenuOptionVO::getParentId));

        List<MenuOptionVO> parents = menuMap.get(0L);
        if (CollectionUtil.isEmpty(parents)) {
            return Collections.emptyList();
        }

        for (MenuOptionVO parent : parents) {
            List<MenuOptionVO> subMenus = menuMap.get(parent.getId());
            if (CollectionUtil.isNotEmpty(subMenus)) {
                subMenus.sort(Comparator.comparingInt(MenuOptionVO::getPriority));
                parent.setSubMenus(subMenus);
            }
        }
        parents.sort(Comparator.comparingInt(MenuOptionVO::getPriority));
        return parents;
    }
}