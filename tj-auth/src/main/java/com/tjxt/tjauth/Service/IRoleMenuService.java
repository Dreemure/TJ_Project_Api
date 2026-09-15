package com.tjxt.tjauth.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tjxt.tjauth.Entity.RoleMenu;

import java.util.List;

/**
 * <p>
 * 账户、角色关联表 服务类
 * </p>
 */
public interface IRoleMenuService extends IService<RoleMenu> {

    void removeByRoleId(Long id);

    void deleteRoleMenus(Long roleId, List<Long> menuIds);
}
