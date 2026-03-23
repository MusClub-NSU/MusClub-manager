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
}

