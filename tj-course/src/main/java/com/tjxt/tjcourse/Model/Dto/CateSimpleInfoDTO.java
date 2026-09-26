package com.tjxt.tjcourse.Model.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 三级分类
 **/
@Data
@Schema(description = "分类")
public class CateSimpleInfoDTO {
    @Schema(description = "一级分类")
    private Long firstCateId;

    @Schema(description = "二级分类id")
    private Long secondCateId;

    @Schema(description = "三级分类id")
    private Long thirdCateId;
}
