package com.tjxt.tjcourse.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 课程分类
 * </p>
 *
 */
@Data
@EqualsAndHashCode(callSuper = false) // 自动生成 equals 和 hashCode，只按当前类的字段比较，不带上父类字段(true为带上)
@Accessors(chain = true) // 让 Lombok 生成的 setter 返回 this，从而可以连续调用多个 setter，写出链式赋值代码
@TableName(value = "category")
public class Category implements Serializable { // Serializable是一个空接口，用来标记“这个类的对象可以被序列化”

    @Serial // 序列化版本号
    private static final long serialVersionUID = 1L;

    /**
     * 课程分类id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    @TableField
    private String name;

    /**
     * 父分类id，一级分类父id为0
     */
    @TableField
    private Long parentId;

    /**
     * 分类级别，1,2,3：代表一级分类，二级分类，三级分类
     */
    @TableField
    private Integer level;

    /**
     * 同级目录优先级，数字越小优先级越高，可以重复
     */
    @TableField
    private Integer priority;

    /**
     * 课程分类状态，1：正常，0：禁用
     */
    @TableField
    private Integer status;

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
     * 创建者
     */
    @TableField
    private Long creater;

    /**
     * 更新者
     */
    @TableField
    private Long updater;

    @TableLogic // 逻辑删除标记。删除变更新，查询自动过滤已删除数据
    private Integer deleted;

}
