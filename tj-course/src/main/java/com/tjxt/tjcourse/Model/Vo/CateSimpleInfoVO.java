package com.tjxt.tjcourse.Model.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 三级分类
 **/
@Data
@Schema(description = "分类")
public class CateSimpleInfoVO {

    @Schema(description = "一级分类")
    private Long firstCateId;

    @Schema(description = "一级分类名称")
    private String firstCateName;

    @Schema(description = "二级分类id")
    private Long secondCateId;

    @Schema(description = "二级分类名称")
    private String secondCateName;

    @Schema(description = "三级分类id")
    private Long thirdCateId;

    @Schema(description = "三级分类名称")
    private String thirdCateName;

}
