package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 课程购买信息 DTO。
 * 职责：承载课程的购买统计数据（报名人数、退款人数、实付总金额），用于课程销售数据展示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程购买信息")
public class CoursePurchaseInfoDTO {

    @Schema(description = "报名人数")
    private Integer enrollNum;

    @Schema(description = "退款人数")
    private Integer refundNum;

    @Schema(description = "实付总金额")
    private Integer realPayAmount;
}
