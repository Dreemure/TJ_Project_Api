package com.tjxt.tjauth.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tjxt.tjauth.Entity.RolePrivilege;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 账户、角色关联表 Mapper 接口
 * </p>
 */
@Mapper
public interface RolePrivilegeMapper extends BaseMapper<RolePrivilege> {
}
