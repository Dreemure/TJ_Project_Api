package com.example.tj_project_apicommon.Constants;

/*
 * 错误信息常量接口。
 * 职责：集中管理业务错误码（Code）和错误消息（Msg），统一全局异常及 API 响应的提示文案。
 * 内容分类：Code（状态码：200成功/0失败）、Msg（各类业务提示信息，如用户不存在、参数非法、操作频繁等）。
 * 使用：通过静态导入（import static ...ErrorInfo.*），在异常处理和响应构建中引用。
 * 注意：Msg 和 Code 采用嵌套接口方式隔离职责，便于分类管理。
 */
public interface ErrorInfo {

    interface Msg {
        String OK = "OK";
        String INVALID_VERIFY_CODE = "验证码错误";


        String SERVER_INTER_ERROR = "服务器内部错误";

        String DB_SAVE_EXCEPTION = "数据新增失败";
        String DB_DELETE_EXCEPTION = "数据删除失败";
        String DB_BATCH_DELETE_EXCEPTION = "数据批量删除失败";
        String DB_UPDATE_EXCEPTION = "数据更新失败";
        String DB_SORT_FIELD_NOT_FOUND = "排序字段不存在";
        String OPERATE_FAILED = "操作失败";

        String REQUEST_PARAM_ILLEGAL = "请求参数不合法";
        String REQUEST_OPERATE_FREQUENTLY = "操作频繁,请稍后重试";
        String REQUEST_TIME_OUT = "请求超时";

        String USER_NOT_EXISTS = "用户信息不存在";
        String INVALID_USER_TYPE = "无效的用户类型";
    }

    interface Code {
        int SUCCESS = 200;
        int FAILED = 0;
    }
}
