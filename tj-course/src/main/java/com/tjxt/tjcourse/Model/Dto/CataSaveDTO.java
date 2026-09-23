package com.tjxt.tjcourse.Model.Dto;

import com.tjxt.tjcommon.Exceptions.BadRequestException;
import com.tjxt.tjcommon.Utils.CollUtils;
import com.tjxt.tjcommon.Utils.StringUtils;
import com.tjxt.tjcommon.Validate.Checker;
import com.tjxt.tjcourse.Constants.CourseConstants;
import com.tjxt.tjcourse.Constants.CourseErrorInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "章节")
public class CataSaveDTO implements Checker {

    @Schema(description = "章、节、练习id")
    private Long id;

    @Schema(description = "目录类型1：章,2：节,3：测试")
    @NotNull(message = "")
    private Integer type;

    @Schema(description = "章节练习名称")
    private String name;

    @Schema(description = "章排序，章一定要传，小节和练习不需要传")
    private Integer index;

    @Schema(description = "当前章的小节或练习")
    @Size(min = 1, message = "不能出现空章")
    private List<CataSaveDTO> sections;

    @Override
    public void check(){
        // 名称为空校验
        if(type == CourseConstants.CataType.CHAPTER && StringUtils.isEmpty(name)){
            throw new BadRequestException(CourseErrorInfo.Msg.COURSE_CATAS_SAVE_NAME_NULL);
        }else if(StringUtils.isEmpty(name)){
            throw new BadRequestException(CourseErrorInfo.Msg.COURSE_CATAS_SAVE_NAME_NULL2);
        }

        // 名称长度问题
        if(type == CourseConstants.CataType.CHAPTER && name.length() > 30){
            throw new BadRequestException(CourseErrorInfo.Msg.COURSE_CATAS_SAVE_NAME_SIZE);
        }else if(name.length() > 30){
            throw new BadRequestException(CourseErrorInfo.Msg.COURSE_CATAS_SAVE_NAME_SIZE2);
        }
        if(CollUtils.isEmpty(sections)){
            throw new BadRequestException("不能出现空章");
        }
    }
}
