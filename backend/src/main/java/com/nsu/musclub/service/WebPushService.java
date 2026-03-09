package com.nsu.musclub.service;

import com.nsu.musclub.domain.PushSubscription;
import com.nsu.musclub.dto.push.PushMessageDto;

/**
 * Сервис отправки Web Push уведомлений
 */
public interface WebPushService {

    /**
     * Отправить push-уведомление на конкретную подписку
     *
     * @param subscription подписка пользователя
     * @param message содержимое уведомления
     * @return true если отправлено успешно
     */
    boolean sendPushNotification(PushSubscription subscription, PushMessageDto message);

    /**
     * Отправить push-уведомление всем подпискам пользователя
     *
     * @param userId ID пользователя
     * @param message содержимое уведомления
     * @return количество успешно отправленных уведомлений
     */
    int sendPushToUser(Long userId, PushMessageDto message);

    /**
     * Получить VAPID публичный ключ для клиента
     */
    String getVapidPublicKey();
}
