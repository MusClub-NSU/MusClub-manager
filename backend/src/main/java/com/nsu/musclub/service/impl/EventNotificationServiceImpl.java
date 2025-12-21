package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.Event;
import com.nsu.musclub.domain.EventMember;
import com.nsu.musclub.domain.EventNotification;
import com.nsu.musclub.domain.EventNotification.Status;
import com.nsu.musclub.domain.User;
import com.nsu.musclub.repository.EventMemberRepository;
import com.nsu.musclub.repository.EventNotificationRepository;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.service.EventNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class EventNotificationServiceImpl implements EventNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationServiceImpl.class);

    private final EventNotificationRepository notifications;
    private final EventRepository events;
    private final EventMemberRepository eventMembers;

    public EventNotificationServiceImpl(EventNotificationRepository notifications, EventRepository events, EventMemberRepository eventMembers) {
        this.notifications = notifications;
        this.events = events;
        this.eventMembers = eventMembers;
    }

    @Override
    public void schedule24hBeforeForEventParticipants(Long eventId) {
        Event event = events.findById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStartTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event start time is required to schedule notifications");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime sendAt = event.getStartTime().minusHours(24);

        if (sendAt.isBefore(now)) {
            sendAt = now.plusMinutes(1);
        }

        List<EventMember> members = eventMembers.findByEvent_Id(eventId);
        if (members.isEmpty()) {
            log.info("No members for event {}, skipping notification scheduling", eventId);
            return;
        }

        int created = 0;
        int skippedNoEmail = 0;
        int skippedDuplicate = 0;

        for (EventMember member : members) {
            User user = member.getUser();
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                skippedNoEmail++;
                continue;
            }

            if (notifications.existsByEventIdAndUserIdAndSendAtAndStatus(eventId, user.getId(), sendAt, Status.PENDING)) {
                skippedDuplicate++;
                continue;
            }

            EventNotification n = new EventNotification();
            n.setEvent(event);
            n.setUser(user);
            n.setSendAt(sendAt);
            n.setSubject(buildSubject(event));
            n.setBody(buildEmailBody(event, user));

            notifications.save(n);
            created++;
        }

        log.info("Scheduled notifications for event {}: created={}, skippedNoEmail={}, skippedDuplicate={}", eventId, created, skippedNoEmail, skippedDuplicate);
    }

    private String buildSubject(Event event) {
        return "Напоминание: " + event.getTitle();
    }

    private String buildEmailBody(Event event, User user) {
        String venue = event.getVenue() != null ? event.getVenue() : "место уточняется";
        String username = user.getUsername() != null ? user.getUsername() : "участник";

        return """
                Привет, %s!
                
                Напоминаем о мероприятии "%s".
                
                Время начала: %s
                Место: %s
                
                До встречи!
                """.formatted(username, event.getTitle(), event.getStartTime(), venue);
    }
}
