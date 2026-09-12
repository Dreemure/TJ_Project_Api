package com.example.tj_project_apimicroservice.Client;

import com.example.tj_project_apicommon.Exceptions.BizIllegalException;
import com.example.tj_project_apimicroservice.Model.Dto.Promotion.CouponDiscountDTO;
import com.example.tj_project_apimicroservice.proto.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import com.example.tj_project_apimicroservice.Model.Dto.Promotion.OrderCourseDTO;
import com.example.tj_project_apimicroservice.proto.FindDiscountSolutionRequest;
import com.example.tj_project_apimicroservice.proto.FindDiscountSolutionResponse;
import com.example.tj_project_apimicroservice.proto.PromotionServiceGrpc;
import com.example.tj_project_apimicroservice.proto.QueryDiscountDetailByOrderRequest;
import com.example.tj_project_apimicroservice.proto.QueryDiscountDetailByOrderResponse;
import com.example.tj_project_apimicroservice.proto.QueryDiscountRulesRequest;
import com.example.tj_project_apimicroservice.proto.RefundCouponRequest;
import com.example.tj_project_apimicroservice.proto.WriteOffCouponRequest;

/*
 * 促销服务 gRPC 客户端封装。
 * 职责：通过 Spring Boot gRPC Starter 注入 Stub，调用 PromotionService，
 *       将 proto 对象转换为业务 DTO，并内聚降级逻辑（失败返回默认值）。
 */
@Slf4j
@Component
public class PromotionGrpcClient {

    /**
     * 自动注入 Stub。
     * 对应 application.yml 中 spring.grpc.client.channels.* 的通道配置。
     */
    @Autowired
    private PromotionServiceGrpc.PromotionServiceBlockingStub stub;

    // ==================== 业务方法 ====================

    /**
     * 查找优惠方案。
     * 调用失败时降级返回空列表。
     *
     * @param userCouponIds 用户优惠券id列表
     * @param rules         优惠券规则
     * @return 优惠方案列表；异常时返回空列表
     */
    public List<CouponDiscountDTO> findDiscountSolution(List<Long> userCouponIds, List<String> rules) {
        try {
            var request = FindDiscountSolutionRequest.newBuilder()
                    .addAllUserCouponIds(userCouponIds == null ? List.of() : userCouponIds)
                    .addAllRules(rules == null ? List.of() : rules)
                    .build();
            var response = stub.findDiscountSolution(request);
            return List.of(convertFromFindSolution(response));
        } catch (StatusRuntimeException e) {
            log.error("查询促销服务异常：findDiscountSolution, userCouponIds={}", userCouponIds, e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询订单的优惠明细。
     * 调用失败时降级返回 null。
     *
     * @param userCouponIds 用户优惠券id列表
     * @param rules         优惠券规则
     * @return 优惠明细；异常时返回 null
     */
    public CouponDiscountDTO queryDiscountDetailByOrder(List<Long> userCouponIds, List<String> rules) {
        try {
            var request = QueryDiscountDetailByOrderRequest.newBuilder()
                    .addAllUserCouponIds(userCouponIds == null ? List.of() : userCouponIds)
                    .addAllRules(rules == null ? List.of() : rules)
                    .build();
            var response = stub.queryDiscountDetailByOrder(request);
            return convertFromQueryDetail(response);
        } catch (StatusRuntimeException e) {
            log.error("查询促销服务异常：queryDiscountDetailByOrder, userCouponIds={}", userCouponIds, e);
            return null;
        }
    }

    /**
     * 核销优惠券。
     * 调用失败时抛出业务异常。
     *
     * @param userCouponIds 待核销的优惠券id列表
     */
    public void writeOffCoupon(List<Long> userCouponIds) {
        try {
            var request = WriteOffCouponRequest.newBuilder()
                    .addAllUserCouponIds(userCouponIds == null ? List.of() : userCouponIds)
                    .build();
            stub.writeOffCoupon(request);
        } catch (StatusRuntimeException e) {
            log.error("核销优惠券异常：userCouponIds={}", userCouponIds, e);
            throw new BizIllegalException(500, "核销优惠券异常", e);
        }
    }

    /**
     * 退还优惠券。
     * 调用失败时抛出业务异常。
     *
     * @param userCouponIds 待退还的优惠券id列表
     */
    public void refundCoupon(List<Long> userCouponIds) {
        try {
            var request = RefundCouponRequest.newBuilder()
                    .addAllUserCouponIds(userCouponIds == null ? List.of() : userCouponIds)
                    .build();
            stub.refundCoupon(request);
        } catch (StatusRuntimeException e) {
            log.error("退还优惠券异常：userCouponIds={}", userCouponIds, e);
            throw new BizIllegalException(500, "退还优惠券异常", e);
        }
    }

    /**
     * 查询优惠券规则。
     * 调用失败时降级返回空列表。
     *
     * @param userCouponIds 用户优惠券id列表
     * @return 优惠券规则列表；异常时返回空列表
     */
    public List<String> queryDiscountRules(List<Long> userCouponIds) {
        try {
            var request = QueryDiscountRulesRequest.newBuilder()
                    .addAllUserCouponIds(userCouponIds == null ? List.of() : userCouponIds)
                    .build();
            return stub.queryDiscountRules(request).getCouponsList();
        } catch (StatusRuntimeException e) {
            log.error("查询促销服务异常：queryDiscountRules, userCouponIds={}", userCouponIds, e);
            return Collections.emptyList();
        }
    }


    /**
     * 将 proto 的 FindDiscountSolutionResponse 转换为业务版 CouponDiscountDTO。
     */
    private CouponDiscountDTO convertFromFindSolution(FindDiscountSolutionResponse proto) {
        if (proto == null) {
            return null;
        }
        var dto = new CouponDiscountDTO();
        dto.setDiscountAmount(proto.getDiscountAmount());
        dto.setDiscountDetail(proto.getDiscountDetailMap());
        return dto;
    }

    /**
     * 将 proto 的 QueryDiscountDetailByOrderResponse 转换为业务版 CouponDiscountDTO。
     */
    private CouponDiscountDTO convertFromQueryDetail(QueryDiscountDetailByOrderResponse proto) {
        if (proto == null) {
            return null;
        }
        var dto = new CouponDiscountDTO();
        dto.setDiscountAmount(proto.getDiscountAmount());
        dto.setDiscountDetail(proto.getDiscountDetailMap());
        return dto;
    }

    /**
     * 将 proto 的 OrderCourseDTO 转换为业务版 OrderCourseDTO。
     * 两个类同名不同包，proto 版用全限定名引用。
     * 注意：proto 生成的类不能用 new 直接构造，业务 DTO 用 new 构造。
     *
     * @param proto proto 版课程对象
     * @return 业务版课程对象；参数为 null 时返回 null
     */
    private OrderCourseDTO convertOrderCourse(
            com.example.tj_project_apimicroservice.proto.OrderCourseDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new OrderCourseDTO();
        dto.setId(proto.getId());
        dto.setCateId(proto.getCateId());
        dto.setPrice(proto.getPrice());
        return dto;
    }
}