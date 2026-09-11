package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 小节信息 DTO。
 * 职责：承载小节与媒资的关联信息（课程id、媒资id、是否免费试看、免费时长），用于课程小节管理。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "小节信息，包含课程id和媒资id")
public class SectionInfoDTO {

    @Schema(description = "课程id")
    private Long courseId;

    @Schema(description = "媒资id")
    private Long mediaId;

    @Schema(description = "是否支持免费试看")
    private Boolean trailer;

    @Schema(description = "免费时长，不免费为0，单位分钟")
    private Integer freeDuration;
}
