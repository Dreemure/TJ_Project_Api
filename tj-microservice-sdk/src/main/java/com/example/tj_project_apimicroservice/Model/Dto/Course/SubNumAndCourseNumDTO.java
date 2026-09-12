package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 老师课程数与出题数统计 DTO。
 * 职责：承载老师的课程负责数与出题数统计信息，用于教学数据展示。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "老师id和老师对应的课程数，出题数")
public class SubNumAndCourseNumDTO {

    @Schema(description = "老师id")
    private Long teacherId;

    @Schema(description = "老师负责的课程数")
    private Integer courseNum;

    @Schema(description = "老师出题数")
    private Integer subjectNum;

}
