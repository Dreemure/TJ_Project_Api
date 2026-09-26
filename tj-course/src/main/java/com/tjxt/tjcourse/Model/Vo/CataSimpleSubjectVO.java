package com.tjxt.tjcourse.Model.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CataSimpleSubjectVO {
    @Schema(description = "小节或练习id")
    private Long cataId;

    @Schema(description = "题目id")
    private List<SubjectInfo> subjects;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubjectInfo{
        private Long id;
        private String name;
    }
}
