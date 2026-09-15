package com.tjxt.tjauth.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tjxt.tjauth.Entity.Role;

import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 */
public interface IRoleService extends IService<Role> {

    boolean exists(Long roleId);
    boolean exists(List<Long> roleIds);

    void deleteRole(Long id);
}
