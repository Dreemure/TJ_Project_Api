package com.tjxt.tjcourse.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 课程内容，主要是一些大文本
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_content_draft")
public class CourseContentDraft implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 课程内容id
     */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /**
     * 课程介绍
     */
    @TableField
    private String courseIntroduce;

    /**
     * 适用人群
     */
    @TableField
    private String usePeople;

    /**
     * 课程详情
     */
    @TableField
    private String courseDetail;

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
