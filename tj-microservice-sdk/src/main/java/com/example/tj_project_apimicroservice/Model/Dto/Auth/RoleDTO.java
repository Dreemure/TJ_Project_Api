package com.example.tj_project_apimicroservice.Model.Dto.Auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/*
 * 角色信息 DTO。
 * 职责：用于跨服务/跨层传输角色基础数据（id、code、name）。
 * 说明：位于 API 模块，供接口调用方使用，字段与后端实体解耦。
 */
@Data
@EqualsAndHashCode(callSuper = false) // 让 Lombok 自动生成 equals() 和 hashCode() 方法
@Accessors(chain = true) // 让 Lombok 生成的 setter 方法返回 this，从而支持链式调用
@Schema(description = "角色实体")
public class RoleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /*
     * 主键
     */
    @Schema(description = "主键", example = "1")
    private Long id;

    /*
     * 角色代号，例如：admin
     */
    @Schema(description = "角色代号", example = "admin")
    private String code;

    /*
     * 角色描述
     */
    @Schema(description = "角色名称", example = "教师")
    private String name;
}
