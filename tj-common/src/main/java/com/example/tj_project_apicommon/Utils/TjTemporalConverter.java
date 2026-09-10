package com.example.tj_project_apicommon.Utils;

import cn.hutool.core.convert.impl.TemporalAccessorConverter;

import java.time.temporal.TemporalAccessor;

/*
 * 时间转换器（支持时间戳字符串）。
 * 职责：继承 TemporalAccessorConverter，重写 convertInternal，使字符串形式的时间戳（如 "1672531200000"）也能正确转换为 TemporalAccessor 子类型（LocalDateTime 等）。
 * 行为：若输入为 String，先尝试 Long.parseLong，成功则按时间戳转换，失败则回退到父类的字符串解析。
 * 使用：在需要兼容时间戳与日期字符串的场景中，注册为自定义 Converter。
 */
public class TjTemporalConverter extends TemporalAccessorConverter {
    public TjTemporalConverter(Class<?> targetType) {
        super(targetType);
    }

    public TjTemporalConverter(Class<?> targetType, String format) {
        super(targetType, format);
    }

    @Override
    protected TemporalAccessor convertInternal(Object value) {
        if (value instanceof String val) {
            if (StringUtils.isNumeric(val)) {
                return super.convertInternal(Long.parseLong(val));
            }
            return super.convertInternal(val);
        }
        return super.convertInternal(value);
    }

}
