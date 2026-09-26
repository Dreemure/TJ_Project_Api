package com.tjxt.tjcourse.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 草稿课程
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_draft")
public class CourseDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 课程草稿id，对应正式草稿id
     */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /**
     * 课程名称
     */
    @TableField
    private String name;

    /**
     * 课程类型，1：直播课，2：录播课
     */
    @TableField
    private Integer courseType;

    /**
     * 封面链接
     */
    @TableField
    private String coverUrl;

    /**
     * 一级课程分类id
     */
    @TableField
    private Long firstCateId;

    /**
     * 二级课程分类id
     */
    @TableField
    private Long secondCateId;

    /**
     * 三级课程分类id
     */
    @TableField
    private Long thirdCateId;

    /**
     * 售卖方式0付费，1：免费
     */
    @TableField
    private Integer free;

    /**
     * 课程价格，单位为分
     */
    @TableField
    private Integer price;

    /**
     * 模板类型，1：固定模板，2：自定义模板
     */
    @TableField
    private Integer templateType;

    /**
     * 自定义模板的连接
     */
    @TableField
    private String templateUrl;

    /**
     * 课程状态，1：待上架，2：已上架，3：下架，4：已完结
     */
    @TableField
    private Integer status;

    /**
     * 课程购买有效期开始时间
     */
    @TableField
    private LocalDateTime purchaseStartTime;

    /**
     * 课程购买有效期结束时间
     */
    @TableField
    private LocalDateTime purchaseEndTime;

    /**
     * 信息填写进度1：基本信息已经保存，2：课程目录已经保存，3：课程视频已保存，4：课程题目已保存，5：课程老师已经保存
     */
    @TableField
    private Integer step;

    /**
     * 课程评分，45代表4.5星
     */
    @TableField
    private Integer score;

    /**
     * 视频总时长
     */
    @TableField
    private Integer mediaDuration;

    /**
     * 课程有效期，单位月
     */
    @TableField
    private Integer validDuration;

    /**
     * 课程总节数
     */
    @TableField
    private Integer sectionNum;

    /**
     * 是否可以修改
     */
    @TableField
    private Boolean canUpdate;

    /**
     * 版本控制
     */
    @TableField
    private Integer cVersion;

    /**
     * 部门id
     */
    @TableField
    private Long depId;

    /**
     * 发布时间
     */
    @TableField
    private LocalDateTime publishTime;

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
}

