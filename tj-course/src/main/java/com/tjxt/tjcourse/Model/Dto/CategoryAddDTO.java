package com.tjxt.tjcourse.Model.Dto;

import com.tjxt.tjcourse.Constants.CourseErrorInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程分类新增模型
 **/
@Data
@Schema(description = "课程分类新增模型")
public class CategoryAddDTO {
    @Schema(description = "父分类id,如果是新增一级分类")
    private Long parentId;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED) //requiredMode = Schema.RequiredMode.REQUIRED 对应 required = true，not为false
    @NotNull(message = CourseErrorInfo.Msg.CATEGORY_ADD_NAME_NOT_NULL)
    @Size(max = 15, message = CourseErrorInfo.Msg.CATEGORY_ADD_NAME_SIZE)
    private String name;

    @Schema(description = "分类序号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 99, min = 1, message = CourseErrorInfo.Msg.CATEGORY_ADD_INDEX_MAX_MIN)
    @NotNull(message = CourseErrorInfo.Msg.CATEGORY_ADD_INDEX_NOT_NULL)
    private Integer index;
}
