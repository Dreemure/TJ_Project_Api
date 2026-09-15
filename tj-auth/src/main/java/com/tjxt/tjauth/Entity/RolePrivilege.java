package com.tjxt.tjauth.Entity;

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
 * 角色-权限关联实体。
 * 职责：映射 role_privilege 表，表示"角色"与"权限"的多对多关系。
 * 说明：一个角色可拥有多个权限；一个权限可被多个角色拥有。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("role_privilege")
@NoArgsConstructor
public class RolePrivilege implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 角色id */
    private Long roleId;

    /** 权限id */
    private Long privilegeId;

    /**
     * 便捷构造器：根据角色id和权限id创建关联。
     *
     * @param roleId      角色id
     * @param privilegeId 权限id
     */
    public RolePrivilege(Long roleId, Long privilegeId) {
        this.roleId = roleId;
        this.privilegeId = privilegeId;
    }
}