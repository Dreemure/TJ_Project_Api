package com.example.tj_project_apimicroservice.Client;

import com.example.tj_project_apimicroservice.Model.Dto.Exam.QuestionBizDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Exam.QuestionDTO;
import com.example.tj_project_apimicroservice.proto.ExamServiceGrpc;
import com.example.tj_project_apimicroservice.proto.QueryQuestionByIdsRequest;
import com.example.tj_project_apimicroservice.proto.QueryQuestionIdsByBizIdsRequest;
import com.example.tj_project_apimicroservice.proto.QueryQuestionScoresByBizIdsRequest;
import com.example.tj_project_apimicroservice.proto.QueryQuestionScoresRequest;
import com.example.tj_project_apimicroservice.proto.SaveQuestionBizInfoBatchRequest;
import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/*
 * 题目服务 gRPC 客户端封装。
 * 职责：通过 Spring Boot gRPC Starter 注入 Stub，调用 ExamService，
 *       将 proto 对象转换为业务 DTO，并内聚降级逻辑（失败返回默认值）。
 */
@Slf4j
@Component
public class ExamGrpcClient {

    /**
     * 自动注入 Stub。
     * 对应 application.yml 中 spring.grpc.client.channels.* 的通道配置。
     */
    @Autowired
    private ExamServiceGrpc.ExamServiceBlockingStub stub;

    // ==================== 业务方法 ====================

    /**
     * 批量保存题目与业务的关联信息。
     * <p>调用失败时抛出业务异常。
     *
     * @param questionBizInfos 题目与业务的关联信息列表
     */
    public void saveQuestionBizInfoBatch(List<QuestionBizDTO> questionBizInfos) {
        try {
            var builder = SaveQuestionBizInfoBatchRequest.newBuilder();
            if (questionBizInfos != null) {
                for (var biz : questionBizInfos) {
                    builder.addQuestionBizInfos(convertToProtoQuestionBiz(biz));
                }
            }
            stub.saveQuestionBizInfoBatch(builder.build());
        } catch (StatusRuntimeException e) {
            log.error("调用题目服务异常：saveQuestionBizInfoBatch, size={}",
                    questionBizInfos == null ? 0 : questionBizInfos.size(), e);
            throw new RuntimeException("保存题目与业务关联信息异常", e);
        }
    }

    /**
     * 根据业务id列表查询题目id列表。
     * <p>调用失败时降级返回空列表。
     *
     * @param bizIds 业务id列表
     * @return 题目与业务的关联信息列表；异常时返回空列表
     */
    public List<QuestionBizDTO> queryQuestionIdsByBizIds(List<Long> bizIds) {
        try {
            var builder = QueryQuestionIdsByBizIdsRequest.newBuilder();
            if (bizIds != null) {
                builder.addAllBizIds(bizIds);
            }
            var response = stub.queryQuestionIdsByBizIds(builder.build());
            return response.getQuestionBizInfosList().stream()
                    .map(this::convertToBizQuestionBiz)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询题目服务异常：queryQuestionIdsByBizIds, bizIds={}", bizIds, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据业务id列表查询题目总分（bizId → score）。
     * <p>调用失败时降级返回空 Map。
     *
     * @param bizIds 业务id列表
     * @return bizId → 题目总分；异常时返回空 Map
     */
    public Map<Long, Integer> queryQuestionScoresByBizIds(List<Long> bizIds) {
        try {
            var builder = QueryQuestionScoresByBizIdsRequest.newBuilder();
            if (bizIds != null) {
                builder.addAllBizIds(bizIds);
            }
            var response = stub.queryQuestionScoresByBizIds(builder.build());
            return response.getScoresMap();
        } catch (StatusRuntimeException e) {
            log.error("查询题目服务异常：queryQuestionScoresByBizIds, bizIds={}", bizIds, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 根据题目id列表查询题目详情。
     * <p>调用失败时降级返回空列表。
     *
     * @param ids 题目id列表
     * @return 题目详情列表；异常时返回空列表
     */
    public List<QuestionDTO> queryQuestionByIds(List<Long> ids) {
        try {
            var builder = QueryQuestionByIdsRequest.newBuilder();
            if (ids != null) {
                builder.addAllIds(ids);
            }
            var response = stub.queryQuestionByIds(builder.build());
            return response.getQuestionsList().stream()
                    .map(this::convertToBizQuestion)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询题目服务异常：queryQuestionByIds, ids={}", ids, e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询题目分数。
     * <p>调用失败时降级返回空 Map。
     *
     * @param id 题目id
     * @return 题目分数；异常时返回空 Map
     */
    public Map<Long, Integer> queryQuestionScores(Long id) {
        try {
            var request = QueryQuestionScoresRequest.newBuilder()
                    .setId(id)
                    .build();
            var response = stub.queryQuestionScores(request);
            return response.getScoresMap();
        } catch (StatusRuntimeException e) {
            log.error("查询题目服务异常：queryQuestionScores, id={}", id, e);
            return Collections.emptyMap();
        }
    }

    // ==================== proto → 业务 DTO 转换 ====================

    /**
     * 业务版 QuestionBizDTO → proto 版 QuestionBizDTO。
     * <p>这是发送请求时使用，需要把业务对象转为 proto。
     */
    private com.example.tj_project_apimicroservice.proto.QuestionBizDTO convertToProtoQuestionBiz(
            QuestionBizDTO biz) {
        if (biz == null) {
            return com.example.tj_project_apimicroservice.proto.QuestionBizDTO.getDefaultInstance();
        }
        return com.example.tj_project_apimicroservice.proto.QuestionBizDTO.newBuilder()
                .setBizId(biz.getBizId() == null ? 0L : biz.getBizId())
                .setQuestionId(biz.getQuestionId() == null ? 0L : biz.getQuestionId())
                .build();
    }

    /**
     * proto 版 QuestionBizDTO → 业务版 QuestionBizDTO。
     */
    private QuestionBizDTO convertToBizQuestionBiz(
            com.example.tj_project_apimicroservice.proto.QuestionBizDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new QuestionBizDTO();
        dto.setBizId(proto.getBizId());
        dto.setQuestionId(proto.getQuestionId());
        return dto;
    }

    /**
     * proto 版 QuestionDTO → 业务版 QuestionDTO。
     * <p>两个类同名不同包，proto 版用全限定名引用。
     */
    private QuestionDTO convertToBizQuestion(
            com.example.tj_project_apimicroservice.proto.QuestionDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new QuestionDTO();
        dto.setId(proto.getId());
        dto.setName(proto.getName());
        dto.setType(proto.getType());
        dto.setDifficulty(proto.getDifficulty());
        dto.setScore(proto.getScore());
        dto.setOptions(proto.getOptionsList());
        dto.setAnswer(proto.getAnswer());
        dto.setAnalysis(proto.getAnalysis());
        return dto;
    }
}