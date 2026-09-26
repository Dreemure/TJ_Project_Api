package com.tjxt.tjcourse.Model.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程简单信息
 **/
@Data
@Schema(description = "课程简单信息")
public class CourseSimpleInfoVO {

    @Schema(description = "课程id")
    private Long id;

    @Schema(description = "课程名称")
    private String name;

    @Schema(description = "封面url")
    private String coverUrl;

    @Schema(description = "价格")
    private Integer price;

    @Schema(description = "一级分类id")
    private Long firstCateId;

    @Schema(description = "二级分类id")
    private Long secondCateId;

    @Schema(description = "三级分类id")
    private Long thirdCateId;

    @Schema(description = "章节数量")
    private Integer sectionNum;

    @Schema(description = "课程有效期")
    private Integer validDuration;

    @Schema(description = "课程过期时间")
    private LocalDateTime purchaseEndTime;
}

