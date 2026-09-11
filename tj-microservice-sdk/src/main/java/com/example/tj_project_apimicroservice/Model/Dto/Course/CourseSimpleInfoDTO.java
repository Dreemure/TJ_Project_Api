package com.example.tj_project_apimicroservice.Model.Dto.Course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/*
 * 课程简要信息 DTO。
 * 职责：承载课程的精简信息（基本属性、分类、价格、有效期等），用于列表展示或关联查询。
 */
@Data
public class CourseSimpleInfoDTO {

    @Schema(description = "课程id")
    private Long id;

    @Schema(description = "课程名称")
    private String name;

    @Schema(description = "封面url")
    private String coverUrl;

    @Schema(description = "价格")
    private Integer price;

    @Schema(description = "课程状态")
    private Integer status;

    @Schema(description = "是否是免费课程")
    private Boolean free;

    @Schema(description = "一级分类id")
    private Long firstCateId;

    @Schema(description = "二级分类id")
    private Long secondCateId;

    @Schema(description = "三级分类id")
    private Long thirdCateId;

    @Schema(description = "小节数量")
    private Integer sectionNum;

    @Schema(description = "课程购买有效期结束时间")
    private LocalDateTime purchaseEndTime;

    @Schema(description = "课程学习有效期，单位：月")
    private Integer validDuration;

    /**
     * 获取课程的三级分类id列表（不参与 JSON 序列化）。
     */
    @JsonIgnore
    public List<Long> getCategoryIds() {
        return List.of(firstCateId, secondCateId, thirdCateId);
    }
}
