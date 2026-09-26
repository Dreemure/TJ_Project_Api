package com.tjxt.tjcourse.Model.Dto;

import com.tjxt.tjcommon.Validate.Annotations.EnumValid;
import com.tjxt.tjcourse.Constants.CourseErrorInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 课程目录启用或停用模型
 **/
@Data
@Schema(description = "课程分类启用/禁用")
public class CategoryDisableOrEnableDTO {
    @Schema(description = "课程分类id")
    @NotNull(message = CourseErrorInfo.Msg.CATEGORY_ID_NOT_NULL)
    private Long id;

    @Schema(description = "课程分类状态，1：启用，0：禁用")
    @EnumValid(enumeration = {0,1}, message = CourseErrorInfo.Msg.CATEGORY_DISABLE_ENABLE_STATUS_ENUM)
    private Integer status;
}
