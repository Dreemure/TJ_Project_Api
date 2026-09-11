package com.example.tj_project_apimicroservice.Client;


import com.example.tj_project_apimicroservice.Model.Dto.Leanring.LearningLessonDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Leanring.LearningRecordDTO;
import com.example.tj_project_apimicroservice.proto.CountLearningLessonByCourseRequest;
import com.example.tj_project_apimicroservice.proto.IsLessonValidRequest;
import com.example.tj_project_apimicroservice.proto.LearningServiceGrpc;
import com.example.tj_project_apimicroservice.proto.QueryLearningRecordByCourseRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.stereotype.Component;

import java.util.List;

//编译命令：mvn clean compile

@Slf4j
@Component
@ImportGrpcClients(basePackages = "com.example.tj_project_apimicroservice.proto") //定位包名
public class LearningGrpcClient {

    /*
     * 自动注入 Stub。
     * 值 "learning-service" 对应 application.yml 中 spring.grpc.client.channels.learning-service 配置。
     */
    @Autowired
    private LearningServiceGrpc.LearningServiceBlockingStub stub;

    /*
     * 统计课程的学习课表数量。
     * 调用失败时降级返回 0。
     *
     * @param courseId 课程id
     * @return 学习课表数量；异常时返回 0
     */
    public Integer countLearningLessonByCourse(Long courseId) {
        try {
            var request = CountLearningLessonByCourseRequest.newBuilder()
                    .setCourseId(courseId)
                    .build();
            return stub.countLearningLessonByCourse(request).getCount();
        } catch (StatusRuntimeException e) {
            log.error("查询学习服务异常：countLearningLessonByCourse, courseId={}", courseId, e);
            return 0;   // 降级默认值
        }
    }

    /*
     * 判断课程学习是否有效。
     * 调用失败时降级返回 null。
     *
     * @param courseId 课程id
     * @return 有效的课表id；异常时返回 null
     */
    public Long isLessonValid(Long courseId) {
        try {
            var request = IsLessonValidRequest.newBuilder()
                    .setCourseId(courseId)
                    .build();
            return stub.isLessonValid(request).getLessonId();
        } catch (StatusRuntimeException e) {
            log.error("查询学习服务异常：isLessonValid, courseId={}", courseId, e);
            return null;  // 降级默认值
        }
    }

    /*
     * 根据课程id查询学习记录。
     * 调用失败时降级返回 null。
     *
     * @param courseId 课程id
     * @return 学习课表进度信息；异常时返回 null
     */
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        try {
            var request = QueryLearningRecordByCourseRequest.newBuilder()
                    .setCourseId(courseId)
                    .build();
            // proto 版 LearningLessonDTO 与业务版同名不同包，用全限定名引用
            var protoResponse = stub.queryLearningRecordByCourse(request);
            convertToBizDTO(protoResponse);
            return null;
        } catch (StatusRuntimeException e) {
            log.error("查询学习服务异常：queryLearningRecordByCourse, courseId={}", courseId, e);
            return null;  // 降级默认值
        }
    }

    // ==================== proto → 业务 DTO 转换 ====================

    /*
     * 将 proto 的 LearningLessonDTO 转换为业务版 LearningLessonDTO。
     * 两个类同名不同包，proto 版用全限定名引用。
     */
    private void convertToBizDTO(
            com.example.tj_project_apimicroservice.proto.LearningLessonDTO proto) {
        if (proto == null) {
            return;
        }
        var dto = new LearningLessonDTO();
        dto.setId(proto.getId());
        dto.setLatestSectionId(proto.getLatestSectionId());
        dto.setRecords(convertRecords(proto.getRecordsList()));
    }

    /*
     * 批量转换学习记录。
     */
    private List<LearningRecordDTO> convertRecords(
            List<com.example.tj_project_apimicroservice.proto.LearningRecordDTO> protoRecords) {
        return protoRecords.stream().map(proto -> {
            var record = new LearningRecordDTO();
            record.setSectionId(proto.getSectionId());
            record.setMoment(proto.getMoment());
            record.setFinished(proto.getFinished());
            return record;
        }).toList();
    }
}
