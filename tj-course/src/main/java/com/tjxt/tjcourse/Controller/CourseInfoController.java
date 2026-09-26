package com.tjxt.tjcourse.Controller;

import com.tjxt.tjcommon.Utils.CollUtils;
import com.tjxt.tjcourse.Service.ICategoryService;
import com.tjxt.tjcourse.Service.ICourseCatalogueService;
import com.tjxt.tjcourse.Service.ICourseService;
import com.tjxt.tjmicroservice.Model.Dto.Course.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内部服务接口调用
 **/
@RestController
@RequestMapping("course")
@Tag(name = "课程相关接口", description = "课程相关接口，内部调用")
public class CourseInfoController {

    @Autowired
    private ICourseCatalogueService courseCatalogueService;

    @Autowired
    private ICourseService courseService;

    @Autowired
    private ICategoryService categoryService;

    @GetMapping("infoByTeacherIds")
    @Operation(summary = "通过老师id获取老师负责的课程和出的题目数量")
    public List<SubNumAndCourseNumDTO> infoByTeacherIds(
            @Parameter(description = "老师id列表", required = true)
            @RequestParam("teacherIds") List<Long> teacherIds) {

        if (CollUtils.isEmpty(teacherIds)) {
            return new ArrayList<>();
        }
        return courseService.countSubjectNumAndCourseNumOfTeacher(teacherIds);
    }

    /**
     * 根据小节id获取小节对应的mediaId和课程id
     *
     * @param sectionId 小节id
     * @return 小节对应的mediaId和课程id
     */
    @GetMapping("/section/{id}")
    @Operation(summary = "根据小节id获取小节对应的mediaId和课程id")
    public SectionInfoDTO sectionInfo(
            @Parameter(
                    name = "id",
                    description = "小节id，不支持章id或者练习id查询",
                    required = true,
                    in = ParameterIn.PATH
            )
            @PathVariable("id") Long sectionId
    ) {
        return courseCatalogueService.getSimpleSectionInfo(sectionId);
    }

    /**
     * 根据媒资Id列表查询媒资被引用的次数
     *
     * @param mediaIds 媒资id列表
     * @return 媒资id和媒资被引用的次数的列表
     */
    @GetMapping("/media/useInfo")
    @Operation(summary = "根据媒资Id列表查询媒资被引用的次数")
    public List<MediaQuoteDTO> mediaUserInfo(
            @Parameter(description = "媒资id列表", required = true)
            @RequestParam("mediaIds") List<Long> mediaIds) {
        return courseCatalogueService.countMediaUserInfo(mediaIds);
    }

    @GetMapping("/{id}/searchInfo")
    @Operation(summary = "课程上架时，需要查询课程信息，加入索引库")
    public CourseDTO getSearchInfo(
            @Parameter(
                    name = "id",
                    description = "课程id",
                    required = true,
                    in = ParameterIn.PATH
            )
            @PathVariable("id") Long id) {
        return courseService.getCourseDTOById(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取课程信息")
    public CourseFullInfoDTO getById(
            @Parameter(description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id,

            @Parameter(description = "是否包含目录", required = false)
            @RequestParam(value = "withCatalogue", required = false) boolean withCatalogue,

            @Parameter(description = "是否包含教师", required = false)
            @RequestParam(value = "withTeachers", required = false) boolean withTeachers) {
        return courseService.getInfoById(id, withCatalogue, withTeachers);
    }

    @GetMapping("/getCateNameMap")
    @Hidden
    public Map<Long, String> queryByThirdCateIds(
            @Parameter(description = "三级分类id列表", required = true)
            @RequestParam("thirdCateIdList") List<Long> thirdCateIdList) {
        return categoryService.queryByThirdCateIds(thirdCateIdList);
    }

    @GetMapping("/name")
    @Operation(summary = "根据课程名称查询课程id列表")
    public List<Long> queryCoursesIdByName(
            @Parameter(description = "课程名称", required = true)
            @RequestParam("name") String name) {
        return courseService.queryCourseIdByName(name);
    }
}