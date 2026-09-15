package com.example.tjauth.Service.Impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.tjauth.Entity.AccountRole;
import com.example.tjauth.Mapper.AccountRoleMapper;
import com.example.tjauth.Service.IAccountRoleService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 账户、角色关联表 服务实现类
 * </p>
 */
@Service
public class AccountRoleServiceImpl extends ServiceImpl<AccountRoleMapper, AccountRole> implements IAccountRoleService {

}
