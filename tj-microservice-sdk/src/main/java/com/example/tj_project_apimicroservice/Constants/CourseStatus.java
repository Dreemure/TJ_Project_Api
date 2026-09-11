package com.example.tj_project_apimicroservice.Constants;

import com.example.tj_project_apicommon.Enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/*
 * 课程状态枚举。
 * 职责：定义课程的四种状态（待上架、已上架、下架、已完结），并提供按值查描述的静态方法。
 */
@Getter
@AllArgsConstructor
public enum CourseStatus implements BaseEnum {
    NO_UP_SHELF(1, "待上架"),
    SHELF(2, "已上架"),
    DOWN_SHELF(3, "下架"),
    FINISHED(4, "已完结");

    private final int value;
    private final String desc;

    /**
     * 根据状态值查询描述。
     *
     * @param status 状态值
     * @return 描述；未匹配或 status 为 null 时返回空字符串
     */
    public static String desc(Integer status) {
        if (status == null) {
            return "";
        }
        return Arrays.stream(values())
                .filter(s -> s.value == status)
                .map(CourseStatus::getDesc)
                .findFirst()
                .orElse("");
    }
}
