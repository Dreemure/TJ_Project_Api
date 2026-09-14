package com.example.tjauth.Constants;

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
        /** 未登录 */
        String UNAUTHORIZED = "未登录";
        /** 无访问权限 */
        String FORBIDDEN = "无访问权限";

        // ==================== Token 相关 ====================
        /** 无效的 token */
        String INVALID_TOKEN = "无效的 token";
        /** token 已过期 */
        String EXPIRED_TOKEN = "token 已过期";
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
        /** 过期 token */
        int EXPIRED_TOKEN_CODE = 40101;
        /** 无效 token */
        int INVALID_TOKEN_CODE = 40102;
        /** token 参数格式错误 */
        int INVALID_TOKEN_PAYLOAD_CODE = 40103;

        // ==================== 鉴权相关 ====================
        /** 未登录 */
        int UNAUTHORIZED_CODE = 40100;
        /** 无权限 */
        int FORBIDDEN_CODE = 40300;
    }
}