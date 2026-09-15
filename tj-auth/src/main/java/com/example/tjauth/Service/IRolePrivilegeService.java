package com.example.tjauth.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.tjauth.Entity.RolePrivilege;

import java.util.List;

/**
 * <p>
 * 账户、角色关联表 服务类
 * </p>
 */
public interface IRolePrivilegeService extends IService<RolePrivilege> {

    void removeByPrivilegeId(Long id);

    void removeByRoleId(Long id);

    void deleteRolePrivileges(Long roleId, List<Long> privilegeIds);
}
