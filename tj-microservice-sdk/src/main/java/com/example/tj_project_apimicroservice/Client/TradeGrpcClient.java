package com.example.tj_project_apimicroservice.Client;

import com.example.tj_project_apimicroservice.Model.Dto.Course.CoursePurchaseInfoDTO;
import com.example.tj_project_apimicroservice.proto.CheckMyLessonRequest;
import com.example.tj_project_apimicroservice.proto.CountEnrollCourseOfStudentRequest;
import com.example.tj_project_apimicroservice.proto.CountEnrollNumOfCourseRequest;
import com.example.tj_project_apimicroservice.proto.GetPurchaseInfoOfCourseRequest;
import com.example.tj_project_apimicroservice.proto.TradeServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 编译命令：mvn clean compile

/*
 * 交易服务 gRPC 客户端封装。
 * 职责：通过 Spring Boot gRPC Starter 注入 Stub，调用 TradeService，
 *       将 proto 对象转换为业务对象，并内聚降级逻辑（失败返回默认值）。
 */
@Slf4j
@Component
public class TradeGrpcClient {

    /**
     * 自动注入 Stub。
     * 对应 application.yml 中 spring.grpc.client.channels.* 的通道配置。
     */
    @Autowired
    private TradeServiceGrpc.TradeServiceBlockingStub stub;

    // ==================== 业务方法 ====================

    /**
     * 批量统计课程的报名人数。
     * <p>调用失败时降级返回空 Map。
     *
     * @param courseIdList 课程id列表
     * @return 课程id → 报名人数；异常时返回空 Map
     */
    public Map<Long, Integer> countEnrollNumOfCourse(List<Long> courseIdList) {
        try {
            var builder = CountEnrollNumOfCourseRequest.newBuilder();
            if (courseIdList != null) {
                builder.addAllCourseIdList(courseIdList);
            }
            var response = stub.countEnrollNumOfCourse(builder.build());
            return response.getEnrollNumMap();
        } catch (StatusRuntimeException e) {
            log.error("查询交易服务异常：countEnrollNumOfCourse, courseIdList={}", courseIdList, e);
            return new HashMap<>();
        }
    }

    /**
     * 批量统计学生报名的课程数量。
     * <p>调用失败时降级返回空 Map。
     *
     * @param studentIds 学生id列表
     * @return 学生id → 报名课程数；异常时返回空 Map
     */
    public Map<Long, Integer> countEnrollCourseOfStudent(List<Long> studentIds) {
        try {
            var builder = CountEnrollCourseOfStudentRequest.newBuilder();
            if (studentIds != null) {
                builder.addAllStudentIds(studentIds);
            }
            var response = stub.countEnrollCourseOfStudent(builder.build());
            return response.getEnrollCourseMap();
        } catch (StatusRuntimeException e) {
            log.error("查询交易服务异常：countEnrollCourseOfStudent, studentIds={}", studentIds, e);
            return new HashMap<>();
        }
    }

    /**
     * 判断当前用户是否已购买指定课程。
     * <p>调用失败时降级返回 false。
     *
     * @param id 课程id
     * @return true：已购买；false：未购买或调用异常
     */
    public Boolean checkMyLesson(Long id) {
        try {
            var request = CheckMyLessonRequest.newBuilder()
                    .setId(id)
                    .build();
            return stub.checkMyLesson(request).getValid();
        } catch (StatusRuntimeException e) {
            log.error("查询交易服务异常：checkMyLesson, id={}", id, e);
            return false;
        }
    }

    /**
     * 获取课程的购买统计信息。
     * <p>调用失败时降级返回空对象。
     *
     * @param courseId 课程id
     * @return 课程购买统计信息；异常时返回空对象
     */
    public CoursePurchaseInfoDTO getPurchaseInfoOfCourse(Long courseId) {
        try {
            var request = GetPurchaseInfoOfCourseRequest.newBuilder()
                    .setCourseId(courseId)
                    .build();
            var protoResponse = stub.getPurchaseInfoOfCourse(request);
            return convertToBizDTO(protoResponse);
        } catch (StatusRuntimeException e) {
            log.error("查询交易服务异常：getPurchaseInfoOfCourse, courseId={}", courseId, e);
            return new CoursePurchaseInfoDTO();
        }
    }


    /**
     * 将 proto 的 CoursePurchaseInfoDTO 转换为业务版 CoursePurchaseInfoDTO。
     * <p>两个类同名不同包，proto 版用全限定名引用。
     * <p>注意：proto 生成的类不能用 new 直接构造，业务 DTO 用 new 构造。
     *
     * @param proto proto 版的响应对象
     * @return 业务版 DTO；参数为 null 时返回空对象
     */
    private CoursePurchaseInfoDTO convertToBizDTO(
            com.example.tj_project_apimicroservice.proto.CoursePurchaseInfoDTO proto) {
        if (proto == null) {
            return new CoursePurchaseInfoDTO();
        }
        // 这里 new 的是业务版 CoursePurchaseInfoDTO（通过 import 引入的那个）
        var dto = new CoursePurchaseInfoDTO();
        dto.setEnrollNum(proto.getEnrollNum());
        dto.setRefundNum(proto.getRefundNum());
        dto.setRealPayAmount(proto.getRealPayAmount());
        return dto;
    }
}