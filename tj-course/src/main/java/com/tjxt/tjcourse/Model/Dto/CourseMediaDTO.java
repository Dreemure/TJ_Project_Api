package com.tjxt.tjcourse.Model.Dto;

import com.tjxt.tjcourse.Constants.CourseErrorInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "课程视频模型")
public class CourseMediaDTO {
    @Schema(description = "目录id")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_MEDIA_SAVE_MEDIA_NULL)
    private Long cataId;

    @Schema(description = "媒资id")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_MEDIA_SAVE_MEDIA_NULL)
    private Long mediaId;

    @Schema(description = "是否支持试看")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_MEDIA_SAVE_MEDIA_NULL)
    private Boolean trailer;

    @Schema(description = "媒资名称")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_MEDIA_SAVE_MEDIA_NULL)
    @Length(min = 1, message = CourseErrorInfo.Msg.COURSE_MEDIA_SAVE_MEDIA_NULL)
    private String videoName;

    @Schema(description = "媒资时长，单位s")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_MEDIA_SAVE_MEDIA_NULL)
    @Min(value = 1, message = CourseErrorInfo.Msg.COURSE_MEDIA_SAVE_MEDIA_NULL)
    private Integer mediaDuration;
}
