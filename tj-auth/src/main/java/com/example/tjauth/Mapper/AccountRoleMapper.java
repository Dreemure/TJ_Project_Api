package com.example.tjauth.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tjauth.Entity.AccountRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 账户、角色关联表 Mapper 接口
 * </p>
 *
 */
@Mapper
public interface AccountRoleMapper extends BaseMapper<AccountRole> {
}
