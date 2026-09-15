package com.example.tjauth.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tjauth.Entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 权限表，包括菜单权限和访问路径权限 Mapper 接口
 * </p>
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    List<Menu> listByRoles(@Param("roleIds") List<Long> roleIds);
}

