package com.example.tjauth.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.tjauth.Model.Dto.PrivilegeDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * 权限实体。
 * 职责：映射 privilege 表，同时承载菜单权限与访问路径权限。
 * 说明：
 *   - 一条记录 = 一条 API 权限规则（method + uri → 所属菜单）
 *   - 审计字段（creater/updater/createTime/updateTime）由 MyBatis-Plus 自动填充
 *   - deleted 为逻辑删除标识
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("`privilege`")
public class Privilege implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId
    private Long id;

    /** 菜单id */
    private Long menuId;

    /** 权限说明 */
    private String intro;

    /** API权限的请求方式 */
    private String method;

    /** API权限的请求路径 */
    private String uri;

    /** 是否是内部接口 */
    private Boolean internal;

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

    public Privilege() {
    }

    /**
     * 从 DTO 构造实体。
     *
     * @param dto 权限表单数据
     */
    public Privilege(PrivilegeDTO dto) {
        this.id = dto.getId();
        this.menuId = dto.getMenuId();
        this.intro = dto.getIntro();
        this.method = dto.getMethod();
        this.uri = dto.getUri();
        this.internal = dto.getInternal();
    }

    /**
     * 转换为 DTO。
     *
     * @return 权限表单数据
     */
    public PrivilegeDTO toDTO() {
        PrivilegeDTO dto = new PrivilegeDTO();
        dto.setId(id);
        dto.setMenuId(menuId);
        dto.setIntro(intro);
        dto.setMethod(method);
        dto.setUri(uri);
        dto.setInternal(internal);
        return dto;
    }
}