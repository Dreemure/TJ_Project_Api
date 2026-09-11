package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/*
 * 分类基础信息 DTO。
 * 职责：承载分类的精简信息（id、名称、父分类id），用于列表展示或关联查询。
 */
@Data
@Schema(description = "分类id和名称信息")
public class CategoryBasicDTO {

    @Schema(description = "分类id", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "Java")
    private String name;

    @Schema(description = "父分类id", example = "0")
    private Long parentId;
}
