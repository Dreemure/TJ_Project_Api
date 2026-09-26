package com.tjxt.tjcourse.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tjxt.tjcourse.Entity.CourseCataSubjectDraft;

/**
 * <p>
 * 课程-题目关系表草稿 服务类
 * </p>
 */
public interface ICourseCataSubjectDraftService extends IService<CourseCataSubjectDraft> {
    /**
     * 删除不在的课程小节目录
     * @param courseId
     */
    void deleteNotInCataIdList(Long courseId);
}

