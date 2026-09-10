package com.example.tj_project_apicommon.Utils;

import com.example.tj_project_apicommon.Exceptions.BadRequestException;
import jakarta.validation.ConstraintViolation;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 手动执行Violation处理校验结果
 **/
public class ViolationUtils {

    public static <T> void process(Set<ConstraintViolation<T>> violations) {
        if(CollUtils.isEmpty(violations)){
            return;
        }
        String message = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("|"));
        throw new BadRequestException(message);
    }
}
