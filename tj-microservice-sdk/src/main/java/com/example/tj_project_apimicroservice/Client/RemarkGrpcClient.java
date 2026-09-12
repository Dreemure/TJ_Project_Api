package com.example.tj_project_apimicroservice.Client;

import com.example.tj_project_apimicroservice.proto.IsBizLikedRequest;
import com.example.tj_project_apimicroservice.proto.RemarkServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * 点赞服务 gRPC 客户端封装。
 * 职责：通过 Spring Boot gRPC Starter 注入 Stub，调用 RemarkService，
 *       将 proto 响应转换为业务 Set，并内聚降级逻辑（失败返回空 Set）。
 */
@Slf4j
@Component
public class RemarkGrpcClient {

    /**
     * 自动注入 Stub。
     * 对应 application.yml 中 spring.grpc.client.channels.* 的通道配置。
     */
    @Autowired
    private RemarkServiceGrpc.RemarkServiceBlockingStub stub;

    /**
     * 批量判断业务对象是否被当前用户点赞。
     * <p>调用失败时降级返回空 Set。
     *
     * @param bizIds 待判断的业务id列表（如小节id、课程id、笔记id等）
     * @return 已点赞的业务id集合；异常时返回空 Set
     */
    public Set<Long> isBizLiked(Iterable<Long> bizIds) {
        try {
            var request = IsBizLikedRequest.newBuilder()
                    .addAllBizIds(toList(bizIds))
                    .build();
            var response = stub.isBizLiked(request);
            // proto 返回 List<Long>，业务需要 Set<Long>，转一下
            return new HashSet<>(response.getLikedBizIdsList());
        } catch (StatusRuntimeException e) {
            log.error("查询点赞服务异常：isBizLiked, bizIds={}", bizIds, e);
            return Collections.emptySet();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 将 Iterable<Long> 转为 List<Long>。
     * <p>若传入的是 List，直接返回；否则遍历收集。
     */
    private List<Long> toList(Iterable<Long> iterable) {
        if (iterable == null) {
            return List.of();
        }
        if (iterable instanceof List<Long> list) {
            return list;
        }
        var list = new java.util.ArrayList<Long>();
        for (var id : iterable) {
            list.add(id);
        }
        return list;
    }
}
