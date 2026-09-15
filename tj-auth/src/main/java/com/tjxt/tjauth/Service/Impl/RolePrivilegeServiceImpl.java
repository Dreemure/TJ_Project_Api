package com.tjxt.tjauth.Service.Impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tjxt.tjauth.Entity.RolePrivilege;
import com.tjxt.tjauth.Mapper.RolePrivilegeMapper;
import com.tjxt.tjauth.Service.IRolePrivilegeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 账户、角色关联表 服务实现类
 * </p>
 */
@Service
public class RolePrivilegeServiceImpl extends ServiceImpl<RolePrivilegeMapper, RolePrivilege> implements IRolePrivilegeService {

    @Override
    public void removeByPrivilegeId(Long id) {
        remove(new QueryWrapper<RolePrivilege>().lambda().eq(RolePrivilege::getPrivilegeId, id));
    }

    @Override
    public void removeByRoleId(Long id) {
        remove(new QueryWrapper<RolePrivilege>().lambda().eq(RolePrivilege::getRoleId, id));
    }

    @Override
    public void deleteRolePrivileges(Long roleId, List<Long> privilegeIds) {
        remove(
                new LambdaQueryWrapper<RolePrivilege>()
                        .eq(RolePrivilege::getRoleId, roleId)
                        .in(RolePrivilege::getPrivilegeId, privilegeIds)
        );
    }
}
