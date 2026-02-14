package com.nsu.musclub.repository;

import com.nsu.musclub.domain.EventNotification;
import com.nsu.musclub.domain.EventNotification.NotificationType;
import com.nsu.musclub.domain.EventNotification.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface EventNotificationRepository extends JpaRepository<EventNotification, Long> {

    /**
     * Найти уведомления готовые к отправке
     */
    List<EventNotification> findByStatusAndSendAtLessThanEqual(Status status, OffsetDateTime sendAt);

    /**
     * Найти уведомления для retry (FAILED с retry_count < maxRetries)
     */
    @Query("SELECT n FROM EventNotification n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetries")
    List<EventNotification> findFailedForRetry(@Param("maxRetries") int maxRetries);

    /**
     * Проверить существование pending уведомления
     */
    boolean existsByEventIdAndUserIdAndNotificationTypeAndStatus(
            Long eventId, Long userId, NotificationType type, Status status);

    /**
     * Отменить все pending уведомления для мероприятия
     */
    @Modifying
    @Query("UPDATE EventNotification n SET n.status = 'CANCELLED' WHERE n.event.id = :eventId AND n.status = 'PENDING'")
    int cancelPendingByEventId(@Param("eventId") Long eventId);

    /**
     * Получить уведомления пользователя
     */
    Page<EventNotification> findByUserIdOrderBySendAtDesc(Long userId, Pageable pageable);

    /**
     * Получить уведомления мероприятия
     */
    List<EventNotification> findByEventIdOrderBySendAtDesc(Long eventId);

    /**
     * Статистика по статусам для мероприятия
     */
    @Query("SELECT n.status, COUNT(n) FROM EventNotification n WHERE n.event.id = :eventId GROUP BY n.status")
    List<Object[]> countByEventIdGroupByStatus(@Param("eventId") Long eventId);

    /**
     * Удалить старые отправленные уведомления
     */
    @Modifying
    @Query("DELETE FROM EventNotification n WHERE n.status IN ('SENT', 'CANCELLED') AND n.sentAt < :before")
    int deleteOldNotifications(@Param("before") OffsetDateTime before);
}
