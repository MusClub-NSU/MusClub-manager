package com.nsu.musclub.service.impl;

import com.nsu.musclub.config.PushNotificationConfig;
import com.nsu.musclub.domain.EventNotification;
import com.nsu.musclub.domain.EventNotification.Status;
import com.nsu.musclub.domain.PushSubscription;
import com.nsu.musclub.dto.push.PushMessageDto;
import com.nsu.musclub.repository.EventNotificationRepository;
import com.nsu.musclub.repository.PushSubscriptionRepository;
import com.nsu.musclub.service.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class PushNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationScheduler.class);

    private final EventNotificationRepository notificationRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final WebPushService webPushService;
    private final PushNotificationConfig config;

    public PushNotificationScheduler(EventNotificationRepository notificationRepository,
                                     PushSubscriptionRepository subscriptionRepository,
                                     WebPushService webPushService,
                                     PushNotificationConfig config) {
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webPushService = webPushService;
        this.config = config;
    }

    /**
     * Обработка pending уведомлений каждые 30 секунд
     */
    @Scheduled(fixedDelayString = "${push.scheduler-interval:30000}")
    @Transactional
    public void processPendingNotifications() {
        OffsetDateTime now = OffsetDateTime.now();
        List<EventNotification> pendingNotifications =
                notificationRepository.findByStatusAndSendAtLessThanEqual(Status.PENDING, now);

        if (pendingNotifications.isEmpty()) {
            return;
        }

        log.info("Processing {} pending push notifications", pendingNotifications.size());

        int sentCount = 0;
        int failedCount = 0;

        for (EventNotification notification : pendingNotifications) {
            try {
                boolean success = sendPushNotification(notification);

                if (success) {
                    notification.setStatus(Status.SENT);
                    notification.setSentAt(OffsetDateTime.now());
                    sentCount++;
                } else {
                    handleFailedNotification(notification, "Нет активных подписок для пользователя");
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Error processing notification id={}", notification.getId(), e);
                handleFailedNotification(notification, e.getMessage());
                failedCount++;
            }
        }

        log.info("Push notification processing complete: sent={}, failed={}", sentCount, failedCount);
    }

    /**
     * Повторная отправка неудачных уведомлений (каждые 5 минут)
     */
    @Scheduled(fixedDelay = 300000) // 5 минут
    @Transactional
    public void retryFailedNotifications() {
        List<EventNotification> failedNotifications =
                notificationRepository.findFailedForRetry(config.getMaxRetries());

        if (failedNotifications.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed push notifications", failedNotifications.size());

        for (EventNotification notification : failedNotifications) {
            notification.incrementRetryCount();
            notification.setStatus(Status.PENDING); // Возвращаем в очередь
            notification.setErrorMessage(null);
        }
    }

    /**
     * Очистка старых уведомлений (ежедневно в 3:00)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(config.getNotificationRetentionDays());
        int deleted = notificationRepository.deleteOldNotifications(threshold);

        if (deleted > 0) {
            log.info("Deleted {} old notifications (older than {} days)",
                    deleted, config.getNotificationRetentionDays());
        }
    }

    /**
     * Отправить push-уведомление пользователю
     */
    private boolean sendPushNotification(EventNotification notification) {
        Long userId = notification.getUser().getId();
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserIdAndActiveTrue(userId);

        if (subscriptions.isEmpty()) {
            log.debug("No active subscriptions for user id={}", userId);
            return false;
        }

        PushMessageDto message = PushMessageDto.builder()
                .title(notification.getTitle())
                .body(notification.getBody())
                .tag("event-" + notification.getEvent().getId() + "-" + notification.getNotificationType().name())
                .actionUrl(notification.getActionUrl())
                .build();

        boolean atLeastOneSent = false;
        for (PushSubscription subscription : subscriptions) {
            if (webPushService.sendPushNotification(subscription, message)) {
                atLeastOneSent = true;
            }
        }

        return atLeastOneSent;
    }

    private void handleFailedNotification(EventNotification notification, String errorMessage) {
        notification.incrementRetryCount();
        notification.setErrorMessage(errorMessage);

        if (notification.getRetryCount() >= config.getMaxRetries()) {
            notification.setStatus(Status.FAILED);
            log.warn("Notification id={} failed after {} retries",
                    notification.getId(), notification.getRetryCount());
        }
        // Иначе останется PENDING для следующей попытки
    }
}
