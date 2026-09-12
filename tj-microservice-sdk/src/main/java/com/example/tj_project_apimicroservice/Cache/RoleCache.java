package com.example.tj_project_apimicroservice.Cache;

import com.example.tj_project_apicommon.Enums.UserType;
import com.example.tj_project_apimicroservice.Client.AuthGrpcClient;
import com.example.tj_project_apimicroservice.Model.Dto.Auth.RoleDTO;
import com.example.tj_project_apimicroservice.Model.Dto.User.UserDTO;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
 * 角色缓存管理。
 * 职责：基于 Caffeine 缓存角色数据（key=角色id），提供角色名称查询与员工名称拼接能力。
 * 数据来源：通过 AuthGrpcClient 调用认证服务获取角色，懒加载进缓存。
 * 注意：角色数据变更时需通过 roleCaches.invalidate(roleId) 主动失效。
 */
@Component
@RequiredArgsConstructor
public class RoleCache {

    private final Cache<Long, RoleDTO> roleCaches;
    private final AuthGrpcClient authGrpcClient;

    /**
     * 根据角色id查询角色名称。
     *
     * @param roleId 角色id
     * @return 角色名称；不存在时返回 null
     */
    public String getRoleName(Long roleId) {
        RoleDTO roleDTO = roleCaches.get(roleId, authGrpcClient::queryRoleById);
        if (roleDTO == null) return null;
        return roleDTO.getName();
    }

    /**
     * 拼接用户显示名称。
     * <p>学生直接返回用户名；员工返回"角色名-用户名"格式。
     *
     * @param u 用户信息
     * @return 显示名称；用户为 null 时返回 "--"
     */
    public String exchangeRoleName(UserDTO u) {
        if (u == null) return "--";
        if (UserType.STUDENT.equalsValue(u.getType())) {
            // 学生，直接返回角色名称
            return u.getName();
        } else {
            // 管理员需要拼接角色名称
            return getRoleName(u.getRoleId()) + "-" + u.getName();
        }
    }
}