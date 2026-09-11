package com.example.tj_project_apimicroservice.Model.Dto.Promotion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/*
 * 订单中课程及优惠券信息 DTO。
 * 职责：承载订单关联的课程列表与用户优惠券id，用于订单结算与优惠计算。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单中课程及优惠券信息")
public class OrderCouponDTO {

    @Schema(description = "用户优惠券id")
    private List<Long> userCouponIds;

    @Schema(description = "订单中的课程列表")
    private List<OrderCourseDTO> courseList;
}
