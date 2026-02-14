package com.nsu.musclub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация push-уведомлений
 */
@Configuration
@ConfigurationProperties(prefix = "push")
public class PushNotificationConfig {

    /**
     * VAPID публичный ключ (Base64 URL-safe)
     */
    private String vapidPublicKey;

    /**
     * VAPID приватный ключ (Base64 URL-safe)
     */
    private String vapidPrivateKey;

    /**
     * Email для VAPID subject (например, mailto:admin@example.com)
     */
    private String vapidSubject = "mailto:admin@musclub.nsu.ru";

    /**
     * URL иконки для уведомлений
     */
    private String notificationIcon = "/icon-192x192.png";

    /**
     * URL badge для уведомлений
     */
    private String notificationBadge = "/icon-192x192.png";

    /**
     * Максимальное количество попыток отправки
     */
    private int maxRetries = 3;

    /**
     * Интервалы напоминаний по умолчанию (в минутах)
     */
    private List<Integer> defaultReminderIntervals = List.of(1440, 120, 15); // 24h, 2h, 15min

    /**
     * Интервал проверки pending уведомлений (в миллисекундах)
     */
    private long schedulerInterval = 30000; // 30 секунд

    /**
     * Время жизни старых уведомлений в днях (для очистки)
     */
    private int notificationRetentionDays = 30;

    // Getters and Setters

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    public void setVapidPublicKey(String vapidPublicKey) {
        this.vapidPublicKey = vapidPublicKey;
    }

    public String getVapidPrivateKey() {
        return vapidPrivateKey;
    }

    public void setVapidPrivateKey(String vapidPrivateKey) {
        this.vapidPrivateKey = vapidPrivateKey;
    }

    public String getVapidSubject() {
        return vapidSubject;
    }

    public void setVapidSubject(String vapidSubject) {
        this.vapidSubject = vapidSubject;
    }

    public String getNotificationIcon() {
        return notificationIcon;
    }

    public void setNotificationIcon(String notificationIcon) {
        this.notificationIcon = notificationIcon;
    }

    public String getNotificationBadge() {
        return notificationBadge;
    }

    public void setNotificationBadge(String notificationBadge) {
        this.notificationBadge = notificationBadge;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public List<Integer> getDefaultReminderIntervals() {
        return defaultReminderIntervals;
    }

    public void setDefaultReminderIntervals(List<Integer> defaultReminderIntervals) {
        this.defaultReminderIntervals = defaultReminderIntervals;
    }

    public long getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(long schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public int getNotificationRetentionDays() {
        return notificationRetentionDays;
    }

    public void setNotificationRetentionDays(int notificationRetentionDays) {
        this.notificationRetentionDays = notificationRetentionDays;
    }
}
