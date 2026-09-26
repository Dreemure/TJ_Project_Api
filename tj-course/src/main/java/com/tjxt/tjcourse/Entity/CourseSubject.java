package com.tjxt.tjcourse.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * 课程题目关系列表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_subject")
@NotNull
@AllArgsConstructor
public class CourseSubject implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 课程题目关系id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField
    private Long courseId;

    @TableField
    private Long subjectId;
}
