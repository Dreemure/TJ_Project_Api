package com.example.tj_project_apicommon.Autoconfigure.Mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.tj_project_apicommon.Utils.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.example.tj_project_apicommon.Constants.Constant.DATA_FIELD_NAME_CREATER;
import static com.example.tj_project_apicommon.Constants.Constant.DATA_FIELD_NAME_UPDATER;

/**
 * MyBatis-Plus 自动填充处理器（创建人、更新人）。字段存在即填充，用户 ID 为 null 时自动填 0
 * 仅支持单条记录的插入/更新，批量操作不生效。
 * 若需自定义填充逻辑，可自行实现 {@link MetaObjectHandler} 并覆盖本 Bean。
 */
@Component
@ConditionalOnMissingBean(MetaObjectHandler.class)
public class BaseMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        var userId = UserContext.getUser();
        var safeUserId = Objects.requireNonNullElse(userId, 0L);  // JDK 原生替代
        strictInsertFill(metaObject, DATA_FIELD_NAME_CREATER, Long.class, safeUserId);
        strictUpdateFill(metaObject, DATA_FIELD_NAME_UPDATER, Long.class, safeUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        var userId = UserContext.getUser();
        var safeUserId = Objects.requireNonNullElse(userId, 0L);
        strictUpdateFill(metaObject, DATA_FIELD_NAME_UPDATER, Long.class, safeUserId);
    }
}