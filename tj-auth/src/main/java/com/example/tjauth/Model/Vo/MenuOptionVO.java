package com.example.tjauth.Model.Vo;

import com.example.tjauth.Entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/*
 * 菜单选项视图对象。
 * 职责：向前端暴露菜单的树形选项结构（id、父id、文本、图标、子菜单），用于下拉树选择。
 * 说明：支持从 Menu（PO）构造，便于快速转换。
 */
@Data
@Schema(description = "菜单选项实体")
public class MenuOptionVO {

    @Schema(description = "菜单id", example = "1")
    private Long id;

    @Schema(description = "父菜单id", example = "0")
    private Long parentId;

    @Schema(description = "菜单文本", example = "系统管理")
    private String label;

    @Schema(description = "菜单图标", example = "el-icon-sys")
    private String icon;

    @Schema(description = "是否有子菜单", example = "false")
    private Boolean hasChildren;

    @Schema(description = "菜单顺序", example = "1")
    private Integer priority;

    @Schema(description = "子菜单集合")
    private List<MenuOptionVO> subMenus;

    public MenuOptionVO() {
    }

    /**
     * 从菜单实体构造 VO。
     *
     * @param menu 菜单实体
     */
    public MenuOptionVO(Menu menu) {
        this.id = menu.getId();
        this.parentId = menu.getParentId();
        this.label = menu.getLabel();
        this.icon = menu.getIcon();
        this.hasChildren = menu.getHasChildren();
        this.priority = menu.getPriority();
    }
}