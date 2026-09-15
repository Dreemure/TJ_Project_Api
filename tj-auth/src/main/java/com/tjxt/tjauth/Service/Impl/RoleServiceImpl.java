package com.tjxt.tjauth.Service.Impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tjxt.tjauth.Constants.AuthConstants;
import com.tjxt.tjauth.Entity.Role;
import com.tjxt.tjauth.Mapper.RoleMapper;
import com.tjxt.tjauth.Service.IRoleMenuService;
import com.tjxt.tjauth.Service.IRolePrivilegeService;
import com.tjxt.tjauth.Service.IRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * 角色服务实现。
 * 职责：
 *   1. 角色 CRUD
 *   2. 删除角色时，级联清理 role_menu / role_privilege 关联
 *   3. 角色变化后，触发权限缓存刷新（写 Redis + 版本号 +1）
 * 说明：
 *   - 权限缓存由 AuthUtils 定时拉取，本类只负责"通知"（version+1）
 *   - 超级管理员角色（ADMIN_ROLE_ID）不允许删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl
        extends ServiceImpl<RoleMapper, Role>
        implements IRoleService {

    private final IRoleMenuService roleMenuService;
    private final IRolePrivilegeService rolePrivilegeService;
    private final StringRedisTemplate stringRedisTemplate;

    // ==================== 存在性判断 ====================

    @Override
    public boolean exists(Long roleId) {
        long count = lambdaQuery().eq(Role::getId, roleId).count();
        return count > 0;
    }

    @Override
    public boolean exists(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        long count = lambdaQuery().in(Role::getId, roleIds).count();
        // ⚠️ 原来写的是 count != roleIds.size()，逻辑反了
        return count == roleIds.size();
    }

    // ==================== 删除角色 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        // 1. 不允许删除超级管理员
        if (AuthConstants.ADMIN_ROLE_ID.equals(id)) {
            throw new RuntimeException("超级管理员角色不允许删除");
        }

        // 2. 删除角色
        removeById(id);

        // 3. 删除关联（角色-菜单、角色-权限）
        roleMenuService.removeByRoleId(id);
        rolePrivilegeService.removeByRoleId(id);

        // 4. 触发权限缓存刷新（写 Redis 版本号 +1）
        refreshPrivilegeCache();
    }

    // ==================== 缓存刷新 ====================

    /**
     * 通知权限缓存失效。
     * <p>只做一件事：version + 1。
     * <p>AuthUtils 的 @Scheduled 检测到版本变化后，会主动从 Redis 拉取最新数据。
     */
    private void refreshPrivilegeCache() {
        stringRedisTemplate.opsForValue().increment(AuthConstants.AUTH_PRIVILEGE_VERSION_KEY);
        log.info("权限缓存版本号已更新");
    }
}