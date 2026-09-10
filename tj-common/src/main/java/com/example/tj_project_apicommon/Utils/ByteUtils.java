package com.example.tj_project_apicommon.Utils;

import cn.hutool.core.util.ByteUtil;

/**
 * 继承自 hutool 的ByteUtil，增加了将byte[] 数组转换成字符串的功能
 */
public class ByteUtils extends ByteUtil {

    private ByteUtils(){}

    /**
     * 将byte[] 数组转换成字符串,如果为空返回 ""
     * @param content 字节内容
     * @return 字符串值
     */
    public static String parse(byte[] content){
        if(content == null || content.length <= 0) {
            return StringUtils.EMPTY;
        }
        return new String(content);
    }
}
