package com.example.tj_project_apimicroservice.Client;

import com.example.tj_project_apimicroservice.Model.Dto.Auth.RoleDTO;
import com.example.tj_project_apimicroservice.proto.AuthServiceGrpc;
import com.example.tj_project_apimicroservice.proto.QueryRoleByIdRequest;
import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

/*
 * 认证服务 gRPC 客户端封装。
 * 职责：通过 Spring Boot gRPC Starter 注入 Stub，调用 AuthService，
 *       将 proto 对象转换为业务 DTO，并内聚降级逻辑（失败返回默认值）。
 */
@Slf4j
@Component
public class AuthGrpcClient {
    @Autowired
    private AuthServiceGrpc.AuthServiceBlockingStub stub;

    /**
     * 根据角色id查询角色信息。
     * <p>调用失败时降级返回 null。
     *
     * @param roleId 角色id
     * @return 角色信息；异常时返回 null
     */
    public RoleDTO queryRoleById(Long roleId) {
        try {
            var request = QueryRoleByIdRequest.newBuilder() // 构建请求体(message)
                    .setId(roleId)
                    .build();
            var protoResponse = stub.queryRoleById(request); // 使用请求体调用方法(rpc)
            return convertToBizRole(protoResponse);
        } catch (StatusRuntimeException e) {
            log.error("查询认证服务异常：queryRoleById, roleId={}", roleId, e);
            return null;
        }
    }

    /**
     * 查询所有角色列表。
     * <p>调用失败时降级返回空列表。
     *
     * @return 角色列表；异常时返回空列表
     */
    public List<RoleDTO> listAllRoles() {
        try {
            var response = stub.listAllRoles(Empty.getDefaultInstance());
            return response.getRolesList().stream()
                    .map(this::convertToBizRole)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询认证服务异常：listAllRoles", e);
            return Collections.emptyList();
        }
    }

    /**
     * 将 proto 的 RoleDTO 转换为业务版 RoleDTO。
     * <p>两个类同名不同包，proto 版用全限定名引用。
     */
    private RoleDTO convertToBizRole(
            com.example.tj_project_apimicroservice.proto.RoleDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new RoleDTO();
        dto.setId(proto.getId());
        dto.setCode(proto.getCode());
        dto.setName(proto.getName());
        return dto;
    }
}
