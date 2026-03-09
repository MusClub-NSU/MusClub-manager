package com.nsu.musclub.dto.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO для создания push-подписки
 */
public class PushSubscriptionDto {

    @NotNull(message = "ID пользователя обязателен")
    private Long userId;

    @NotBlank(message = "Endpoint обязателен")
    private String endpoint;

    @NotBlank(message = "P256dh ключ обязателен")
    private String p256dh;

    @NotBlank(message = "Auth ключ обязателен")
    private String auth;

    private String userAgent;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public void setP256dh(String p256dh) {
        this.p256dh = p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
