package com.nsu.musclub.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение: ресурс не найден
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(
                String.format("%s с id=%d не найден", resourceName, id),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
        );
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(
                String.format("%s '%s' не найден", resourceName, identifier),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
        );
    }
}
