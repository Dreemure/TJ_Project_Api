package com.example.tj_project_apimicroservice.Model.Dto.Leanring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/*
 * 学习记录表单数据 DTO。
 * 职责：承载前端提交的学习进度数据（小节类型、课表id、小节id、时长、观看进度、提交时间），用于学习进度上报。
 */
@Data
@Schema(description = "学习记录表单数据")
public class LearningRecordFormDTO {

    @Schema(description = "小节类型：1-视频，2-考试")
    private Integer sectionType;

    @Schema(description = "课表id")
    private Long lessonId;

    @Schema(description = "对应节的id")
    private Long sectionId;

    @Schema(description = "视频总时长，单位秒")
    private Integer duration;

    @Schema(description = "视频的当前观看时长，单位秒，第一次提交填0")
    private Integer moment;

    @Schema(description = "提交时间")
    private LocalDateTime commitTime;
}