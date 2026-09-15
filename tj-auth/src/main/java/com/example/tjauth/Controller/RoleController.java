package com.example.tjauth.Controller;

import cn.hutool.core.collection.CollectionUtil;
import com.example.tjauth.Entity.Role;
import com.example.tjauth.Service.IRoleService;
import com.example.tjmicroservice.Model.Dto.Auth.RoleDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/*
 * 角色管理相关接口。
 * 职责：
 *   - 查询全部角色，供员工分配角色时使用
 *   - 查询自定义角色列表（不含内置角色）
 *   - 按主键查询角色
 *   - 新增 / 修改 / 删除角色
 * 说明：
 *   - 新增角色时强制将 type 设为 CUSTOM，避免误创建内置角色
 *   - 查询列表接口在无数据时返回空集合而非 null，便于前端直接遍历
 *   - 权限控制使用 Spring Security 的方法级注解 @PreAuthorize，权限标识形如 role:xxx
 */
@RestController
@RequestMapping("/roles")
@Tag(name = "角色管理")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService roleService;

    /**
     * 查询全部角色，包含内置角色与自定义角色。
     * 注：@GetMapping 指定了 "/list"，映射为 GET /roles/list。
     */
    @Operation(summary = "查询员工角色列表")
    @PreAuthorize("hasAuthority('role:view')")
    @GetMapping("/list")
    public List<RoleDTO> listAllRoles() {
        List<Role> list = roleService.list();
        if (CollectionUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(Role::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询自定义角色列表，用于员工分配角色。
     * 注：@GetMapping 未指定路径时，映射为类级路径 GET /roles，而非方法名。
     */
    @Operation(summary = "查询员工角色列表")
    @PreAuthorize("hasAuthority('role:view')")
    @GetMapping
    public List<RoleDTO> listStaffRoles() {
        List<Role> list = roleService.lambdaQuery()
                .eq(Role::getType, Role.RoleType.CUSTOM)
                .list();
        if (CollectionUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(Role::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据主键查询角色。
     */
    @Operation(summary = "根据id查询角色")
    @PreAuthorize("hasAuthority('role:view')")
    @GetMapping("/{id}")
    public RoleDTO queryRoleById(
            @Parameter(description = "角色id", example = "1")
            @PathVariable("id") Long id) {
        Role role = roleService.getById(id);
        return role == null ? null : role.toDTO();
    }

    /**
     * 新增自定义角色，type 强制为 CUSTOM。
     * 注：@PostMapping 未指定路径时，映射为类级路径 POST /roles，而非方法名。
     */
    @Operation(summary = "新增角色")
    @PreAuthorize("hasAuthority('role:add')")
    @PostMapping
    public RoleDTO saveRole(@Valid @RequestBody RoleDTO roleDTO) {
        Role role = new Role(roleDTO);
        role.setType(Role.RoleType.CUSTOM);
        roleService.save(role);
        roleDTO.setId(role.getId());
        return roleDTO;
    }

    /**
     * 根据主键修改角色信息。
     */
    @Operation(summary = "修改角色信息")
    @PreAuthorize("hasAuthority('role:edit')")
    @PutMapping("{id}")
    public void updateRole(
            @Valid @RequestBody RoleDTO roleDTO,
            @Parameter(description = "角色id", example = "1")
            @PathVariable("id") Long id) {
        Role role = new Role(roleDTO);
        role.setId(id);
        roleService.updateById(role);
    }

    /**
     * 根据主键删除角色。
     */
    @Operation(summary = "删除角色信息")
    @PreAuthorize("hasAuthority('role:delete')")
    @DeleteMapping("{id}")
    public void deleteRole(
            @Parameter(description = "角色id", example = "1")
            @PathVariable("id") Long id) {
        roleService.deleteRole(id);
    }
}