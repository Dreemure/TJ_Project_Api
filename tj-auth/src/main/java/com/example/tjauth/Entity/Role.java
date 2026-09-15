package com.example.tjauth.Entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.tjmicroservice.Model.Dto.Auth.RoleDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * 角色实体。
 * 职责：映射 role 表，定义角色基础信息（代号、名称、类型）。
 * 说明：
 *   - code 为角色代号（如 admin、teacher），JWT 中的 roleName 用的就是它
 *   - type 分为固定角色（不可删）和自定义角色
 *   - 审计字段由 MyBatis-Plus 自动填充
 *   - deleted 为逻辑删除标识
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("role")
@NoArgsConstructor
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId
    private Long id;

    /** 角色代号，例如：admin */
    private String code;

    /** 角色名称 */
    private String name;

    /** 角色类型：0-固定角色（不可选）1-自定义角色 */
    private RoleType type;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者id */
    private Long creater;

    /** 更新者id */
    private Long updater;

    /** 部门id */
    private Long depId;

    /** 逻辑删除，默认0 */
    private Integer deleted;

    /**
     * 从 DTO 构造实体。
     *
     * @param dto 角色表单数据
     */
    public Role(RoleDTO dto) {
        this.id = dto.getId();
        this.code = dto.getCode();
        this.name = dto.getName();
    }

    /**
     * 转换为 DTO。
     *
     * @return 角色表单数据
     */
    public RoleDTO toDTO() {
        RoleDTO dto = new RoleDTO();
        dto.setId(id);
        dto.setCode(code);
        dto.setName(name);
        return dto;
    }

    /**
     * 角色类型枚举。
     * <p>配合 MyBatis-Plus 的 @EnumValue，存入数据库的是 value（0/1）。
     */
    @Getter
    public enum RoleType {
        /** 固定角色，不可选 */
        CONSTANT(0, "固定角色"),
        /** 自定义角色 */
        CUSTOM(1, "自定义角色");

        /** 存入数据库的值 */
        @EnumValue
        final int value;

        /** 描述 */
        final String desc;

        RoleType(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }
    }
}