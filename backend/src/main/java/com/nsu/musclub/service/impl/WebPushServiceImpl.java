package com.nsu.musclub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsu.musclub.config.PushNotificationConfig;
import com.nsu.musclub.domain.PushSubscription;
import com.nsu.musclub.dto.push.PushMessageDto;
import com.nsu.musclub.repository.PushSubscriptionRepository;
import com.nsu.musclub.service.WebPushService;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

@Service
public class WebPushServiceImpl implements WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushServiceImpl.class);

    private final PushNotificationConfig config;
    private final PushSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    private PushService pushService;

    public WebPushServiceImpl(PushNotificationConfig config,
                              PushSubscriptionRepository subscriptionRepository,
                              ObjectMapper objectMapper) {
        this.config = config;
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // Регистрация BouncyCastle провайдера для криптографии
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        try {
            if (config.getVapidPublicKey() != null && config.getVapidPrivateKey() != null) {
                pushService = new PushService()
                        .setPublicKey(config.getVapidPublicKey())
                        .setPrivateKey(config.getVapidPrivateKey())
                        .setSubject(config.getVapidSubject());
                log.info("Web Push Service initialized successfully");
            } else {
                log.warn("VAPID keys not configured. Push notifications will not work.");
            }
        } catch (GeneralSecurityException e) {
            log.error("Failed to initialize Web Push Service", e);
        }
    }

    @Override
    public boolean sendPushNotification(PushSubscription subscription, PushMessageDto message) {
        if (pushService == null) {
            log.warn("Push service not initialized, skipping notification");
            return false;
        }

        try {
            // Добавляем иконку и badge из конфигурации
            if (message.getIcon() == null) {
                message.setIcon(config.getNotificationIcon());
            }
            if (message.getBadge() == null) {
                message.setBadge(config.getNotificationBadge());
            }

            String payload = objectMapper.writeValueAsString(message);

            Subscription webPushSubscription = new Subscription(
                    subscription.getEndpoint(),
                    new Subscription.Keys(subscription.getP256dhKey(), subscription.getAuthKey())
            );

            Notification notification = new Notification(webPushSubscription, payload);
            pushService.send(notification);

            log.debug("Push notification sent to endpoint: {}",
                    subscription.getEndpoint().substring(0, Math.min(50, subscription.getEndpoint().length())));
            return true;

        } catch (Exception e) {
            log.error("Failed to send push notification to subscription id={}", subscription.getId(), e);

            // Если endpoint больше недействителен, деактивируем подписку
            if (isSubscriptionExpired(e)) {
                log.info("Deactivating expired subscription id={}", subscription.getId());
                subscription.setActive(false);
                subscriptionRepository.save(subscription);
            }

            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int sendPushToUser(Long userId, PushMessageDto message) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserIdAndActiveTrue(userId);

        if (subscriptions.isEmpty()) {
            log.debug("No active subscriptions for user id={}", userId);
            return 0;
        }

        int successCount = 0;
        for (PushSubscription subscription : subscriptions) {
            if (sendPushNotification(subscription, message)) {
                successCount++;
            }
        }

        log.info("Sent {} of {} push notifications to user id={}", successCount, subscriptions.size(), userId);
        return successCount;
    }

    @Override
    public String getVapidPublicKey() {
        return config.getVapidPublicKey();
    }

    /**
     * Проверить, истекла ли подписка (например, пользователь отписался в браузере)
     */
    private boolean isSubscriptionExpired(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;

        return message.contains("410") ||
               message.contains("404") ||
               message.contains("ExpiredSubscription");
    }
}
