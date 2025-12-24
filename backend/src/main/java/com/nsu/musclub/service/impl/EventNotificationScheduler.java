package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.EventNotification;
import com.nsu.musclub.domain.EventNotification.Status;
import com.nsu.musclub.repository.EventNotificationRepository;
import com.nsu.musclub.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class EventNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationScheduler.class);

    private final EventNotificationRepository notifications;
    private final EmailService emailService;

    public EventNotificationScheduler(EventNotificationRepository notifications, EmailService emailService) {
        this.notifications = notifications;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processDueNotifications() {
        OffsetDateTime now = OffsetDateTime.now();
        List<EventNotification> due = notifications.findByStatusAndSendAtLessThanEqual(Status.PENDING, now);

        if (due.isEmpty()) {
            return;
        }

        log.info("Processing {} pending notifications", due.size());

        for (EventNotification n : due) {
            try {
                String to = n.getUser().getEmail();
                emailService.sendEmail(to, n.getSubject(), n.getBody());

                n.setStatus(Status.SENT);
                n.setSentAt(OffsetDateTime.now());
            } catch (Exception e) {
                log.error("Failed to send notification id={} to user id={}", n.getId(), n.getUser().getId(), e);
                n.setStatus(Status.FAILED);
            }
        }
    }
}
