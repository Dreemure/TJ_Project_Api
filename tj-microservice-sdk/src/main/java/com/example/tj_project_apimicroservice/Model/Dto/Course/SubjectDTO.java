package com.example.tj_project_apimicroservice.Model.Dto.Course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/*
 * 考试问题详情 DTO。
 * 职责：承载试题的详细信息（题干、选项、分值、类型、难度、解析、答案），用于考试与练习场景。
 */
@Data
@Schema(description = "考试问题详情")
public class SubjectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题id")
    private Long id;

    @Schema(description = "题干")
    private String name;

    @Schema(description = "选择题的选项")
    private List<String> options;

    @Schema(description = "分值")
    private Integer score;

    @Schema(description = "问题类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题")
    private Integer subjectType;

    @Schema(description = "难易度，1：简单，2：中等，3：困难")
    private Integer difficulty;

    @Schema(description = "解析")
    private String analysis;

    @Schema(description = "选择题答案，0对应A，1对应B，可填多个")
    private List<Integer> answers;
}
