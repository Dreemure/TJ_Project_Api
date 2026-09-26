package com.tjxt.tjcourse.Controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tjxt.tjcommon.Model.Dto.PageDTO;
import com.tjxt.tjcommon.Validate.Annotations.ParamChecker;
import com.tjxt.tjcourse.Constants.CourseStatus;
import com.tjxt.tjcourse.Model.Dto.*;
import com.tjxt.tjcourse.Model.Vo.*;
import com.tjxt.tjcourse.Service.*;
import com.tjxt.tjcourse.Utils.CourseSaveBaseGroup;
import com.tjxt.tjmicroservice.Model.Dto.Course.CourseSimpleInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程controller
 **/
@Tag(name = "课程相关接口", description = "课程相关接口")
@RestController
@RequestMapping("courses")
@Slf4j
@Validated
public class CourseController {

    @Autowired
    private ICourseDraftService courseDraftService;

    @Autowired
    private ICourseCatalogueDraftService courseCatalogueDraftService;

    @Autowired
    private ICourseTeacherDraftService courseTeacherDraftService;

    @Autowired
    private ICourseService courseService;

    @Autowired
    private ICourseCatalogueService courseCatalogueService;

    @GetMapping("baseInfo/{id}")
    @Operation(summary = "获取课程基础信息", description = "是否是用于查看页面查看数据，默认是查看，如果不是界面查看数据就是编辑页面使用")
    public CourseBaseInfoVO baseInfo(
            @Parameter(description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id,
            @Parameter(description = "是否查看页面，默认是查看", required = false)
            @RequestParam(value = "see", required = false, defaultValue = "1") Boolean see) {
        return courseDraftService.getCourseBaseInfo(id, see);
    }

    @PostMapping("baseInfo/save")
    @Operation(summary = "保存课程基本信息")
    @ParamChecker
    // 校验非业务限制的字段
    public CourseSaveVO save(
            @RequestBody @Validated(CourseSaveBaseGroup.class) CourseBaseInfoSaveDTO courseBaseInfoSaveDTO) {
        return courseDraftService.save(courseBaseInfoSaveDTO);
    }

    @GetMapping("catas/{id}")
    @Operation(summary = "获取课程的章节")
    public List<CataVO> catas(
            @Parameter(description = "课程id", required = false, in = ParameterIn.PATH)
            @PathVariable(value = "id", required = false) Long id,
            @Parameter(description = "是否查看页面，默认是查看", required = false)
            @RequestParam(value = "see", required = false, defaultValue = "1") Boolean see,
            @Parameter(description = "是否包含练习，默认包含", required = false)
            @RequestParam(value = "withPractice", required = false, defaultValue = "1") Boolean withPractice) {
        return courseCatalogueDraftService.queryCourseCatalogues(id, see, withPractice);
    }

    @PostMapping("catas/save/{id}/{step}")
    @Operation(summary = "保存章节")
    @ParamChecker
    public void catasSave(
            @RequestBody @Validated List<CataSaveDTO> cataSaveDTOS,
            @Parameter(description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id,
            @Parameter(description = "步骤", required = false, in = ParameterIn.PATH)
            @PathVariable(value = "step", required = false) Integer step) {
        courseCatalogueDraftService.save(id, cataSaveDTOS, step);
    }

    @PostMapping("media/save/{id}")
    @Operation(summary = "课程视频")
    public void mediaSave(
            @Parameter(description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id,
            @RequestBody @Valid List<CourseMediaDTO> courseMediaDTOS) {
        courseCatalogueDraftService.saveMediaInfo(id, courseMediaDTOS);
    }

    @PostMapping("subjects/save/{id}")
    @Operation(summary = "保存小节或练习中的题目")
    public void saveSuject(
            @Parameter(description = "小节或练习id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id,
            @RequestBody @Validated List<CataSubjectDTO> cataSubjectDTO) {
        courseCatalogueDraftService.saveSuject(id, cataSubjectDTO);
    }

    @GetMapping("subjects/get/{id}")
    @Operation(summary = "获取小节或练习中的题目（用于编辑）")
    public List<CataSimpleSubjectVO> getSuject(
            @Parameter(description = "小节或练习id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id) {
        return courseCatalogueDraftService.getSuject(id);
    }

    @GetMapping("teachers/{id}")
    @Operation(summary = "查询课程相关的老师信息")
    public List<CourseTeacherVO> teacher(
            @Parameter(description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id,
            @Parameter(description = "是否查看页面，默认是查看", required = false)
            @RequestParam(value = "see", required = false, defaultValue = "1") Boolean see) {
        return courseTeacherDraftService.queryTeacherOfCourse(id, see);
    }

    @PostMapping("teachers/save")
    @Operation(summary = "保存老师信息")
    public void teachersSave(
            @RequestBody @Validated CourseTeacherSaveDTO courseTeacherSaveDTO) {
        courseTeacherDraftService.save(courseTeacherSaveDTO);
    }

    @PostMapping("upShelf")
    @Operation(summary = "课程上架")
    public void upShelf(
            @RequestBody @Validated CourseIdDTO courseIdDTO) {
        courseDraftService.upShelf(courseIdDTO.getId());
    }

    @GetMapping("checkBeforeUpShelf/{id}")
    @Operation(summary = "课程上架前校验")
    public void checkBeforeUpShelf(
            @Parameter(description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id) {
        courseDraftService.checkBeforeUpShelf(id);
    }

    @PostMapping("downShelf")
    @Operation(summary = "课程下架")
    public void downShelf(
            @RequestBody @Validated CourseIdDTO courseIdDTO) {
        courseDraftService.downShelf(courseIdDTO.getId());
    }

    /**
     * 先去删除加上数据删除后，再去删除草稿
     *
     * @param id 课程id
     */
    @DeleteMapping("delete/{id}")
    @Operation(summary = "课程删除")
    public void deleteById(
            @Parameter(description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id) {
        courseService.delete(id);
    }

    @Operation(summary = "根根条件列表获取课程信息")
    @GetMapping("/simpleInfo/list")
    public List<CourseSimpleInfoDTO> getSimpleInfoList(CourseSimpleInfoListDTO courseSimpleInfoListDTO) {
        return courseService.getSimpleInfoList(courseSimpleInfoListDTO);
    }

    @Operation(summary = "根据课程id，查询所有章节的序号")
    @GetMapping("/catas/index/list/{id}")
    public List<CataSimpleInfoVO> catasIndexList(
            @Parameter(description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long id) {
        return courseCatalogueService.getCatasIndexList(id);
    }

    @Operation(summary = "生成练习id")
    @GetMapping("generator")
    public CourseCataIdVO generator() {
        return new CourseCataIdVO(IdWorker.getId());
    }

    @Operation(summary = "管理端课程搜索接口")
    @GetMapping("/page")
    public PageDTO<CoursePageVO> queryForPage(CoursePageQuery coursePageQuery) {
        if (CourseStatus.NO_UP_SHELF.equals(coursePageQuery.getStatus()) ||
                CourseStatus.DOWN_SHELF.equals(coursePageQuery.getStatus())) {
            // 待上架已下架查询草稿
            return courseDraftService.queryForPage(coursePageQuery);
        } else {
            // 已上架已完结查询正式数据
            return courseService.queryForPage(coursePageQuery);
        }
    }

    @Operation(summary = "校验课程名称是否已经存在")
    @GetMapping("/checkName")
    public NameExistVO checkNameExist(
            @Parameter(description = "课程id", required = false)
            @RequestParam(value = "id", required = false) Long id,
            @Parameter(description = "课程名称", required = true)
            @RequestParam(value = "name") String name) {
        return courseService.checkName(name, id);
    }

    @Operation(summary = "查询课程基本信息、目录、学习进度")
    @GetMapping("/{id}/catalogs")
    public CourseAndSectionVO queryCourseAndCatalogById(
            @Parameter(name = "id", description = "课程id", required = true, in = ParameterIn.PATH)
            @PathVariable("id") Long courseId) {
        return courseService.queryCourseAndCatalogById(courseId);
    }
}