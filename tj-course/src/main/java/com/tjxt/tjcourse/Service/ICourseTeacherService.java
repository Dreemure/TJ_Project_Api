package com.tjxt.tjcourse.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tjxt.tjcourse.Entity.CourseTeacher;
import com.tjxt.tjcourse.Model.Vo.CourseTeacherVO;

import java.util.List;

/**
 * <p>
 * 课程老师关系表草稿 服务类
 * </p>
 */
public interface ICourseTeacherService extends IService<CourseTeacher> {

    /**
     * 查询老师课程信息
     * @param couserId 课程id
     * @return 教师信息
     */
    List<CourseTeacherVO> queryTeachers(Long couserId);

    void deleteByCourseId(Long courserId);

    /**
     * 根据课程id获取老师id列表，并且排序
     * @param courseId 课程id
     * @return 教师信息
     */
    List<Long> getTeacherIdOfCourse(Long courseId);


}

