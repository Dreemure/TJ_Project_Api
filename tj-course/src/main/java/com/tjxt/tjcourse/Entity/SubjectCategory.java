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

/**
 * <p>
 * 课程分类关系表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("subject_category")
public class SubjectCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 题目id
     */
    @TableField
    private Long subjectId;

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

}
