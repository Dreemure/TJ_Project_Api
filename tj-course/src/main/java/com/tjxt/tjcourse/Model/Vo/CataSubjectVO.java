package com.tjxt.tjcourse.Model.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 目录和习题模型
 **/
@Data
@Schema(description = "课程题目统计")
public class CataSubjectVO {

    @Schema(description = "小节或测试id")
    private Long cataId;

    @Schema(description = "小节或测试名称")
    private String cataName;

    @Schema(description = "类型，2：节，3：测试")
    private Integer type;

    @Schema(description = "题目数量")
    private Integer subjectNum;

    @Schema(description = "题目总分")
    private Integer subjectScore;
}
