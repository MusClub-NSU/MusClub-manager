package com.nsu.musclub.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends ApiException {

    public ResourceAlreadyExistsException(String resourceName, String field, String value) {
        super(
                String.format("%s с %s='%s' уже существует", resourceName, field, value),
                HttpStatus.CONFLICT,
                "RESOURCE_ALREADY_EXISTS"
        );
    }

    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS");
    }
}
