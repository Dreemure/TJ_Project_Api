package com.example.tj_project_apimicroservice.Model.Dto.Trade;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/*
 * 订单基础信息 DTO。
 * 职责：承载订单的核心信息（订单id、用户id、课程id集合、完成时间），用于订单数据传递。
 */
@Data
@Builder
@Schema(description = "订单基础信息")
public class OrderBasicDTO {

    @Schema(description = "订单id")
    private Long orderId;

    @Schema(description = "下单用户id")
    private Long userId;

    @Schema(description = "下单的课程id集合")
    private List<Long> courseIds;

    @Schema(description = "订单完成时间")
    private LocalDateTime finishTime;
}
