package com.example.tj_project_apimicroservice.Model.Dto.Exam;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/*
 * 题目与业务关联信息 DTO。
 * 职责：承载题目与业务对象（如小节）的关联关系，用于题目与业务的双向绑定。
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Schema(description = "题目与业务关联信息")
public class QuestionBizDTO {

    @Schema(description = "业务id，要关联问题的某业务id，例如小节id")
    private Long bizId;

    @Schema(description = "题目id")
    private Long questionId;
}
