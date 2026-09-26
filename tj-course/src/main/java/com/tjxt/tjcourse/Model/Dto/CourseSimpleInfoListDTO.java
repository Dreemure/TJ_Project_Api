package com.tjxt.tjcourse.Model.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CourseSimpleInfoListDTO {
    @Schema(description = "三级分类id列表")
    private List<Long> thirdCataIds;

    @Schema(description = "课程id列表")
    private List<Long> ids;
}
