package com.nsu.musclub.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение: неверный запрос (валидация бизнес-логики)
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public BadRequestException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
