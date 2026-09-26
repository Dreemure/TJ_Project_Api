package com.tjxt.tjcourse.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_catalogue")
public class CourseCatalogue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 课程目录id
     */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /**
     * 目录名称
     */
    @TableField
    private String name;

    /**
     * 是否支持试看
     */
    @TableField
    private Integer trailer;

    /**
     * 课程id
     */
    @TableField
    private Long courseId;

    /**
     * 目录类型1：章，2：节，3：测试
     */
    @TableField
    private Integer type;

    /**
     * 所属章id，只有小节和测试有该值，章没有，章默认为0
     */
    @TableField
    private Long parentCatalogueId;

    /**
     * 媒资id
     */
    @TableField
    private Long mediaId;

    /**
     * 视频id
     */
    @TableField
    private Long videoId;

    /**
     * 视频名称
     */
    @TableField
    private String videoName;

    /**
     * 直播开始时间
     */
    @TableField
    private LocalDateTime livingStartTime;

    /**
     * 直播结束时间
     */
    @TableField
    private LocalDateTime livingEndTime;

    /**
     * 是否支持回放
     */
    @TableField
    private Integer playBack;

    /**
     * 视频时长，以秒为单位
     */
    @TableField
    private Integer mediaDuration;

    /**
     * 用于章节排序
     */
    @TableField
    private Integer cIndex;

    /**
     * 部门id
     */
    @TableField
    private Long depId;

    /**
     * 创建时间
     */
    @TableField
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField
    private Long creater;

    /**
     * 更新人
     */
    @TableField
    private Long updater;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

}
