package com.tjxt.tjcourse.Model.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 所有课程分类数据
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "所有课程分类数据")
public class SimpleCategoryVO {

    private Long id;

    private String name;

    private List<SimpleCategoryVO> children;

    private Integer level;

    private Long parentId;

}

