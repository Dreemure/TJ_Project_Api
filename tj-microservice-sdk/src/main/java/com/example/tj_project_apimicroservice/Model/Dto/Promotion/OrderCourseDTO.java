package com.example.tj_project_apimicroservice.Model.Dto.Promotion;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/*
 * 订单中的课程信息 DTO。
 * 职责：承载订单关联课程的精简信息（课程id、三级分类id、价格），用于订单结算与优惠计算。
 */
@Data
@Accessors(chain = true)
@Schema(description = "订单中的课程信息")
public class OrderCourseDTO {

    @Schema(description = "课id")
    private Long id;

    @Schema(description = "课程的三级分类id")
    private Long cateId;

    @Schema(description = "课程价格")
    private Integer price;
}
