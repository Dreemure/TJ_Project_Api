package com.tjxt.tjauth.Model.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * 登录记录视图对象。
 * 职责：向前端暴露登录记录的展示数据（时间、时长、IP），不包含敏感字段。
 * 说明：与 LoginRecord（PO）相比，去掉了 cellPhone 等敏感/冗余字段。
 */
@Data
@Schema(description = "登录记录")
public class LoginRecordVO {

    @Schema(description = "用户id", example = "1")
    private Long userId;

    @Schema(description = "登录时间", example = "2026-09-15 09:00:00")
    private LocalDateTime loginTime;

    @Schema(description = "登出时间", example = "2026-09-15 18:00:00")
    private LocalDateTime logoutTime;

    @Schema(description = "登录日期", example = "2026-09-15")
    private LocalDate loginDate;

    @Schema(description = "登录时长，单位秒", example = "3600")
    private Long duration;

    @Schema(description = "客户端 IPv4 地址", example = "192.168.1.100")
    private String ipv4;
}