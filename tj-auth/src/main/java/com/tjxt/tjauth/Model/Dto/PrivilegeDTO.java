package com.tjxt.tjauth.Model.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/*
 * API 权限。
 * 职责：定义一条 API 权限规则（菜单、说明、请求方式、URI），用于权限管理与动态鉴权。
 */
@Data
@Schema(description = "API权限")
public class PrivilegeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "权限id", example = "1")
    private Long id;

    @Schema(description = "权限所属菜单id", example = "1")
    private Long menuId;

    @Schema(description = "权限说明", example = "新增员工")
    @NotNull(message = "权限说明不能为空")
    private String intro;

    @Schema(description = "API请求方式", example = "GET")
    @Pattern(regexp = "^GET|POST|PUT|DELETE$", message = "请求方式必须是大写")
    private String method;

    @Schema(description = "API请求路径", example = "/account/staff")
    @NotNull(message = "uri不能为空")
    private String uri;

    @Schema(description = "是否是内部权限")
    private Boolean internal;
}