package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 媒资被引用情况 DTO。
 * 职责：承载媒资的引用统计信息（媒资id、引用数），用于媒资管理与清理场景。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "媒资被引用情况")
public class MediaQuoteDTO {

    @Schema(description = "媒资id")
    private Long mediaId;

    @Schema(description = "引用数")
    private Integer quoteNum;
}
