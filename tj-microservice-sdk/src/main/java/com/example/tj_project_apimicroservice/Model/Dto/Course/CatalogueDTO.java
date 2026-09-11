package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/*
 * 课程目录 DTO。
 * 职责：承载课程章、节、练习的层级结构信息，支持嵌套（sections 递归包含子目录）。
 */
@Data
@Schema(description = "课程目录")
public class CatalogueDTO {

    @Schema(description = "章、节、练习id")
    private Long id;

    @Schema(description = "序号")
    private Integer index;

    @Schema(description = "章节练习名称")
    private String name;

    @Schema(description = "课程总时长,单位秒")
    private Integer mediaDuration;

    @Schema(description = "是否支持免费试看")
    private Boolean trailer;

    @Schema(description = "媒资名称")
    private String mediaName;

    @Schema(description = "媒资id")
    private Long mediaId;

    @Schema(description = "目录类型1：章，2：节，3：测试")
    private Integer type;

    @Schema(description = "题目数量")
    private Integer subjectNum;

    @Schema(description = "题目总分")
    private Integer totalScore;

    @Schema(description = "是否可以修改,默认不能修改")
    private Boolean canUpdate = false;

    @Schema(description = "该章的所有小节和练习")
    private List<CatalogueDTO> sections;
}
