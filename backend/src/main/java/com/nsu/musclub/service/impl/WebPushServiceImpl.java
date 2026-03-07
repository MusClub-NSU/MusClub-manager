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
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.debug("BouncyCastle provider registered");
        }

        try {
            if (isVapidConfigured()) {
                log.info("VAPID configuration found. Initializing Web Push Service...");
                log.debug("VAPID Public Key length: {}", config.getVapidPublicKey().length());

                pushService = new PushService()
                        .setPublicKey(config.getVapidPublicKey())
                        .setPrivateKey(config.getVapidPrivateKey())
                        .setSubject(config.getVapidSubject());

                log.info("Web Push Service initialized successfully");
            } else {
                log.error("VAPID keys not configured! Push notifications will NOT work.");
                log.error("Set environment variables: VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY");
                log.error("Or configure in application.yml under push.vapid-*");
            }
        } catch (GeneralSecurityException e) {
            log.error("Failed to initialize Web Push Service due to security error", e);
            throw new RuntimeException("Web Push Service initialization failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during Web Push Service initialization", e);
            throw new RuntimeException("Web Push Service initialization failed", e);
        }
    }

    private boolean isVapidConfigured() {
        String publicKey = config.getVapidPublicKey();
        String privateKey = config.getVapidPrivateKey();
        return publicKey != null && !publicKey.isBlank()
                && privateKey != null && !privateKey.isBlank();
    }

    @Override
    public boolean sendPushNotification(PushSubscription subscription, PushMessageDto message) {
        if (pushService == null) {
            log.error("Push service not initialized - VAPID keys may be missing");
            return false;
        }

        try {
            if (subscription == null || subscription.getEndpoint() == null) {
                log.error("Invalid subscription: null or missing endpoint");
                return false;
            }

            if (message.getIcon() == null) {
                message.setIcon(config.getNotificationIcon());
            }
            if (message.getBadge() == null) {
                message.setBadge(config.getNotificationBadge());
            }

            log.debug("Preparing push payload - title: {}, body: {}", message.getTitle(), message.getBody());
            String payload = objectMapper.writeValueAsString(message);
            log.debug("Payload size: {} bytes", payload.length());

            Subscription webPushSubscription = new Subscription(
                    subscription.getEndpoint(),
                    new Subscription.Keys(subscription.getP256dhKey(), subscription.getAuthKey())
            );

            log.debug("Sending notification to endpoint: {}...",
                    subscription.getEndpoint().substring(0, Math.min(50, subscription.getEndpoint().length())));

            Notification notification = new Notification(webPushSubscription, payload);
            pushService.send(notification);

            log.info("Push notification sent successfully to subscription id={}", subscription.getId());
            return true;

        } catch (Exception e) {
            log.error("Failed to send push notification to subscription id={}: {} - {}",
                    subscription.getId(), e.getClass().getSimpleName(), e.getMessage());
            log.debug("Full error stack trace:", e);

            if (isSubscriptionExpired(e)) {
                log.warn("Subscription id={} expired, marking as inactive", subscription.getId());
                subscription.setActive(false);
                subscriptionRepository.save(subscription);
            }

            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int sendPushToUser(Long userId, PushMessageDto message) {
        if (userId == null) {
            log.error("Cannot send push: userId is null");
            return 0;
        }

        log.info("Attempting to send push notification to user id={}", userId);

        List<PushSubscription> subscriptions = subscriptionRepository.findByUserIdAndActiveTrue(userId);

        if (subscriptions.isEmpty()) {
            log.warn("No active subscriptions found for user id={}", userId);
            return 0;
        }

        log.info("Found {} active subscription(s) for user id={}", subscriptions.size(), userId);

        int successCount = 0;
        int failureCount = 0;

        for (PushSubscription subscription : subscriptions) {
            log.debug("Processing subscription id={}", subscription.getId());
            try {
                if (sendPushNotification(subscription, message)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Unexpected error sending to subscription id={}: {}", subscription.getId(), e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("Push notification delivery summary for user id={}: {} sent, {} failed",
                userId, successCount, failureCount);

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
