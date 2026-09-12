package com.example.tj_project_apimicroservice.Client;

import com.example.tj_project_apimicroservice.Model.Dto.User.LoginFormDTO;
import com.example.tj_project_apicommon.Model.Dto.LoginUserDTO;
import com.example.tj_project_apimicroservice.Model.Dto.User.UserDTO;
import com.example.tj_project_apimicroservice.proto.ExchangeUserIdWithPhoneRequest;
import com.example.tj_project_apimicroservice.proto.ExchangeUserIdWithPhoneResponse;
import com.example.tj_project_apimicroservice.proto.QueryUserByIdRequest;
import com.example.tj_project_apimicroservice.proto.QueryUserByIdsRequest;
import com.example.tj_project_apimicroservice.proto.QueryUserDetailRequest;
import com.example.tj_project_apimicroservice.proto.QueryUserTypeRequest;
import com.example.tj_project_apimicroservice.proto.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;


/*
 * 用户服务 gRPC 客户端封装。
 * 职责：通过 Spring Boot gRPC Starter 注入 Stub，调用 UserService，
 *       将 proto 对象转换为业务 DTO，并内聚降级逻辑（失败返回默认值）。
 */
@Slf4j
@Component
public class UserGrpcClient {

    /**
     * 自动注入 Stub。
     * 对应 application.yml 中 spring.grpc.client.channels.* 的通道配置。
     */
    @Autowired
    private UserServiceGrpc.UserServiceBlockingStub stub;

    // ==================== 业务方法 ====================

    /**
     * 根据手机号查询用户id。
     * <p>调用失败时降级返回 null。
     *
     * @param phone 手机号
     * @return 用户id；异常时返回 null
     */
    public Long exchangeUserIdWithPhone(String phone) {
        try {
            var request = ExchangeUserIdWithPhoneRequest.newBuilder()
                    .setPhone(phone)
                    .build();
            ExchangeUserIdWithPhoneResponse response = stub.exchangeUserIdWithPhone(request);
            return response.getUserId();
        } catch (StatusRuntimeException e) {
            log.error("查询用户服务异常：exchangeUserIdWithPhone, phone={}", phone, e);
            return null;
        }
    }

    /**
     * 登录接口。
     * <p>调用失败时降级返回 null。
     *
     * @param loginDTO 登录表单
     * @param isStaff  是否是员工
     * @return 登录用户信息；异常时返回 null
     */
    public LoginUserDTO queryUserDetail(LoginFormDTO loginDTO, boolean isStaff) {
        try {
            var request = QueryUserDetailRequest.newBuilder()
                    .setLoginDTO(convertToProtoLoginForm(loginDTO))
                    .setIsStaff(isStaff)
                    .build();
            var protoResponse = stub.queryUserDetail(request);
            return convertToBizLoginUser(protoResponse);
        } catch (StatusRuntimeException e) {
            log.error("查询用户服务异常：queryUserDetail, isStaff={}", isStaff, e);
            return null;
        }
    }

    /**
     * 查询用户类型。
     * <p>调用失败时降级返回 null。
     *
     * @param id 用户id
     * @return 用户类型（0-普通学员，1-老师，2-其他员工）；异常时返回 null
     */
    public Integer queryUserType(Long id) {
        try {
            var request = QueryUserTypeRequest.newBuilder()
                    .setId(id)
                    .build();
            return stub.queryUserType(request).getType();
        } catch (StatusRuntimeException e) {
            log.error("查询用户服务异常：queryUserType, id={}", id, e);
            return null;
        }
    }

    /**
     * 根据id批量查询用户信息。
     * <p>调用失败时降级返回空列表。
     *
     * @param ids 用户id集合
     * @return 用户列表；异常时返回空列表
     */
    public List<UserDTO> queryUserByIds(Iterable<Long> ids) {
        try {
            var builder = QueryUserByIdsRequest.newBuilder();
            if (ids != null) {
                builder.addAllIds(ids);
            }
            var response = stub.queryUserByIds(builder.build());
            return response.getUsersList().stream()
                    .map(this::convertToBizUser)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询用户服务异常：queryUserByIds, ids={}", ids, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据id查询单个用户信息。
     * <p>调用失败时降级返回 null。
     *
     * @param id 用户id
     * @return 用户信息；异常时返回 null
     */
    public UserDTO queryUserById(Long id) {
        try {
            var request = QueryUserByIdRequest.newBuilder()
                    .setId(id)
                    .build();
            var protoResponse = stub.queryUserById(request);
            return convertToBizUser(protoResponse);
        } catch (StatusRuntimeException e) {
            log.error("查询用户服务异常：queryUserById, id={}", id, e);
            return null;
        }
    }


    /**
     * 业务版 LoginFormDTO → proto 版 LoginFormDTO。
     */
    private com.example.tj_project_apimicroservice.proto.LoginFormDTO convertToProtoLoginForm(
            LoginFormDTO biz) {
        if (biz == null) {
            return com.example.tj_project_apimicroservice.proto.LoginFormDTO.getDefaultInstance();
        }
        return com.example.tj_project_apimicroservice.proto.LoginFormDTO.newBuilder()
                .setType(biz.getType() == null ? 0 : biz.getType())
                .setUsername(biz.getUsername() == null ? "" : biz.getUsername())
                .setCellPhone(biz.getCellPhone() == null ? "" : biz.getCellPhone())
                .setPassword(biz.getPassword() == null ? "" : biz.getPassword())
                .setRememberMe(biz.getRememberMe() != null && biz.getRememberMe())
                .build();
    }

    /**
     * proto 版 LoginUserDTO → 业务版 LoginUserDTO。
     */
    private LoginUserDTO convertToBizLoginUser(
            com.example.tj_project_apimicroservice.proto.LoginUserDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new LoginUserDTO();
        dto.setUserId(proto.getUserId());
        dto.setRoleId(proto.getRoleId());
        dto.setRememberMe(proto.getRememberMe());
        return dto;
    }

    /**
     * proto 版 UserDTO → 业务版 UserDTO。
     */
    private UserDTO convertToBizUser(
            com.example.tj_project_apimicroservice.proto.UserDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new UserDTO();
        dto.setId(proto.getId());
        dto.setCellPhone(proto.getCellPhone());
        dto.setName(proto.getName());
        dto.setType(proto.getType());
        dto.setRoleId(proto.getRoleId());
        dto.setIcon(proto.getIcon());
        dto.setJob(proto.getJob());
        dto.setIntro(proto.getIntro());
        dto.setPhoto(proto.getPhoto());
        dto.setUsername(proto.getUsername());
        dto.setEmail(proto.getEmail());
        dto.setQq(proto.getQq());
        dto.setProvince(proto.getProvince());
        dto.setCity(proto.getCity());
        dto.setDistrict(proto.getDistrict());
        dto.setGender(proto.getGender());
        return dto;
    }
}