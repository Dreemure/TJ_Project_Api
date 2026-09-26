package com.tjxt.tjcourse.Model.Dto;

import com.tjxt.tjcourse.Constants.CourseErrorInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "课程id")
public class CourseIdDTO {
    @Schema(description = "课程id")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_OPERATE_ID_NULL)
    private Long id;
}
