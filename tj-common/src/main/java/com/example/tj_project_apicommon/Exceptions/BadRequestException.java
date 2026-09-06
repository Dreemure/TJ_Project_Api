package com.example.tj_project_apicommon.Exceptions;

public class BadRequestException extends CommonException {
    private final int status = 400;

    public BadRequestException(String message) {
        super(400, message);
    }

    public BadRequestException(int code, String message) {
        super(code, message);
    }

    public BadRequestException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
