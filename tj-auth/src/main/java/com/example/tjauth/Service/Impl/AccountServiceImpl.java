package com.example.tjauth.Service.Impl;

import com.example.tjauth.Config.AuthProperties;
import com.example.tjauth.Constants.AuthConstants;
import com.example.tjauth.Service.IAccountService;
import com.example.tjauth.Service.ILoginRecordService;
import com.example.tjauth.Utils.JwtIssuer;
import com.example.tjcommon.Exceptions.BadRequestException;
import com.example.tjcommon.Model.Dto.LoginUserDTO;
import com.example.tjcommon.Utils.WebUtils;
import com.example.tjmicroservice.Client.UserGrpcClient;
import com.example.tjmicroservice.Model.Dto.User.LoginFormDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/*
 * 账号服务实现。
 * 职责：处理登录、登出、刷新 token 三大核心流程。
 * 说明：
 *   - 登录：gRPC 调 user 服务校验账号 → 签发 access/refresh token → 记录登录日志
 *   - 登出：清理 refresh token 的 Redis JTI → 清除 Cookie
 *   - 刷新：校验 refresh token → 签发新的双 token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements IAccountService {

    private final JwtIssuer jwtIssuer;
    private final UserGrpcClient userGrpcClient;
    private final ILoginRecordService loginRecordService;
    private final AuthProperties authProperties;
    private final StringRedisTemplate stringRedisTemplate;

    // ==================== 登录 ====================

    @Override
    public String login(LoginFormDTO loginDTO, boolean isStaff) {
        // 1. gRPC 调用 user 服务校验账号
        LoginUserDTO detail = userGrpcClient.queryUserDetail(loginDTO, isStaff);
        if (detail == null) {
            throw new BadRequestException("登录信息有误");
        }

        // 2. 设置"记住我"标记
        detail.setRememberMe(loginDTO.getRememberMe());

        // 3. 生成 token（access + refresh）
        String accessToken = generateToken(detail);

        // 4. 记录登录日志
        loginRecordService.loginSuccess(loginDTO.getCellPhone(), detail.getUserId());

        // 5. 返回 access token
        return accessToken;
    }

    // ==================== 登出 ====================

    @Override
    public void logout() {
        // 1. 清理 Redis 中的 refresh token JTI
        String refreshToken = WebUtils.getCookie(AuthConstants.REFRESH_HEADER);
        if (StringUtils.hasText(refreshToken)) {
            jwtIssuer.cleanRefreshToken(refreshToken);
        }

        // 2. 清除 Cookie
        WebUtils.cookieBuilder()
                .name(AuthConstants.REFRESH_HEADER)
                .value("")
                .maxAge(0)
                .httpOnly(true)
                .build();
        WebUtils.cookieBuilder()
                .name(AuthConstants.ADMIN_REFRESH_HEADER)
                .value("")
                .maxAge(0)
                .httpOnly(true)
                .build();
    }

    // ==================== 刷新 token ====================

    @Override
    public String refreshToken(String refreshToken) {
        // 1. 校验 refresh token（签名 + 过期 + Redis JTI）
        LoginUserDTO user = jwtIssuer.parseRefreshToken(refreshToken);
        if (user == null) {
            throw new BadRequestException("refresh token 无效或已过期");
        }
        // 2. 签发新的双 token
        return generateToken(user);
    }

    // ==================== 私有方法 ====================

    /**
     * 生成 access token + refresh token，并把 refresh token 写入 Cookie。
     *
     * @param user 登录用户信息
     * @return access token（返回给前端，用于后续请求的 Authorization 头）
     */
    private String generateToken(LoginUserDTO user) {
        // 1. 签发 access token
        String accessToken = jwtIssuer.issueToken(user);

        // 2. 签发 refresh token（同时把 JTI 存 Redis）
        String refreshToken = jwtIssuer.issueRefreshToken(user);

        // 3. 写 Cookie（记住我 7 天，否则会话级）
        int maxAge = Boolean.TRUE.equals(user.getRememberMe())
                ? (int) authProperties.getJwt().getRememberMeTtl().toSeconds()
                : -1;
        String cookieName = isStudentRole(user.getRoleId())
                ? AuthConstants.REFRESH_HEADER
                : AuthConstants.ADMIN_REFRESH_HEADER;
        WebUtils.cookieBuilder()
                .name(cookieName)
                .value(refreshToken)
                .maxAge(maxAge)
                .httpOnly(true)
                .build();

        return accessToken;
    }

    /**
     * 判断是否是学生角色（决定用哪个 refresh cookie 名）。
     * <p>学生用 REFRESH_HEADER，员工用 ADMIN_REFRESH_HEADER。
     */
    private boolean isStudentRole(Long roleId) {
        // TODO: 根据你的角色表调整，这里假设 2 是学生
        return roleId != null && roleId == 2L;
    }
}