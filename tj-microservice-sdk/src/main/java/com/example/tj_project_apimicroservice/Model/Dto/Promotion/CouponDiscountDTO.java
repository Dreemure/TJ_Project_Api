package com.example.tj_project_apimicroservice.Model.Dto.Promotion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * 订单的可用优惠券及折扣信息 DTO。
 * 职责：承载订单可用的优惠券信息与折扣明细（优惠券id、规则、最大优惠金额、按课程的优惠明细），用于订单结算。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(description = "订单的可用优惠券及折扣信息")
public class CouponDiscountDTO {

    @Schema(description = "用户优惠券id集合")
    private List<Long> ids = new ArrayList<>();

    @Schema(description = "优惠券规则")
    private List<String> rules = new ArrayList<>();

    @Schema(description = "本订单最大优惠金额")
    private Integer discountAmount = 0;

    @Schema(description = "优惠明细,key是课程id，value是课程优惠金额")
    private Map<Long, Integer> discountDetail;
}
