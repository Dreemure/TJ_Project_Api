package com.tjxt.tjauth.Model.Vo;

import com.tjxt.tjauth.Entity.Privilege;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/*
 * API 权限选项视图对象。
 * 职责：向前端暴露权限选项（id、说明、是否选中），用于角色授权时的权限勾选。
 * 说明：支持从 Privilege（PO）构造，便于快速转换。
 */
@Data
@Schema(description = "API权限选项实体")
public class PrivilegeOptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "权限id", example = "1")
    private Long id;

    @Schema(description = "权限说明", example = "新增员工")
    private String intro;

    @Schema(description = "是否选中", example = "true")
    private Boolean checked;

    public PrivilegeOptionVO() {
    }

    /**
     * 从权限实体构造 VO。
     *
     * @param privilege 权限实体
     */
    public PrivilegeOptionVO(Privilege privilege) {
        this.id = privilege.getId();
        this.intro = privilege.getIntro();
    }
}