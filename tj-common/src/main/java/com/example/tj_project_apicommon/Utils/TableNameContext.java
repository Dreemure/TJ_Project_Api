package com.example.tj_project_apicommon.Utils;

/**
 * 用于动态表名参数传递（ThreadLocal 上下文）。
 * 使用完请务必调用 {@link #clear()} 清理，避免线程复用污染。
 */
public class TableNameContext {

    private static final ThreadLocal<String> YEAR_HOLDER = new ThreadLocal<>();

    public static void setYear(String year) {
        YEAR_HOLDER.set(year);
    }

    public static String getYear() {
        return YEAR_HOLDER.get();
    }

    public static void clear() {
        YEAR_HOLDER.remove();
    }
}
