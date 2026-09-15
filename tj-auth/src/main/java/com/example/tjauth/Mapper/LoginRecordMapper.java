package com.example.tjauth.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tjauth.Entity.LoginRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 登录信息记录表 Mapper 接口
 * </p>
 */
@Mapper
public interface LoginRecordMapper extends BaseMapper<LoginRecord> {

}

