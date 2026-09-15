package com.tjxt.tjauth.Constants;

/*
 * 认证服务错误信息常量。
 * 职责：集中管理认证/鉴权场景下的业务状态码（Code）与提示消息（Msg）。
 * 使用：通过静态导入（import static ...AuthErrorInfo.*）在异常处理和响应构建中引用。
 * 注意：Code 与 Msg 通过嵌套接口隔离职责，便于分类管理和国际化扩展。
 */
public interface AuthErrorInfo {

    /**
     * 业务提示消息（用户可见）。
     */
    interface Msg {

        // ==================== 通用鉴权 ====================
        /** 未登录（与 common/ErrorInfo、网关 401 输出保持一致） */
        String UNAUTHORIZED = com.tjxt.tjcommon.Constants.ErrorInfo.Msg.UNAUTHORIZED;
        /** 无访问权限（与 common/ErrorInfo、网关 403 输出保持一致） */
        String FORBIDDEN = com.tjxt.tjcommon.Constants.ErrorInfo.Msg.FORBIDDEN;

        // ==================== Token 相关 ====================
        /** 无效的 token */
        String INVALID_TOKEN = com.tjxt.tjcommon.Constants.ErrorInfo.Msg.INVALID_TOKEN;
        /** token 已过期 */
        String EXPIRED_TOKEN = com.tjxt.tjcommon.Constants.ErrorInfo.Msg.EXPIRED_TOKEN;
        /** token 参数格式错误 */
        String INVALID_TOKEN_PAYLOAD = "token 参数格式错误";

        // ==================== 员工与角色 ====================
        /** 无效的账户类型 */
        String INVALID_STAFF_TYPE = "无效的账户类型";
        /** 绑定的角色不存在 */
        String INVALID_ROLE_ID = "绑定的角色不存在";
        /** 角色数据不存在 */
        String ROLE_NOT_FOUND = "角色数据不存在";

        // ==================== 权限与菜单 ====================
        /** 权限信息已存在 */
        String PRIVILEGE_EXISTS = "权限信息已存在";
        /** 权限数据不存在 */
        String PRIVILEGE_NOT_FOUND = "权限数据不存在";
        /** 菜单数据不存在 */
        String MENU_NOT_FOUND = "菜单数据不存在";
    }

    /**
     * 业务状态码（前端根据此码判断具体错误）。
     * <p>编码规则：401xx 表示鉴权相关错误。
     */
    interface Code {

        // ==================== Token 相关 ====================
        /** 过期 token（与 common/ErrorInfo 同码） */
        int EXPIRED_TOKEN_CODE = com.tjxt.tjcommon.Constants.ErrorInfo.Code.EXPIRED_TOKEN;
        /** 无效 token（与 common/ErrorInfo 同码） */
        int INVALID_TOKEN_CODE = com.tjxt.tjcommon.Constants.ErrorInfo.Code.INVALID_TOKEN;
        /** token 参数格式错误 */
        int INVALID_TOKEN_PAYLOAD_CODE = 40103;

        // ==================== 鉴权相关 ====================
        /** 未登录（与 common/ErrorInfo 同码） */
        int UNAUTHORIZED_CODE = com.tjxt.tjcommon.Constants.ErrorInfo.Code.UNAUTHORIZED;
        /** 无权限（与 common/ErrorInfo 同码） */
        int FORBIDDEN_CODE = com.tjxt.tjcommon.Constants.ErrorInfo.Code.FORBIDDEN;
    }
}