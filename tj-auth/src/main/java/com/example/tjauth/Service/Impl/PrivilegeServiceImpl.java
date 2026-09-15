package com.example.tjauth.Service.Impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.tjauth.Constants.AuthConstants;
import com.example.tjauth.Entity.Privilege;
import com.example.tjauth.Entity.RolePrivilege;
import com.example.tjauth.Mapper.PrivilegeMapper;
import com.example.tjauth.Model.Dto.PrivilegeRoleDTO;
import com.example.tjauth.Service.IPrivilegeService;
import com.example.tjauth.Service.IRolePrivilegeService;
import com.example.tjauth.Service.IRoleService;
import com.example.tjcommon.Exceptions.CommonException;
import com.example.tjcommon.Model.Response.PageQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.tjauth.Constants.AuthConstants.AUTH_PRIVILEGE_KEY;
import static com.example.tjauth.Constants.AuthConstants.AUTH_PRIVILEGE_VERSION_KEY;
import static com.example.tjauth.Constants.AuthErrorInfo.Msg.*;

/*
 * 权限服务实现。
 * 职责：
 *   1. 权限的 CRUD
 *   2. 角色-权限绑定的维护
 *   3. 权限数据变化后，同步到 Redis（供 AuthUtils 定时拉取）
 * 说明：
 *   - 权限数据存在 Redis Hash（auth:privileges）
 *   - 每次变化后 version + 1，AuthUtils 检测到版本变化自动刷新本地缓存
 *   - 超级管理员（ADMIN_ROLE_ID）自动拥有所有权限
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl
        extends ServiceImpl<PrivilegeMapper, Privilege>
        implements IPrivilegeService {

    private final IRolePrivilegeService rolePrivilegeService;
    private final IRoleService roleService;
    private final StringRedisTemplate stringRedisTemplate;

    // ==================== 分页查询 ====================

    @Override
    public Page<Privilege> listPrivilegesByPage(PageQuery pageQuery) {
        // 1. 构造 QueryWrapper（支持字符串列名）
        QueryWrapper<Privilege> wrapper = new QueryWrapper<>();
        // 2. 排序（sortBy 不为空时才加）
        if (StringUtils.hasText(pageQuery.getSortBy())) {
            wrapper.orderBy(true, pageQuery.getIsAsc(), pageQuery.getSortBy());
        }
        // 3. 分页
        return page(new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize()), wrapper);
    }

    // ==================== 新增权限 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePrivilege(Privilege p) {
        p.setMethod(p.getMethod().toUpperCase());

        // 1. 判断是否重复
        long count = lambdaQuery()
                .eq(Privilege::getMethod, p.getMethod())
                .eq(Privilege::getUri, p.getUri())
                .count();
        if (count > 0) {
            throw new CommonException(PRIVILEGE_EXISTS);
        }

        // 2. 新增权限数据
        save(p);

        // 3. 超级管理员自动拥有该权限
        RolePrivilege rolePrivilege = new RolePrivilege()
                .setPrivilegeId(p.getId())
                .setRoleId(AuthConstants.ADMIN_ROLE_ID);
        rolePrivilegeService.save(rolePrivilege);

        // 4. 刷新 Redis 缓存 + 版本号
        refreshPrivilegeCache();
    }

    // ==================== 删除权限 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePrivilegeById(Long id) {
        // 1. 删除权限
        removeById(id);
        // 2. 删除角色-权限关联
        rolePrivilegeService.removeByPrivilegeId(id);
        // 3. 刷新 Redis 缓存 + 版本号
        refreshPrivilegeCache();
    }

    // ==================== 查询权限对应的角色 ====================

    @Override
    public List<PrivilegeRoleDTO> listPrivilegeRoles() {
        // 1. 查所有权限
        List<Privilege> privileges = list();
        if (CollectionUtil.isEmpty(privileges)) {
            return Collections.emptyList();
        }

        // 2. 查所有角色-权限关联
        List<RolePrivilege> rpList = rolePrivilegeService.list();

        // 3. 按 privilegeId 分组
        Map<Long, List<RolePrivilege>> rpMap = rpList.stream()
                .collect(Collectors.groupingBy(RolePrivilege::getPrivilegeId));

        // 4. 组装 PrivilegeRoleDTO
        List<PrivilegeRoleDTO> result = new ArrayList<>(privileges.size());
        for (Privilege p : privileges) {
            Set<Long> roles = rpMap.getOrDefault(p.getId(), Collections.emptyList())
                    .stream()
                    .map(RolePrivilege::getRoleId)
                    .collect(Collectors.toSet());

            PrivilegeRoleDTO dto = new PrivilegeRoleDTO();
            dto.setId(p.getId());
            dto.setRoles(roles);
            dto.setAntPath(p.getMethod() + ":" + p.getUri());
            dto.setInternal(p.getInternal());
            result.add(dto);
        }
        return result;
    }

    // ==================== 查询角色拥有的权限 ====================

    @Override
    public Set<Long> listPrivilegeByRoleId(Long roleId) {
        List<RolePrivilege> rolePrivileges = rolePrivilegeService.lambdaQuery()
                .eq(RolePrivilege::getRoleId, roleId)
                .list();
        if (CollectionUtil.isEmpty(rolePrivileges)) {
            return Collections.emptySet();
        }
        return rolePrivileges.stream()
                .map(RolePrivilege::getPrivilegeId)
                .collect(Collectors.toSet());
    }

    // ==================== 绑定角色-权限 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindRolePrivileges(Long roleId, List<Long> privilegeIds) {
        // 1. 校验角色存在
        if (!roleService.exists(roleId)) {
            throw new CommonException(ROLE_NOT_FOUND);
        }
        // 2. 校验权限存在
        long privilegeCount = lambdaQuery().in(Privilege::getId, privilegeIds).count();
        if (privilegeCount != privilegeIds.size()) {
            throw new CommonException(PRIVILEGE_NOT_FOUND);
        }
        // 3. 组装并批量保存
        List<RolePrivilege> rolePrivileges = privilegeIds.stream()
                .map(pid -> new RolePrivilege(roleId, pid))
                .toList();
        rolePrivilegeService.saveBatch(rolePrivileges);

        // 4. 刷新 Redis 缓存 + 版本号
        refreshPrivilegeCache();
    }

    // ==================== 解绑角色-权限 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRolePrivileges(Long roleId, List<Long> privilegeIds) {
        // 1. 删除关联
        rolePrivilegeService.deleteRolePrivileges(roleId, privilegeIds);
        // 2. 刷新 Redis 缓存 + 版本号
        refreshPrivilegeCache();
    }

    // ==================== 缓存刷新（核心变化） ====================

    /**
     * 刷新 Redis 中的权限缓存。
     * <p>做两件事：
     *   1. 把最新的"权限 → 角色"数据写入 Redis Hash（auth:privileges）
     *   2. version + 1（AuthUtils 检测到版本变化后刷新本地缓存）
     * <p>说明：不用主动推送给各个 auth 实例，靠 AuthUtils 的 @Scheduled 定时拉取即可。
     */
    private void refreshPrivilegeCache() {
        // 1. 查询最新的权限数据
        List<PrivilegeRoleDTO> list = listPrivilegeRoles();

        // 2. 整体覆盖 Redis Hash
        Map<String, String> cacheMap = list.stream()
                .collect(Collectors.toMap(
                        PrivilegeRoleDTO::getAntPath,
                        JSON::toJSONString,
                        (a, b) -> a));
        if (cacheMap.isEmpty()) {
            stringRedisTemplate.delete(AUTH_PRIVILEGE_KEY);
        } else {
            stringRedisTemplate.delete(AUTH_PRIVILEGE_KEY);
            stringRedisTemplate.opsForHash().putAll(AUTH_PRIVILEGE_KEY, cacheMap);
        }

        // 3. version + 1
        stringRedisTemplate.opsForValue().increment(AUTH_PRIVILEGE_VERSION_KEY);

        log.info("权限缓存已更新，共 {} 条", cacheMap.size());
    }
}