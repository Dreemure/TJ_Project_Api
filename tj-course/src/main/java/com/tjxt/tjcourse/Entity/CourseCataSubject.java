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
 * 课程-题目关系表草稿
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_cata_subject")
public class CourseCataSubject implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 小节题目关系id
     */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /**
     * 课程id
     */
    @TableField
    private Long courseId;

    /**
     * 小节id
     */
    @TableField
    private Long cataId;

    /**
     * 题目id
     */
    @TableField
    private Long subjectId;
}
