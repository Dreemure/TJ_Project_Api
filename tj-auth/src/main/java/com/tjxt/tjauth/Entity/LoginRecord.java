package com.tjxt.tjauth.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * 登录信息记录实体。
 * 职责：映射 login_record 表，记录每次登录的上下文（时间、IP、时长）。
 * 说明：用于登录审计、在线时长统计、异常登录检测等场景。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("login_record")
public class LoginRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户id */
    private Long userId;

    /** 手机号 */
    private String cellPhone;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 登出时间 */
    private LocalDateTime logoutTime;

    /** 登录时长，单位秒 */
    private Long duration;

    /** 客户端 IPv4 地址 */
    private String ipv4;
}