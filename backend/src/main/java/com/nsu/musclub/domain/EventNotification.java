package com.nsu.musclub.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Push-уведомление о мероприятии
 */
@Entity
@Table(name = "event_notifications")
public class EventNotification {

    public enum Status {
        PENDING,    // Ожидает отправки
        SENT,       // Успешно отправлено
        FAILED,     // Ошибка при отправке
        CANCELLED   // Отменено (мероприятие отменено/удалено)
    }

    public enum NotificationType {
        REMINDER_24H,      // Напоминание за 24 часа
        REMINDER_2H,       // Напоминание за 2 часа
        REMINDER_15MIN,    // Напоминание за 15 минут
        EVENT_UPDATED,     // Мероприятие обновлено
        EVENT_CANCELLED,   // Мероприятие отменено
        CUSTOM             // Кастомное уведомление
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "send_at", nullable = false)
    private OffsetDateTime sendAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    private NotificationType notificationType = NotificationType.REMINDER_24H;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * URL для перехода при клике на уведомление
     */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /**
     * Количество попыток отправки
     */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /**
     * Сообщение об ошибке (если status = FAILED)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public OffsetDateTime getSendAt() {
        return sendAt;
    }

    public void setSendAt(OffsetDateTime sendAt) {
        this.sendAt = sendAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
