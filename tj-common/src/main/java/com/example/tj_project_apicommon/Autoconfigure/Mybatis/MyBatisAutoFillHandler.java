package com.example.tj_project_apicommon.Autoconfigure.Mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.tj_project_apicommon.Utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.example.tj_project_apicommon.Constants.Constant.DATA_FIELD_NAME_CREATER;
import static com.example.tj_project_apicommon.Constants.Constant.DATA_FIELD_NAME_UPDATER;

/*
 * MyBatis-Plus 自动填充处理器。仅当字段存在且值为 null 且用户 ID 不为 null 时才填充
 * 职责：在插入/更新操作时自动填充创建人、更新人及时间字段（从 UserContext 获取用户 ID）。
 * 使用：直接作为 Spring Bean 注入，MyBatis-Plus 自动拦截生效。
 * 注意：仅支持单条记录操作；需配合 @TableField(fill = FieldFill.INSERT/UPDATE) 使用。
 */
@Slf4j
@Component
public class MyBatisAutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 1. 获取当前用户 ID（可能为 null，表示未登录）
        Long userId = UserContext.getUser();
        // 2. 自动填充创建人（如果实体类有该字段且未设置）
        if (metaObject.hasSetter(DATA_FIELD_NAME_CREATER)) {
            Object createVal = getFieldValByName(DATA_FIELD_NAME_CREATER, metaObject);
            if (createVal == null && userId != null) setFieldValByName(DATA_FIELD_NAME_CREATER, userId, metaObject);
        }
        // 3. 自动填充更新时间（可选）
        if (metaObject.hasSetter("createTime")) setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        // 4. 更新人字段在插入时也可以同步填充（视业务需要）
        updateFill(metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = UserContext.getUser();
        if (metaObject.hasSetter(DATA_FIELD_NAME_UPDATER) && userId != null) {
            Object updateVal = getFieldValByName(DATA_FIELD_NAME_UPDATER, metaObject);
            if (updateVal == null) setFieldValByName(DATA_FIELD_NAME_UPDATER, userId, metaObject);
        }
        // 填充更新时间
        if (metaObject.hasSetter("updateTime")) {
            setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }
    }
}