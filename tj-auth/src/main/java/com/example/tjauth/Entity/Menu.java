package com.example.tjauth.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.tjauth.Model.Dto.MenuDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * 菜单实体。
 * 职责：映射 menu 表，同时承载菜单权限与访问路径权限。
 * 说明：
 *   - 菜单采用树形结构（parentId 关联父菜单），hasChildren 标识是否有子菜单
 *   - 审计字段（creater/updater/createTime/updateTime）由 MyBatis-Plus 自动填充
 *   - deleted 为逻辑删除标识，查询时自动过滤
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("menu")
@NoArgsConstructor
public class Menu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId
    private Long id;

    /** 父菜单id，默认0代表没有父菜单 */
    private Long parentId;

    /** 是否有子菜单，默认 false */
    private Boolean hasChildren;

    /** 菜单文本 */
    private String label;

    /** 菜单路径 */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 顺序优先级，默认127 */
    private Integer priority;

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
     * @param dto 菜单表单数据
     */
    public Menu(MenuDTO dto) {
        this.id = dto.getId();
        this.parentId = dto.getParentId();
        this.label = dto.getLabel();
        this.path = dto.getPath();
        this.icon = dto.getIcon();
        this.priority = dto.getPriority();
    }

    /**
     * 转换为 DTO。
     *
     * @return 菜单表单数据
     */
    public MenuDTO toDTO() {
        MenuDTO dto = new MenuDTO();
        dto.setId(id);
        dto.setPath(path);
        dto.setParentId(parentId);
        dto.setLabel(label);
        dto.setIcon(icon);
        dto.setPriority(priority);
        return dto;
    }
}