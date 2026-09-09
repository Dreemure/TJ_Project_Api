package com.example.tj_project_apicommon.Utils;

import com.example.tj_project_apicommon.Exceptions.BadRequestException;
import org.apache.commons.lang3.BooleanUtils;
import java.util.Map;

import static com.example.tj_project_apicommon.Constants.ErrorInfo.Msg.REQUEST_PARAM_ILLEGAL;

public final class AssertUtils {

    private AssertUtils() {}

    /**
     * 断言两个对象相等（使用 equals 比较）。
     * <p>若任一对象为 null 或 equals 返回 false，则抛出异常。
     *
     * @param obj1    对象1
     * @param obj2    对象2
     * @param message 可选错误信息（取第一个非空值）
     * @throws BadRequestException 当对象不相等时抛出
     */
    public static void equals(Object obj1, Object obj2, String... message) {
        if (obj1 == null || !obj1.equals(obj2)) {
            handleException(message);
        }
    }

    /**
     * 断言对象不为 null。
     *
     * @param obj     被检查对象
     * @param message 可选错误信息
     * @throws BadRequestException 当 obj 为 null 时抛出
     */
    public static void isNotNull(Object obj, String... message) {
        if (obj == null) {
            handleException(message);
        }
    }

    /**
     * 断言字符串非空白（非 null、非空、非仅含空白字符）。
     *
     * @param str     被检查字符串
     * @param message 可选错误信息
     * @throws BadRequestException 当字符串为空白时抛出
     */
    public static void isNotBlank(String str, String... message) {
        if (StringUtils.isBlank(str)) {
            handleException(message);
        }
    }

    /**
     * 断言条件为 true。
     *
     * @param condition 布尔条件
     * @param message   可选错误信息
     * @throws BadRequestException 当 condition 为 false 时抛出
     */
    public static void isTrue(Boolean condition, String... message) {
        if (BooleanUtils.isFalse(condition)) {
            handleException(message);
        }
    }

    /**
     * 断言条件为 false。
     *
     * @param condition 布尔条件
     * @param message   可选错误信息
     * @throws BadRequestException 当 condition 为 true 时抛出
     */
    public static void isFalse(Boolean condition, String... message) {
        if (BooleanUtils.isTrue(condition)) {
            handleException(message);
        }
    }

    /**
     * 断言集合非空（非 null 且 contains 至少一个元素）。
     *
     * @param coll    被检查集合
     * @param message 可选错误信息
     * @throws BadRequestException 当集合为空或 null 时抛出
     */
    public static void isNotEmpty(Iterable<?> coll, String... message) {
        if (CollUtils.isEmpty(coll)) {
            handleException(message);
        }
    }

    /**
     * 断言 Map 非空（非 null 且 contains 至少一个键值对）。
     *
     * @param map     被检查 Map
     * @param message 可选错误信息
     * @throws BadRequestException 当 Map 为空或 null 时抛出
     */
    public static void isNotEmpty(Map<?, ?> map, String... message) {
        if (CollUtils.isEmpty(map)) {
            handleException(message);
        }
    }

    /**
     * 断言数字大于 0（用于正整数校验）。
     *
     * @param num     被检查数字
     * @param message 可选错误信息
     * @throws BadRequestException 当 num <= 0 时抛出
     */
    public static void isPositive(Number num, String... message) {
        if (num == null || num.doubleValue() <= 0) {
            handleException(message);
        }
    }

    /**
     * 断言数字大于等于 0（用于非负整数校验）。
     *
     * @param num     被检查数字
     * @param message 可选错误信息
     * @throws BadRequestException 当 num < 0 时抛出
     */
    public static void isNonNegative(Number num, String... message) {
        if (num == null || num.doubleValue() < 0) {
            handleException(message);
        }
    }

    /**
     * 内部异常处理：构造并抛出 BadRequestException。
     * 优先使用传入的 message（取第一个非空），否则使用默认消息。
     */
    private static void handleException(String... message) {
        var msg = REQUEST_PARAM_ILLEGAL;
        for (var m : message) {
            if (StringUtils.isNotBlank(m)) {
                msg = m;
                break;
            }
        }
        throw new BadRequestException(msg);
    }
}
