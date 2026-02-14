package com.nsu.musclub.service;

import com.nsu.musclub.dto.push.PushSubscriptionDto;
import com.nsu.musclub.dto.push.PushSubscriptionResponseDto;

import java.util.List;

/**
 * Сервис управления push-подписками
 */
public interface PushSubscriptionService {

    /**
     * Создать или обновить push-подписку
     */
    PushSubscriptionResponseDto subscribe(PushSubscriptionDto dto);

    /**
     * Отписать устройство от push-уведомлений
     */
    void unsubscribe(String endpoint);

    /**
     * Отписать все устройства пользователя
     */
    void unsubscribeAllForUser(Long userId);

    /**
     * Получить все активные подписки пользователя
     */
    List<PushSubscriptionResponseDto> getSubscriptionsForUser(Long userId);

    /**
     * Проверить, подписан ли пользователь
     */
    boolean isUserSubscribed(Long userId);

    /**
     * Получить количество активных подписок пользователя
     */
    long getSubscriptionCount(Long userId);
}
