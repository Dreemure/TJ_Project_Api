package com.example.tj_project_apicommon.Constants;

/*
 * 项目公共常量接口。
 * 职责：集中管理全局常量定义，涵盖请求头、数据字段名、逻辑标志及特殊标识。
 * 内容分类：链路追踪头（REQUEST_ID_HEADER）、服务来源标识（GATEWAY_ORIGIN_NAME）、
 *           数据库字段名（CREATER/UPDATER等）、软删除标志（DATA_DELETE）、
 *           响应处理标记（BODY_PROCESSED_MARK_HEADER）。
 * 使用：静态导入常量（import static ...Constant.*），避免硬编码字符串。
 * 注意：接口中的字段隐式为 public static final，无需显式修饰符。
 */
public interface Constant {
    String REQUEST_ID_HEADER = "requestId";
    String REQUEST_FROM_HEADER = "x-request-from";

    String GATEWAY_ORIGIN_NAME = "gateway";
    String FEIGN_ORIGIN_NAME = "feign";

    // 数据字段 - id
    String DATA_FIELD_NAME_ID = "id";

    // 数据字段 - create_time
    String DATA_FIELD_NAME_CREATE_TIME = "create_time";
    String DATA_FIELD_NAME_CREATE_TIME_CAMEL = "createTime";

    // 数据字段 - update_time
    String DATA_FIELD_NAME_UPDATE_TIME = "update_time";
    String DATA_FIELD_NAME_UPDATE_TIME_CAMEL = "updateTime";

    // 数据字段 - liked_times
    String DATA_FIELD_NAME_LIKED_TIME = "liked_times";
    String DATA_FIELD_NAME_LIKED_TIME_CAMEL = "likedTimes";

    // 数据字段 - creater
    String DATA_FIELD_NAME_CREATER = "creater";

    // 数据字段 - updater
    String DATA_FIELD_NAME_UPDATER = "updater";

    // 数据已经删除标识值
    boolean DATA_DELETE = true;
    // 数据未删除标识值
    boolean DATA_NOT_DELETE = false;
    // 响应结果是否被R标记过
    String BODY_PROCESSED_MARK_HEADER = "IS_BODY_PROCESSED";
}

