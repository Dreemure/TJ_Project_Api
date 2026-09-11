package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/*
 * 课程分类 DTO。
 * 职责：承载课程分类的完整信息（名称、层级、状态、数量统计、时间等），用于分类管理与展示。
 */
@Data
@Schema(description = "课程分类")
public class CategoryDTO {

    @Schema(description = "课程分类id")
    private Long id;

    @Schema(description = "课程分类名称")
    private String name;

    @Schema(description = "三级分类数量")
    private Integer thirdCategoryNum;

    @Schema(description = "课程数量")
    private Integer courseNum;

    @Schema(description = "状态：1：正常，2：禁用")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "排序")
    private Integer index;

    @Schema(description = "父id")
    private Long parentId;

    @Schema(description = "级别")
    private Integer level;
}
