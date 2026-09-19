package com.tjxt.tjauth.Controller;

import com.tjxt.tjauth.Constants.AuthConstants;
import com.tjxt.tjauth.Service.IAccountService;
import com.tjxt.tjcommon.Exceptions.BadRequestException;
import com.tjxt.tjcommon.Utils.WebUtils;
import com.tjxt.tjmicroservice.Model.Dto.User.LoginFormDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static org.yaml.snakeyaml.util.UriEncoder.decode;


/*
 * 账户登录相关接口。
 * 职责：
 *   - 前台/管理端登录，返回 access token
 *   - 登出，清理 refresh token 的 Redis JTI + 清除 Cookie
 *   - 刷新 token，用 refresh token 换新的 access token
 * 说明：
 *   - access token 通过响应体返回，前端存入 localStorage 并放入 Authorization 头
 *   - refresh token 通过 HttpOnly Cookie 保存，前端无法通过 JS 读取（防 XSS）
 */
@RestController
@RequestMapping("/v2/accounts")
@Tag(name = "账户管理")
@RequiredArgsConstructor
public class AccountController {

    private final IAccountService accountService;

    /**
     * 前台登录。
     */
    @Operation(summary = "登录并获取token")
    @PostMapping("/login")
    public String loginByPw(@RequestBody LoginFormDTO loginFormDTO) {
        return accountService.login(loginFormDTO, false);
    }

    /**
     * 管理端登录。
     */
    @Operation(summary = "管理端登录并获取token")
    @PostMapping("/admin/login")
    public String adminLoginByPw(@RequestBody LoginFormDTO loginFormDTO) {
        return accountService.login(loginFormDTO, true);
    }

    /**
     * 登出。
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public void logout() {
        accountService.logout();
    }

    @Operation(summary = "前台刷新token")
    @GetMapping("/refresh")
    public String refreshUserToken(
            @CookieValue(value = AuthConstants.REFRESH_HEADER, required = false) String studentToken) {
        if (studentToken == null) {
            throw new BadRequestException("登录超时");
        }
        return accountService.refreshToken(decode(studentToken));
    }

    @Operation(summary = "管理端刷新token")
    @GetMapping("/admin/refresh")
    public String refreshAdminToken(
            @CookieValue(value = AuthConstants.ADMIN_REFRESH_HEADER, required = false) String adminToken) {
        if (adminToken == null) {
            throw new BadRequestException("登录超时");
        }
        return accountService.refreshToken(decode(adminToken));
    }
}