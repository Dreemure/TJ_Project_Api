package com.tjxt.tjcourse.Model.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "课程视频保存模型")
public class CourseMediaSaveDTO {
    @Schema(description = "小节id")
    private Long cataId;

    @Schema(description = "媒资id")
    private Long mediaId;

    @Schema(description = "是否支持试看")
    private Boolean trailer;
}
