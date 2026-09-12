package com.example.tj_project_apimicroservice.Client;

import com.example.tj_project_apimicroservice.Model.Dto.Course.CataSimpleInfoDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Course.CatalogueDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Course.CategoryBasicDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Course.CourseFullInfoDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Course.CourseSearchDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Course.MediaQuoteDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Course.SectionInfoDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Course.SubNumAndCourseNumDTO;
import com.example.tj_project_apimicroservice.Model.Dto.Course.SubjectDTO;
import com.example.tj_project_apimicroservice.proto.BatchQueryCatalogueRequest;
import com.example.tj_project_apimicroservice.proto.CourseServiceGrpc;
import com.example.tj_project_apimicroservice.proto.GetCourseInfoByIdRequest;
import com.example.tj_project_apimicroservice.proto.GetSearchInfoRequest;
import com.example.tj_project_apimicroservice.proto.InfoByTeacherIdsRequest;
import com.example.tj_project_apimicroservice.proto.MediaUserInfoRequest;
import com.example.tj_project_apimicroservice.proto.QueryByIdsRequest;
import com.example.tj_project_apimicroservice.proto.SectionInfoRequest;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

// 编译命令：mvn clean compile

/*
 * 课程服务 gRPC 客户端封装。
 * 职责：通过 Spring Boot gRPC Starter 注入 Stub，调用 CourseService，
 *       将 proto 对象转换为业务 DTO，并内聚降级逻辑（失败返回默认值）。
 */
@Slf4j
@Component
public class CourseGrpcClient {

    // Stub：（桩）是 gRPC 生成的"客户端代理对象"，调用它的方法就像调用本地方法一样

    /**
     * 自动注入 Stub。
     * 对应 application.yml 中 spring.grpc.client.channels.* 的通道配置。
     */
    @Autowired
    private CourseServiceGrpc.CourseServiceBlockingStub stub;

    // ==================== 业务方法 ====================

    /**
     * 根据目录id列表批量查询目录信息。
     * <p>调用失败时降级返回空列表。
     *
     * @param ids 目录id列表
     * @return 目录简要信息列表；异常时返回空列表
     */
    public List<CataSimpleInfoDTO> batchQueryCatalogue(List<Long> ids) {
        try {
            var builder = BatchQueryCatalogueRequest.newBuilder();
            if (ids != null) {
                builder.addAllIds(ids);
            }
            var response = stub.batchQueryCatalogue(builder.build());
            return response.getCataSimpleInfosList().stream()
                    .map(this::convertToBizCataSimpleInfo)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询课程服务异常：batchQueryCatalogue, ids={}", ids, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有一级分类。
     * <p>调用失败时降级返回空列表。
     *
     * @return 一级分类列表；异常时返回空列表
     */
    public List<CategoryBasicDTO> getAllOfOneLevel() {
        try {
            var response = stub.getAllOfOneLevel(Empty.getDefaultInstance());
            return response.getCategoriesList().stream()
                    .map(this::convertToBizCategory)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询课程服务异常：getAllOfOneLevel", e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据老师id列表获取老师出题数据和讲课数据。
     * <p>调用失败时降级返回空列表。
     *
     * @param teacherIds 老师id列表
     * @return 老师课程数与出题数统计；异常时返回空列表
     */
    public List<SubNumAndCourseNumDTO> infoByTeacherIds(List<Long> teacherIds) {
        try {
            var builder = InfoByTeacherIdsRequest.newBuilder();
            if (teacherIds != null) {
                builder.addAllTeacherIds(teacherIds);
            }
            var response = stub.infoByTeacherIds(builder.build());
            return response.getSubNumAndCourseList().stream()
                    .map(this::convertToBizSubNumAndCourseNum)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询课程服务异常：infoByTeacherIds, teacherIds={}", teacherIds, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据小节id获取小节对应的 mediaId 和课程id。
     * <p>调用失败时降级返回 null。
     *
     * @param sectionId 小节id
     * @return 小节信息；异常时返回 null
     */
    public SectionInfoDTO sectionInfo(Long sectionId) {
        try {
            var request = SectionInfoRequest.newBuilder()
                    .setSectionId(sectionId)
                    .build();
            var protoResponse = stub.sectionInfo(request);
            return convertToBizSectionInfo(protoResponse);
        } catch (StatusRuntimeException e) {
            log.error("查询课程服务异常：sectionInfo, sectionId={}", sectionId, e);
            return null;
        }
    }

    /**
     * 根据媒资id列表查询媒资被引用的次数。
     * <p>调用失败时降级返回空列表。
     *
     * @param mediaIds 媒资id列表
     * @return 媒资引用信息列表；异常时返回空列表
     */
    public List<MediaQuoteDTO> mediaUserInfo(List<Long> mediaIds) {
        try {
            var builder = MediaUserInfoRequest.newBuilder();
            if (mediaIds != null) {
                builder.addAllMediaIds(mediaIds);
            }
            var response = stub.mediaUserInfo(builder.build());
            return response.getMediaQuotesList().stream()
                    .map(this::convertToBizMediaQuote)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询课程服务异常：mediaUserInfo, mediaIds={}", mediaIds, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据课程id查询索引库需要的数据。
     * <p>调用失败时降级返回 null。
     *
     * @param id 课程id
     * @return 课程搜索信息；异常时返回 null
     */
    public CourseSearchDTO getSearchInfo(Long id) {
        try {
            var request = GetSearchInfoRequest.newBuilder()
                    .setId(id)
                    .build();
            var protoResponse = stub.getSearchInfo(request);
            return convertToBizCourseSearch(protoResponse);
        } catch (StatusRuntimeException e) {
            log.error("查询课程服务异常：getSearchInfo, id={}", id, e);
            return null;
        }
    }

    /**
     * 根据课程id获取课程、目录、教师信息。
     * <p>调用失败时降级返回 null。
     *
     * @param id            课程id
     * @param withCatalogue 是否包含目录
     * @param withTeachers  是否包含教师信息
     * @return 课程完整信息；异常时返回 null
     */
    public CourseFullInfoDTO getCourseInfoById(Long id, boolean withCatalogue, boolean withTeachers) {
        try {
            var request = GetCourseInfoByIdRequest.newBuilder()
                    .setId(id)
                    .setWithCatalogue(withCatalogue)
                    .setWithTeachers(withTeachers)
                    .build();
            var protoResponse = stub.getCourseInfoById(request);
            return convertToBizCourseFullInfo(protoResponse);
        } catch (StatusRuntimeException e) {
            log.error("查询课程服务异常：getCourseInfoById, id={}", id, e);
            return null;
        }
    }

    /**
     * 根据题目id列表查询题目信息。
     * <p>调用失败时降级返回空列表。
     *
     * @param ids 题目id列表
     * @return 题目列表；异常时返回空列表
     */
    public List<SubjectDTO> queryByIds(List<Long> ids) {
        try {
            var builder = QueryByIdsRequest.newBuilder();
            if (ids != null) {
                builder.addAllIds(ids);
            }
            var response = stub.queryByIds(builder.build());
            return response.getSubjectsList().stream()
                    .map(this::convertToBizSubject)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("查询课程服务异常：queryByIds, ids={}", ids, e);
            return Collections.emptyList();
        }
    }

    // ==================== proto → 业务 DTO 转换 ====================

    /**
     * proto 版 CataSimpleInfoDTO → 业务版 CataSimpleInfoDTO。
     */
    private CataSimpleInfoDTO convertToBizCataSimpleInfo(
            com.example.tj_project_apimicroservice.proto.CataSimpleInfoDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new CataSimpleInfoDTO();
        dto.setId(proto.getId());
        dto.setName(proto.getName());
        dto.setCIndex(proto.getCIndex());
        return dto;
    }

    /**
     * proto 版 CategoryBasicDTO → 业务版 CategoryBasicDTO。
     */
    private CategoryBasicDTO convertToBizCategory(
            com.example.tj_project_apimicroservice.proto.CategoryBasicDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new CategoryBasicDTO();
        dto.setId(proto.getId());
        dto.setName(proto.getName());
        dto.setParentId(proto.getParentId());
        return dto;
    }

    /**
     * proto 版 SubNumAndCourseNumDTO → 业务版 SubNumAndCourseNumDTO。
     */
    private SubNumAndCourseNumDTO convertToBizSubNumAndCourseNum(
            com.example.tj_project_apimicroservice.proto.SubNumAndCourseNumDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new SubNumAndCourseNumDTO();
        dto.setTeacherId(proto.getTeacherId());
        dto.setCourseNum(proto.getCourseNum());
        dto.setSubjectNum(proto.getSubjectNum());
        return dto;
    }

    /**
     * proto 版 SectionInfoDTO → 业务版 SectionInfoDTO。
     */
    private SectionInfoDTO convertToBizSectionInfo(
            com.example.tj_project_apimicroservice.proto.SectionInfoDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new SectionInfoDTO();
        dto.setCourseId(proto.getCourseId());
        dto.setMediaId(proto.getMediaId());
        dto.setTrailer(proto.getTrailer());
        dto.setFreeDuration(proto.getFreeDuration());
        return dto;
    }

    /**
     * proto 版 MediaQuoteDTO → 业务版 MediaQuoteDTO。
     */
    private MediaQuoteDTO convertToBizMediaQuote(
            com.example.tj_project_apimicroservice.proto.MediaQuoteDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new MediaQuoteDTO();
        dto.setMediaId(proto.getMediaId());
        dto.setQuoteNum(proto.getQuoteNum());
        return dto;
    }

    /**
     * proto 版 CourseSearchDTO → 业务版 CourseSearchDTO。
     */
    private CourseSearchDTO convertToBizCourseSearch(
            com.example.tj_project_apimicroservice.proto.CourseSearchDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new CourseSearchDTO();
        dto.setId(proto.getId());
        dto.setName(proto.getName());
        dto.setCategoryIdLv1(proto.getCategoryIdLv1());
        dto.setCategoryIdLv2(proto.getCategoryIdLv2());
        dto.setCategoryIdLv3(proto.getCategoryIdLv3());
        dto.setCoverUrl(proto.getCoverUrl());
        dto.setPrice(proto.getPrice());
        dto.setFree(proto.getFree());
        dto.setPublishTime(convertTimestamp(proto.getPublishTime()));
        dto.setSections(proto.getSections());
        dto.setDuration(proto.getDuration());
        dto.setTeacher(proto.getTeacher());
        dto.setCourseType(proto.getCourseType());
        dto.setSold(proto.getSold());
        dto.setScore(proto.getScore());
        return dto;
    }

    /**
     * proto 版 CourseFullInfoDTO → 业务版 CourseFullInfoDTO。
     */
    private CourseFullInfoDTO convertToBizCourseFullInfo(
            com.example.tj_project_apimicroservice.proto.CourseFullInfoDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new CourseFullInfoDTO();
        dto.setId(proto.getId());
        dto.setName(proto.getName());
        dto.setCoverUrl(proto.getCoverUrl());
        dto.setPrice(proto.getPrice());
        dto.setFirstCateId(proto.getFirstCateId());
        dto.setSecondCateId(proto.getSecondCateId());
        dto.setThirdCateId(proto.getThirdCateId());
        dto.setSectionNum(proto.getSectionNum());
        dto.setPurchaseEndTime(convertTimestamp(proto.getPurchaseEndTime()));
        dto.setValidDuration(proto.getValidDuration());
        dto.setTeacherIds(proto.getTeacherIdsList());
        // ⚠️ 如果业务版 CourseFullInfoDTO 有 chapters 字段，需要补充转换
        return dto;
    }

    /**
     * proto 版 CatalogueDTO → 业务版 CatalogueDTO（递归）。
     */
    private CatalogueDTO convertToBizCatalogue(
            com.example.tj_project_apimicroservice.proto.CatalogueDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new CatalogueDTO();
        dto.setId(proto.getId());
        dto.setIndex(proto.getIndex());
        dto.setName(proto.getName());
        dto.setMediaDuration(proto.getMediaDuration());
        dto.setTrailer(proto.getTrailer());
        dto.setMediaName(proto.getMediaName());
        dto.setMediaId(proto.getMediaId());
        dto.setType(proto.getType());
        dto.setSubjectNum(proto.getSubjectNum());
        dto.setTotalScore(proto.getTotalScore());
        dto.setCanUpdate(proto.getCanUpdate());
        // 递归转换子节点
        dto.setSections(proto.getSectionsList().stream()
                .map(this::convertToBizCatalogue)
                .toList());
        return dto;
    }

    /**
     * proto 版 SubjectDTO → 业务版 SubjectDTO。
     */
    private SubjectDTO convertToBizSubject(
            com.example.tj_project_apimicroservice.proto.SubjectDTO proto) {
        if (proto == null) {
            return null;
        }
        var dto = new SubjectDTO();
        dto.setId(proto.getId());
        dto.setName(proto.getName());
        dto.setOptions(proto.getOptionsList());
        dto.setScore(proto.getScore());
        dto.setSubjectType(proto.getSubjectType());
        dto.setDifficulty(proto.getDifficulty());
        dto.setAnalysis(proto.getAnalysis());
        dto.setAnswers(proto.getAnswersList());
        return dto;
    }

    // ==================== 工具方法 ====================

    /**
     * 将 proto 的 Timestamp 转换为 LocalDateTime。
     *
     * @param timestamp proto 时间戳
     * @return LocalDateTime；参数为 null 或默认值时返回 null
     */
    private LocalDateTime convertTimestamp(Timestamp timestamp) {
        if (timestamp == null
                || (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0)) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneId.systemDefault()
        );
    }
}