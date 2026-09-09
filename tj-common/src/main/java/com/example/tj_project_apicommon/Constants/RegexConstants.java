package com.example.tj_project_apicommon.Constants;

import cn.hutool.core.lang.RegexPool;

/*
 * 正则表达式常量接口。
 * 职责：集中管理项目中常用的正则表达式模式，用于参数校验、格式验证等场景。
 * 内容分类：手机号（PHONE_PATTERN）、邮箱（EMAIL_PATTERN）、密码（PASSWORD_PATTERN）、
 *           用户名（USERNAME_PATTERN）、验证码（VERIFY_CODE_PATTERN）、
 *           优惠券兑换码（COUPON_CODE_PATTERN）。
 * 使用：静态导入（import static ...RegexConstants.*），配合 @Pattern 注解或手动校验。
 * 注意：继承自 Hutool 的 RegexPool，可复用其提供的常用正则（如身份证、IP等）。
 */
public interface RegexConstants extends RegexPool {
    // 手机号正则
    String PHONE_PATTERN = "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}$";
    // 邮箱正则
    String EMAIL_PATTERN = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
    // 密码正则。6~32位的字母、数字、下划线
    String PASSWORD_PATTERN = "^\\w{4,24}$";
    // 用户名正则。6~32位的字母、数字、下划线
    String USERNAME_PATTERN = "^\\w{4,32}$";
    // 验证码正则, 6位数字或字母
    String VERIFY_CODE_PATTERN = "^[a-zA-Z\\d]{6}$";
    // 优惠券兑换码模板
    String COUPON_CODE_PATTERN = "^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8,10}$";
}
