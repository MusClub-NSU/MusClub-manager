package com.nsu.musclub.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO для стандартного ответа об ошибке
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String errorCode;
    private final String message;
    private final String path;
    private final List<FieldError> fieldErrors;

    public ErrorResponse(int status, String error, String errorCode, String message, String path) {
        this(status, error, errorCode, message, path, null);
    }

    public ErrorResponse(int status, String error, String errorCode, String message, String path, List<FieldError> fieldErrors) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Ошибка валидации конкретного поля
     */
    public record FieldError(String field, String message, Object rejectedValue) {
    }
}
