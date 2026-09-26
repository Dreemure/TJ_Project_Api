package com.tjxt.tjcourse.Model.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Schema(description = "课程保存结果")
@AllArgsConstructor
@NotNull
@Builder
public class CourseSaveVO {
    @Schema(description = "课程id")
    private Long id;
}
