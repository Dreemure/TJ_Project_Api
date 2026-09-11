package com.example.tj_project_apimicroservice.Model.Dto.Remark;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 点赞次数 DTO。
 * 职责：承载业务对象的点赞数统计（业务id、点赞次数），用于点赞数据传递。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Schema(description = "点赞次数")
public class LikedTimesDTO {

    @Schema(description = "业务id")
    private Long bizId;

    @Schema(description = "点赞次数")
    private Integer likedTimes;
}