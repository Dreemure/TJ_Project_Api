package com.tjxt.tjcommon.Utils;

import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/*
 * 角色名解析工具。
 * 职责：把登录用户信息（用户类型 + 角色代号）翻译成 Spring Security 的权限名（ROLE_xxx），
 *       供 gateway（验签后授权）、auth 服务（本服务鉴权）、microservice-sdk（下游服务鉴权）三方共用，
 *       保证三处角色口径完全一致。
 * 说明：
 *   - 用户类型：1-员工 → ROLE_STAFF，2-学员 → ROLE_STUDENT，3-老师 → ROLE_TEACHER（见 UserType 枚举）
 *   - 角色代号：数据库 role.code（如 admin）→ ROLE_ADMIN，便于 @PreAuthorize("hasRole('ADMIN')")
 *   - 未识别的类型/代号不授予任何权限，避免“默认放行”
 */
public final class RoleUtils {

    private RoleUtils() {
        // 工具类私有构造，防止实例化
    }

    /**
     * 解析用户类型与角色代号对应的角色名集合。
     *
     * @param type     用户类型：1-员工 2-学员 3-老师，可为 null
     * @param roleName 角色代号，可为空
     * @return 角色名集合（可能为空集合，不会为 null）
     */
    public static Set<String> resolve(Integer type, String roleName) {
        Set<String> roles = new LinkedHashSet<>();
        if (type != null) {
            switch (type) {
                case 1 -> roles.add(AuthConstants.ROLE_STAFF);
                case 2 -> roles.add(AuthConstants.ROLE_STUDENT);
                case 3 -> roles.add(AuthConstants.ROLE_TEACHER);
                default -> {
                    // 未知类型：不授予任何角色
                }
            }
        }
        if (StringUtils.isNotBlank(roleName)) {
            roles.add(AuthConstants.ROLE_PREFIX + roleName.trim().toUpperCase(Locale.ROOT));
        }
        return roles;
    }

    /**
     * 解析登录用户的角色名集合。
     *
     * @param user 登录用户信息，可为 null
     * @return 角色名集合（可能为空集合，不会为 null）
     */
    public static Set<String> resolve(LoginUserDTO user) {
        if (user == null) {
            return Set.of();
        }
        return resolve(user.getType(), user.getRoleName());
    }
}
