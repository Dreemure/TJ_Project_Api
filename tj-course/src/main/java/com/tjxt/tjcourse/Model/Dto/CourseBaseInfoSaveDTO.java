package com.tjxt.tjcourse.Model.Dto;

import com.tjxt.tjcommon.Exceptions.BadRequestException;
import com.tjxt.tjcommon.Utils.DateUtils;
import com.tjxt.tjcommon.Validate.Checker;
import com.tjxt.tjcourse.Constants.CourseErrorInfo;
import com.tjxt.tjcourse.Utils.CourseSaveBaseGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程基本信息保存 DTO
 *
 * 关于 groups = CourseSaveBaseGroup.class 的说明：
 *
 * 1. 没写 groups 的 @NotNull 属于默认组（Default）：
 *    name、thirdCateId、free、purchaseEndTime、validDuration
 *    只要控制器用了 @Valid 或 @Validated，就会校验这些字段。
 *
 * 2. 写了 groups = CourseSaveBaseGroup.class 的 @NotNull 属于 CourseSaveBaseGroup 组：
 *    coverUrl、introduce、usePeople、detail
 *    只有控制器显式指定了这个分组时，才会校验这些字段。
 *
 * 3. 控制器示例：(写参数里)
 *    @Validated(CourseSaveBaseGroup.class)
 *    → 只校验 coverUrl、introduce、usePeople、detail
 *
 *    @Validated({Default.class, CourseSaveBaseGroup.class})
 *    → 默认组和 CourseSaveBaseGroup 组都校验
 *
 *    @Valid
 *    → 只校验默认组，不校验 coverUrl、introduce、usePeople、detail
 *
 * 4. check() 方法是手动调用的业务校验，不受分组影响。
 */
@Data
@Schema(description = "课程基本信息保存")
public class CourseBaseInfoSaveDTO implements Checker {
    @Schema(description = "课程id，新课程该值不能传，老课程必填")
    private Long id;

    @Schema(description = "课程名称")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_NAME_NULL)
    private String name;

    @Schema(description = "三级课程分类id")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_CATEGORY_NULL)
    private Long thirdCateId;

    @Schema(description = "封面链接url")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_COVER_URL_NULL, groups = CourseSaveBaseGroup.class)
    private String coverUrl;

    @Schema(description = "是否是免费")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_FREE_NULL)
    private Boolean free;

    @Schema(description = "课程价格")
    @Min(value = 0, message = CourseErrorInfo.Msg.COURSE_SAVE_PRICE_NEGATIVE)
    private Integer price;

    @Schema(description = "购买周期开始时间")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_PURCHASE_TIME_NULL)
    private LocalDateTime purchaseStartTime;

    @Schema(description = "购买周期结束时间")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_PURCHASE_TIME_NULL)
    private LocalDateTime purchaseEndTime;

    @Schema(description = "课程介绍")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_INTRODUCE_NULL, groups = CourseSaveBaseGroup.class)
    private String introduce;

    @Schema(description = "使用人群")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_USE_PEOPLE_NULL, groups = CourseSaveBaseGroup.class)
    private String usePeople;

    @Schema(description = "详情")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_DETAIL_NULL, groups = CourseSaveBaseGroup.class)
    private String detail;

    @Schema(description = "学习周期，0或不传表示没有期限，其他表示月数")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_SAVE_DURATION_NULL)
    private Integer validDuration;

    @Override
    public void check() {
        if(!free) { //非免费
            if(price == null) { //付费课程未设置价格
                throw new BadRequestException(CourseErrorInfo.Msg.COURSE_SAVE_PRICE_NULL);
            }
            if(price <= 0){ //付费课程设置价格小于0
                throw new BadRequestException(CourseErrorInfo.Msg.COURSE_SAVE_PRICE_NEGATIVE);
            }
        }else { //免费
            if(price != null && price > 0){ //免费课程设置了价格
                throw new BadRequestException(CourseErrorInfo.Msg.COURSE_SAVE_PRICE_FREE);
            }
        }
        if(purchaseEndTime.isBefore(DateUtils.now())){
            throw new BadRequestException(CourseErrorInfo.Msg.COURSE_SAVE_PURCHASE_ILLEGAL);
        }
        if (purchaseStartTime.isAfter(purchaseEndTime)) {
            throw new BadRequestException(CourseErrorInfo.Msg.COURSE_SAVE_PURCHASE_ILLEGAL);
        }
        if(id == null && purchaseStartTime.isBefore(LocalDateTime.now())){
            throw new BadRequestException(CourseErrorInfo.Msg.COURSE_SAVE_PURCHASE_ILLEGAL2);
        }
    }
}
