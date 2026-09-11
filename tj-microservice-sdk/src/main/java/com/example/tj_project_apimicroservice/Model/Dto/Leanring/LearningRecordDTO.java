package com.example.tj_project_apimicroservice.Model.Dto.Leanring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/*
 * 小节信息及学习进度 DTO。
 * 职责：承载小节的学习进度（小节id、当前观看时长、是否完成），用于学习记录展示与进度计算。
 */
@Data
@Schema(description = "小节信息及学习进度")
public class LearningRecordDTO {

    @Schema(description = "对应节的id")
    private Long sectionId;

    @Schema(description = "视频的当前观看时长，单位秒")
    private Integer moment;

    @Schema(description = "是否完成学习，默认false")
    private Boolean finished;
}
