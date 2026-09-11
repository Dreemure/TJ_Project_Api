package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/*
 * 课程目录简要信息 DTO。
 * 职责：承载课程目录的精简信息（id、名称、数字序号），用于列表展示或关联查询。
 */
@Data
@Schema(description = "课程目录简要信息")
public class CataSimpleInfoDTO {

    @Schema(description = "目录id")
    private Long id;

    @Schema(description = "目录名称")
    private String name;

    @Schema(description = "数字序号，不包含章序号")
    private Integer cIndex;
}
