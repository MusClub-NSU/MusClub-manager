package com.nsu.musclub.repository;

import com.nsu.musclub.domain.EventNotification;
import com.nsu.musclub.domain.EventNotification.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface EventNotificationRepository extends JpaRepository<EventNotification, Long> {

    List<EventNotification> findByStatusAndSendAtLessThanEqual(Status status, OffsetDateTime sendAt);

    boolean existsByEventIdAndUserIdAndSendAtAndStatus(Long eventId, Long userId, OffsetDateTime sendAt, Status status);
}
