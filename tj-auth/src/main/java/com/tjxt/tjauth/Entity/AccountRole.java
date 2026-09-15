package com.tjxt.tjauth.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/*
 * 账户-角色关联实体。
 * 职责：映射 account_role 表，表示"账户"与"角色"的多对多关系。
 * 说明：一个账户可以绑定多个角色；一个角色可以被多个账户绑定。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("account_role")
public class AccountRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 账户id */
    private Long accountId;

    /** 角色id */
    private Long roleId;
}