package com.tjxt.tjcourse.Handler;

import com.tjxt.tjcourse.Service.ICourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

@Component
@Slf4j
public class CourseFinishedProcessor implements BasicProcessor { // PowerJob 中一个 Processor 类对应一个任务

    @Autowired
    private ICourseService courseService;

    @Override
    public ProcessResult process(TaskContext context) {
        try {
            courseService.courseFinished();
            return new ProcessResult(true, "课程完结任务执行成功");
        } catch (Exception e) {
            log.error("courseFinished 任务执行异常", e);
            return new ProcessResult(false, "执行失败：" + e.getMessage());
        }
    }
}