package com.tjxt.tjcourse.Controller;

import com.tjxt.tjcourse.Model.Vo.CataSimpleInfoVO;
import com.tjxt.tjcourse.Service.ICourseCatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 目录课程相关接口
 **/
@Tag(name = "章节目录", description = "章节目录相关接口")
@RestController
@RequestMapping("catalogues")
public class CatalogueController {

    @Autowired
    private ICourseCatalogueService courseCatalogueService;

    @GetMapping("batchQuery/{ids}")
    @Operation(summary = "根据章节目录批量查询基础信息")
    public List<CataSimpleInfoVO> batchQuery(
            @Parameter(
                    name = "ids",
                    description = "章节目录id列表，多个id以英文逗号分隔",
                    required = true,
                    in = ParameterIn.PATH
            )
            @PathVariable("ids") List<Long> ids) {
        return courseCatalogueService.getManyCataSimpleInfo(ids);
    }

    @GetMapping("querySectionInfoById/{id}")
    @Operation(summary = "获取小节信息")
    public CataSimpleInfoVO querySectionInfoById(
            @Parameter(
                    name = "id",
                    description = "小节id",
                    required = true,
                    in = ParameterIn.PATH
            )
            @PathVariable("id") Long id) {
        return courseCatalogueService.querySectionInfoById(id);
    }
}