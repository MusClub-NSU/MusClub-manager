package com.nsu.musclub.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение: ошибка при отправке push-уведомления
 */
public class PushNotificationException extends ApiException {

    public PushNotificationException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "PUSH_NOTIFICATION_ERROR");
    }

    public PushNotificationException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "PUSH_NOTIFICATION_ERROR", cause);
    }
}
