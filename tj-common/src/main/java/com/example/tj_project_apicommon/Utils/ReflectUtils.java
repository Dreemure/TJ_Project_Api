package com.example.tj_project_apicommon.Utils;

import cn.hutool.core.util.ReflectUtil;

/*
 * 继承自 hutool的ReflectUtil(反射工具类)
 */
public class ReflectUtils extends ReflectUtil {
    private ReflectUtils(){}

    /**
     * 判断一个类中是否含有指定字段
     *
     * @param fieldName 指定字段名称
     * @param clazz     类class
     * @return 是否包含 true/false
     */
    public static boolean containField(String fieldName, Class<?> clazz) {
        return getField(clazz, fieldName) != null;
    }
}
