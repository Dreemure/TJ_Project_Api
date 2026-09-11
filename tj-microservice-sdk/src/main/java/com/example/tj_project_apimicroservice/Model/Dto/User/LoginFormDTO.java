package com.example.tj_project_apimicroservice.Model.Dto.User;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*
 * 登录表单实体 DTO。
 * 职责：承载登录请求参数（登录方式、用户名、手机号、密码、是否记住我），用于登录接口。
 */
@Data
@Schema(description = "登录表单实体")
public class LoginFormDTO {

    @Schema(description = "登录方式：1-密码登录; 2-验证码登录", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer type;

    @Schema(description = "用户名", example = "jack")
    private String username;

    @Schema(description = "手机号", example = "13800010001")
    private String cellPhone;

    @Schema(description = "密码", example = "123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String password;

    @Schema(description = "7天免密登录", example = "true")
    private Boolean rememberMe = false;
}
