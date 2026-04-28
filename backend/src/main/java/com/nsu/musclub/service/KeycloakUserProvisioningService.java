package com.nsu.musclub.service;

import com.nsu.musclub.dto.user.UserCreateDto;

public interface KeycloakUserProvisioningService {
    /**
     * Создаёт пользователя в Keycloak и назначает роль realm (MEMBER/ORGANIZER и т.п).
     * Возвращает id пользователя в Keycloak.
     */
    String createUserAndAssignRole(UserCreateDto dto);

    /**
     * Лучший-успех: удаляет пользователя в Keycloak (используется для отката).
     */
    void deleteUserQuietly(String keycloakUserId);

    /**
     * Удаляет пользователя в Keycloak по email/username из нашей БД.
     */
    void deleteUserByProfileQuietly(String email, String username);

    /**
     * Обновляет профиль пользователя в Keycloak (username/email/role/password).
     */
    void updateUserProfile(String oldEmail, String oldUsername, String newUsername, String newEmail, String role, String password);

    /**
     * Сбрасывает пароль пользователя в Keycloak.
     */
    void resetPasswordByProfile(String email, String username, String password);
}

