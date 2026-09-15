package com.example.tjauth.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/*
 * 角色-菜单关联实体。
 * 职责：映射 role_menu 表，表示"角色"与"菜单"的多对多关系。
 * 说明：一个角色可绑定多个菜单；一个菜单可被多个角色绑定。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("role_menu")
@NoArgsConstructor
public class RoleMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 角色id */
    private Long roleId;

    /** 菜单id */
    private Long menuId;

    /**
     * 便捷构造器：根据角色id和菜单id创建关联。
     *
     * @param roleId 角色id
     * @param menuId 菜单id
     */
    public RoleMenu(Long roleId, Long menuId) {
        this.roleId = roleId;
        this.menuId = menuId;
    }
}