package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/*
 * 课程搜索信息 DTO。
 * 职责：承载课程搜索结果的精简信息（基本属性、分类、价格、销量、评分等），用于课程列表与搜索展示。
 */
@Data
@Schema(description = "课程信息")
public class CourseSearchDTO {

    @Schema(description = "课程id")
    private Long id;

    @Schema(description = "课程名称")
    private String name;

    @Schema(description = "一级课程分类id")
    private Long categoryIdLv1;

    @Schema(description = "二级课程分类id")
    private Long categoryIdLv2;

    @Schema(description = "三级课程分类id")
    private Long categoryIdLv3;

    @Schema(description = "课程封面")
    private String coverUrl;

    @Schema(description = "价格")
    private Integer price;

    @Schema(description = "是否免费")
    private Boolean free;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "章节数")
    private Integer sections;

    @Schema(description = "课程时长")
    private Integer duration;

    @Schema(description = "老师id")
    private Long teacher;

    @Schema(description = "课程类型，1：直播课程，2：录播课程")
    private Integer courseType;

    @Schema(description = "课程报名人数（销量）", example = "3920")
    private Integer sold = 0;

    @Schema(description = "课程评价得分，45代表4.5星", example = "35")
    private Integer score = 0;
}