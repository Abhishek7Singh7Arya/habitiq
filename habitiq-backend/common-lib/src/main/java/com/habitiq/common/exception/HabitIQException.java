package com.habitiq.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HabitIQException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public HabitIQException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static HabitIQException notFound(String message) {
        return new HabitIQException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static HabitIQException badRequest(String message) {
        return new HabitIQException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static HabitIQException unauthorized(String message) {
        return new HabitIQException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static HabitIQException conflict(String message) {
        return new HabitIQException(message, HttpStatus.CONFLICT, "CONFLICT");
    }
}
