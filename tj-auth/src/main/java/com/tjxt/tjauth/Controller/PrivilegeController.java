package com.tjxt.tjauth.Controller;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tjxt.tjauth.Entity.Privilege;
import com.tjxt.tjauth.Model.Dto.PrivilegeDTO;
import com.tjxt.tjauth.Model.Vo.PrivilegeOptionVO;
import com.tjxt.tjauth.Service.IPrivilegeService;
import com.tjxt.tjcommon.Model.Dto.PageDTO;
import com.tjxt.tjcommon.Model.Response.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * API 权限管理相关接口。
 * 职责：
 *   - 分页查询权限，供管理端权限列表页使用
 *   - 按菜单查询可勾选权限，并标记某角色已勾选状态
 *   - 新增 / 修改 / 删除权限
 *   - 绑定与解绑角色-权限关系
 * 说明：
 *   - 权限（Privilege）指访问路径权限，通过 menuId 归属于某个菜单
 *   - 查询下拉选项时过滤 internal = false，内置权限不对外暴露
 *   - 权限控制使用 Spring Security 的方法级注解 @PreAuthorize，权限标识形如 privilege:xxx
 */
@RestController
@RequestMapping("/privileges")
@Tag(name = "权限管理接口")
@RequiredArgsConstructor
public class PrivilegeController {

    private final IPrivilegeService privilegesService;

    /**
     * 分页查询所有权限。
     */
    @Operation(summary = "分页查询所有权限")
    @PreAuthorize("hasAuthority('privilege:view')")
    @GetMapping
    public PageDTO<PrivilegeDTO> listAllPrivileges(PageQuery pageQuery) {
        Page<Privilege> page = privilegesService.listPrivilegesByPage(pageQuery);
        List<Privilege> list = page.getRecords();
        if (CollectionUtil.isEmpty(list)) {
            return new PageDTO<>(page.getTotal(), page.getPages(), Collections.emptyList());
        }
        List<PrivilegeDTO> dtoList = list.stream()
                .map(Privilege::toDTO)
                .collect(Collectors.toList());
        return new PageDTO<>(page.getTotal(), page.getPages(), dtoList);
    }

    /**
     * 查询指定菜单下可勾选的权限项，用作下拉选框数据源。
     */
    @Operation(summary = "查询菜单下的所有权限，作为下拉选框菜单")
    @PreAuthorize("hasAuthority('privilege:view')")
    @GetMapping("options/{menuId}")
    public List<PrivilegeOptionVO> listAllPrivilegesOptionsByMenuId(
            @Parameter(description = "菜单id", example = "1")
            @PathVariable("menuId") Long menuId) {
        List<Privilege> list = privilegesService.lambdaQuery()
                .eq(Privilege::getMenuId, menuId)
                .eq(Privilege::getInternal, false)
                .list();
        if (CollectionUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(PrivilegeOptionVO::new)
                .collect(Collectors.toList());
    }

    /**
     * 查询指定菜单下权限项，并标记指定角色是否已勾选。
     */
    @Operation(summary = "查询菜单下的权限列表，某个角色的权限")
    @PreAuthorize("hasAuthority('privilege:view')")
    @GetMapping("/roles/{roleId}/{menuId}")
    public List<PrivilegeOptionVO> listPrivilegeByRoleId(
            @Parameter(description = "角色id", required = true, example = "1")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "菜单id", required = true, example = "1")
            @PathVariable("menuId") Long menuId) {
        Set<Long> privilegeIds = privilegesService.listPrivilegeByRoleId(roleId);
        if (CollectionUtil.isEmpty(privilegeIds)) {
            return Collections.emptyList();
        }
        List<PrivilegeOptionVO> vos = listAllPrivilegesOptionsByMenuId(menuId);
        for (PrivilegeOptionVO vo : vos) {
            vo.setChecked(privilegeIds.contains(vo.getId()));
        }
        return vos;
    }

    /**
     * 新增权限。
     * * 注意：@PostMapping 未指定路径，映射为 POST /privileges，而非方法名。
     */
    @Operation(summary = "新增权限")
    @PreAuthorize("hasAuthority('privilege:add')")
    @PostMapping
    public PrivilegeDTO savePrivilege(@Valid @RequestBody PrivilegeDTO privilegeDTO) {
        Privilege privilege = new Privilege(privilegeDTO);
        privilegesService.savePrivilege(privilege);
        return privilege.toDTO();
    }

    /**
     * 根据主键修改权限。
     */
    @Operation(summary = "修改权限")
    @PreAuthorize("hasAuthority('privilege:edit')")
    @PutMapping("{id}")
    public PrivilegeDTO updatePrivilege(
            @Valid @RequestBody PrivilegeDTO privilegeDTO,
            @Parameter(description = "要修改的权限id", required = true, example = "1")
            @PathVariable("id") Long id) {
        Privilege privilege = new Privilege(privilegeDTO);
        privilege.setId(id);
        privilegesService.updateById(privilege);
        return privilege.toDTO();
    }

    /**
     * 根据主键删除权限。
     */
    @Operation(summary = "删除权限")
    @PreAuthorize("hasAuthority('privilege:delete')")
    @DeleteMapping("{id}")
    public void removePrivilegeById(
            @Parameter(description = "要删除的权限id", required = true, example = "1")
            @PathVariable("id") Long id) {
        privilegesService.removePrivilegeById(id);
    }

    /**
     * 绑定角色与 API 权限。
     */
    @Operation(summary = "绑定角色与API权限")
    @PreAuthorize("hasAuthority('privilege:bind')")
    @PostMapping("/role/{roleId}")
    public void bindRolePrivileges(
            @Parameter(description = "角色id", example = "1")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "API权限的id集合")
            @RequestBody List<Long> privilegeIds) {
        privilegesService.bindRolePrivileges(roleId, privilegeIds);
    }

    /**
     * 解除角色与 API 权限的绑定关系。
     */
    @Operation(summary = "解除角色的API权限")
    @PreAuthorize("hasAuthority('privilege:unbind')")
    @DeleteMapping("/role/{roleId}")
    public void deleteRolePrivileges(
            @Parameter(description = "角色id", example = "1")
            @PathVariable("roleId") Long roleId,
            @Parameter(description = "API权限的id集合")
            @RequestBody List<Long> privilegeIds) {
        privilegesService.deleteRolePrivileges(roleId, privilegeIds);
    }
}