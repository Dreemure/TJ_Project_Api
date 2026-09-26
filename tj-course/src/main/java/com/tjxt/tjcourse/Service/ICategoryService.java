package com.tjxt.tjcourse.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tjxt.tjcourse.Entity.Category;
import com.tjxt.tjcourse.Entity.Course;
import com.tjxt.tjcourse.Model.Dto.CategoryAddDTO;
import com.tjxt.tjcourse.Model.Dto.CategoryDisableOrEnableDTO;
import com.tjxt.tjcourse.Model.Dto.CategoryListDTO;
import com.tjxt.tjcourse.Model.Dto.CategoryUpdateDTO;
import com.tjxt.tjcourse.Model.Vo.CategoryInfoVO;
import com.tjxt.tjcourse.Model.Vo.CategoryVO;
import com.tjxt.tjcourse.Model.Vo.SimpleCategoryVO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 课程分类 服务类
 * </p>
 */
public interface ICategoryService extends IService<Category> {

    /**
     * 分页查询课程信息
     *
     * @param categoryPageDTO 分页参数
     * @return 课程分页信息
     */
    List<CategoryVO> list(CategoryListDTO categoryPageDTO);

    /**
     * 新增课程分页
     *
     * @param categoryAddDTO 分类信息
     */
    void add(CategoryAddDTO categoryAddDTO);

    /**
     * 获取课程分类信息
     * @param id 课程id
     * @return 课程分类信息
     */
    CategoryInfoVO get(Long id);

    /**
     * 删除课程分类
     * @param id 分类id
     */
    void delete(Long id);

    /**
     * 课程分类启用或禁用
     */
    void disableOrEnable(CategoryDisableOrEnableDTO categoryDisableOrEnableDTO);

    /**
     * 更新课程分类信息
     */
    void update(CategoryUpdateDTO categoryUpdateDTO);

    /**
     * 获取所有分类的数据及结构
     */
    List<SimpleCategoryVO> all(Boolean admin);

    /**
     * 获取课程分类id和名称
     * @return 课程分类id和名称
     */
    Map<Long, String> getCateIdAndName();

    List<CategoryVO> allOfOneLevel();

    /**
     * 根据课程分类id查询分类列表
     * @param ids  课程分类id
     * @return 分类列表
     */
    List<Category> queryByIds(List<Long> ids);

    /**
     * 根据三级课程分类查询课程分类信息
     * @param thirdCateIdList 三级课程分类
     * @return 课程分类信息
     */
    Map<Long, String> queryByThirdCateIds(@RequestParam("thirdCateIdList") List<Long> thirdCateIdList);

    /**
     * 获取课程分类信息
     *
     * @param course
     * @return
     */
    List<String> queryCourseCategorys(Course course);

    /**
     * 校验课程分类是否符合要求,并按顺序返回一二三级课程分类id列表
     *
     * @param thirdCateId 三级课程分类
     * @return 一二三级课程分类id列表
     */
    List<Long> checkCategory(Long thirdCateId);
}

