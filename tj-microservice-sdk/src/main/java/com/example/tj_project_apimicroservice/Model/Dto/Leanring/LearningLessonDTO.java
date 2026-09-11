package com.example.tj_project_apimicroservice.Model.Dto.Leanring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/*
 * 学习课表进度信息 DTO。
 * 职责：承载课表的学习进度（课表id、最近学习小节id、学习记录列表），用于学习中心展示。
 */
@Data
@Schema(description = "学习课表进度信息")
public class LearningLessonDTO {

    @Schema(description = "课表id")
    private Long id;

    @Schema(description = "最近学习的小节id")
    private Long latestSectionId;

    @Schema(description = "学习过的小节的记录")
    private List<LearningRecordDTO> records;
}
