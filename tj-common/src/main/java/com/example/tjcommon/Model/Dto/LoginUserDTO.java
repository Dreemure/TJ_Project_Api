package com.example.tjcommon.Model.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录用户信息")
public class LoginUserDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "用户角色")
    private String roleName;

    @Schema(description = "是否记住我")
    private Boolean rememberMe;
}