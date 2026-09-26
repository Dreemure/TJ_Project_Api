package com.tjxt.tjcourse.Entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tjxt.tjcommon.Utils.CollUtils;
import com.tjxt.tjcommon.Utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 题目
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("subject")
public class Subject implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 题目id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 题干
     */
    @TableField
    private String name;

    /**
     * 题目类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题
     */
    @TableField
    private Integer subjectType;

    /**
     * 难易度，1：简单，2：中等，3：困难
     */
    @TableField
    private Integer difficulty;

    /**
     * 选择题答案1，
     */
    @TableField
    private String option1;

    /**
     * 选择题答案2
     */
    @TableField
    private String option2;

    /**
     * 选择题答案3
     */
    @TableField
    private String option3;

    /**
     * 选择题答案4
     */
    @TableField
    private String option4;

    /**
     * 选择题答案5
     */
    @TableField
    private String option5;

    /**
     * 选择题答案6
     */
    @TableField
    private String option6;

    /**
     * 选择题答案7
     */
    @TableField
    private String option7;

    /**
     * 选择题答案8
     */
    @TableField
    private String option8;

    /**
     * 选择题答案9
     */
    @TableField
    private String option9;

    /**
     * 选择题答案10
     */
    @TableField
    private String option10;

    /**
     * 选择题正确答案1到10，如果有多个答案，中间使用逗号隔开，如果是判断题，1：代表正确，其他代表错误
     */
    @TableField
    private String answer;

    /**
     * 答案解析
     */
    @TableField
    private String analysis;

    /**
     * 回答正确次数
     */
    @TableField
    private Integer correctTimes;

    /**
     * 分值
     */
    @TableField
    private Integer score;

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
     * 引用次数
     */
    @TableField
    private Integer useTimes;

    /**
     * 作答次数
     */
    @TableField
    private Integer answerTimes;

    /**
     * 创建人
     */
    @TableField
    private Long creater;

    /**
     * 更新人
     */
    private Long updater;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    public List<String> getOptions() {
        return Stream.of(option1, option2, option3, option4, option5, option6, option7, option8, option9, option10)
                .filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

    public List<Integer> getAnswers() {
        return CollUtils.convertToInteger(StringUtils.split(answer, ","));
    }
}
