package com.example.tjauth.Service.Impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.tjauth.Constants.AuthConstants;
import com.example.tjauth.Entity.AccountRole;
import com.example.tjauth.Entity.Menu;
import com.example.tjauth.Entity.RoleMenu;
import com.example.tjauth.Mapper.MenuMapper;
import com.example.tjauth.Service.IAccountRoleService;
import com.example.tjauth.Service.IMenuService;
import com.example.tjauth.Service.IRoleMenuService;
import com.example.tjauth.Service.IRoleService;
import com.example.tjcommon.Exceptions.CommonException;
import com.example.tjcommon.Utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.tjauth.Constants.AuthErrorInfo.Msg.MENU_NOT_FOUND;
import static com.example.tjauth.Constants.AuthErrorInfo.Msg.ROLE_NOT_FOUND;

/**
 * <p>
 * 权限表，包括菜单权限和访问路径权限 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements IMenuService {

    private final IRoleMenuService roleMenuService;
    private final IRoleService roleService;
    private final IAccountRoleService accountRoleService;

    @Override
    public List<Menu> listMenuByUser() {
        // 1.获取用户信息
        Long userId = UserContext.getUser();
        // 2.查询角色
        List<AccountRole> accountRoles = accountRoleService.lambdaQuery().eq(AccountRole::getAccountId, userId).list();
        if (CollUtil.isEmpty(accountRoles)) {
            return Collections.emptyList();
        }
        List<Long> roleIds = accountRoles.stream().map(AccountRole::getRoleId).collect(Collectors.toList());
        // 3.查询菜单
        return getBaseMapper().listByRoles(roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMenu(Menu menu) {
        // 1. 新增菜单
        save(menu);

        // 2. 若有父菜单，判断是否需要更新父菜单的 hasChildren
        Long parentId = menu.getParentId();
        if (parentId != null && parentId != 0L) {
            Menu parent = getById(parentId);
            // 只有父菜单存在、且当前 hasChildren 为 false/null 时才需要更新
            if (parent != null && !Boolean.TRUE.equals(parent.getHasChildren())) {
                parent.setHasChildren(true);
                // ⚠️ 不要手动 setUpdateTime(null)，交给 MyBatis-Plus 自动填充
                updateById(parent);
            }
        }

        // 3. 与管理员角色关联
        RoleMenu roleMenu = new RoleMenu()
                .setMenuId(menu.getId())
                .setRoleId(AuthConstants.ADMIN_ROLE_ID);   // 见下文说明
        roleMenuService.save(roleMenu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        // 1.查询当前菜单
        Menu menu = getById(id);
        if (menu == null) return;
        // 2.判断当前菜单是否有子菜单
        List<Long> delIds;
        if (menu.getHasChildren()) {
            // 2.1.添加子菜单及父菜单
            delIds = lambdaQuery()
                    .eq(Menu::getParentId, id)
                    .list()
                    .stream()
                    .map(Menu::getId)
                    .collect(Collectors.toList());
            // 添加父菜单id
            delIds.add(id);
        }else {
            // 2.2.添加父菜单id
            delIds = Collections.singletonList(id);
        }
        // 3.删除菜单
        removeByIds(delIds);
        // 4.删除菜单与角色的关联数据
        roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getMenuId, delIds));
    }

    @Override
    public void bindRoleMenus(Long roleId, List<Long> menuIds) {
        // 1.判断角色是否存在
        boolean exists = roleService.exists(roleId);
        if (!exists) {
            throw new CommonException(ROLE_NOT_FOUND);
        }
        // 2.判断菜单是否存在
        int menuCount = Math.toIntExact(lambdaQuery().in(Menu::getId, menuIds).count());
        if (menuCount != menuIds.size()) {
            throw new CommonException(MENU_NOT_FOUND);
        }
        // 3.绑定关系
        List<RoleMenu> roleMenus = new ArrayList<>(menuCount);
        for (Long menuId : menuIds) {
            roleMenus.add(new RoleMenu(roleId, menuId));
        }
        // 4.写入数据库
        roleMenuService.saveBatch(roleMenus);
    }

    @Override
    public void deleteRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuService.deleteRoleMenus(roleId, menuIds);
    }
}

