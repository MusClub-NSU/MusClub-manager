package com.nsu.musclub.service.impl;

import com.nsu.musclub.config.PushNotificationConfig;
import com.nsu.musclub.domain.Event;
import com.nsu.musclub.domain.EventMember;
import com.nsu.musclub.domain.EventNotification;
import com.nsu.musclub.domain.EventNotification.NotificationType;
import com.nsu.musclub.domain.EventNotification.Status;
import com.nsu.musclub.domain.User;
import com.nsu.musclub.dto.push.EventNotificationSettingsDto;
import com.nsu.musclub.dto.push.NotificationResponseDto;
import com.nsu.musclub.dto.push.NotificationStatsDto;
import com.nsu.musclub.dto.push.PushMessageDto;
import com.nsu.musclub.exception.BadRequestException;
import com.nsu.musclub.exception.ResourceNotFoundException;
import com.nsu.musclub.repository.EventMemberRepository;
import com.nsu.musclub.repository.EventNotificationRepository;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.service.EventNotificationService;
import com.nsu.musclub.service.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class EventNotificationServiceImpl implements EventNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", new Locale("ru"));

    private final EventNotificationRepository notificationRepository;
    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final WebPushService webPushService;
    private final PushNotificationConfig config;

    public EventNotificationServiceImpl(EventNotificationRepository notificationRepository,
                                        EventRepository eventRepository,
                                        EventMemberRepository eventMemberRepository,
                                        WebPushService webPushService,
                                        PushNotificationConfig config) {
        this.notificationRepository = notificationRepository;
        this.eventRepository = eventRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.webPushService = webPushService;
        this.config = config;
    }

    @Override
    public int scheduleNotificationsForEvent(Long eventId) {
        EventNotificationSettingsDto defaultSettings = new EventNotificationSettingsDto();
        defaultSettings.setReminder24h(true);
        defaultSettings.setReminder2h(true);
        defaultSettings.setReminder15min(true);
        return scheduleNotificationsForEvent(eventId, defaultSettings);
    }

    @Override
    public int scheduleNotificationsForEvent(Long eventId, EventNotificationSettingsDto settings) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие", eventId));

        if (event.getStartTime() == null) {
            throw new BadRequestException("Для планирования уведомлений необходимо указать время начала мероприятия");
        }

        List<EventMember> members = eventMemberRepository.findByEvent_Id(eventId);
        if (members.isEmpty()) {
            log.info("Нет участников для мероприятия id={}, уведомления не созданы", eventId);
            return 0;
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<ReminderConfig> reminders = buildReminderConfigs(event.getStartTime(), now, settings);

        int totalCreated = 0;
        for (EventMember member : members) {
            User user = member.getUser();
            for (ReminderConfig reminder : reminders) {
                if (!notificationRepository.existsByEventIdAndUserIdAndNotificationTypeAndStatus(
                        eventId, user.getId(), reminder.type, Status.PENDING)) {

                    EventNotification notification = createNotification(event, user, reminder);
                    notificationRepository.save(notification);
                    totalCreated++;
                }
            }
        }

        log.info("Запланировано {} уведомлений для мероприятия id={}", totalCreated, eventId);
        return totalCreated;
    }

    @Override
    public int cancelNotificationsForEvent(Long eventId) {
        int cancelled = notificationRepository.cancelPendingByEventId(eventId);
        log.info("Отменено {} уведомлений для мероприятия id={}", cancelled, eventId);
        return cancelled;
    }

    @Override
    public int sendImmediateNotification(Long eventId, String title, String body) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие", eventId));

        List<EventMember> members = eventMemberRepository.findByEvent_Id(eventId);
        if (members.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        for (EventMember member : members) {
            PushMessageDto message = PushMessageDto.builder()
                    .title(title)
                    .body(body)
                    .tag("event-" + eventId + "-immediate")
                    .actionUrl("/events/" + eventId)
                    .build();

            int sent = webPushService.sendPushToUser(member.getUser().getId(), message);
            if (sent > 0) {
                // Сохраняем запись об отправленном уведомлении
                EventNotification notification = new EventNotification();
                notification.setEvent(event);
                notification.setUser(member.getUser());
                notification.setNotificationType(NotificationType.CUSTOM);
                notification.setTitle(title);
                notification.setBody(body);
                notification.setActionUrl("/events/" + eventId);
                notification.setSendAt(OffsetDateTime.now());
                notification.setSentAt(OffsetDateTime.now());
                notification.setStatus(Status.SENT);
                notificationRepository.save(notification);

                sentCount++;
            }
        }

        log.info("Отправлено {} немедленных уведомлений для мероприятия id={}", sentCount, eventId);
        return sentCount;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderBySendAtDesc(userId, pageable)
                .map(this::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsForEvent(Long eventId) {
        return notificationRepository.findByEventIdOrderBySendAtDesc(eventId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatsDto getStatsForEvent(Long eventId) {
        List<Object[]> stats = notificationRepository.countByEventIdGroupByStatus(eventId);

        NotificationStatsDto dto = new NotificationStatsDto();
        long total = 0;

        for (Object[] row : stats) {
            Status status = (Status) row[0];
            long count = (Long) row[1];
            total += count;

            switch (status) {
                case PENDING -> dto.setPending(count);
                case SENT -> dto.setSent(count);
                case FAILED -> dto.setFailed(count);
                case CANCELLED -> dto.setCancelled(count);
            }
        }

        dto.setTotal(total);
        return dto;
    }

    @Override
    public int notifyEventUpdated(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие", eventId));

        String title = "Мероприятие обновлено";
        String body = String.format("Информация о мероприятии \"%s\" была изменена. Проверьте актуальные данные.",
                event.getTitle());

        return sendImmediateNotificationWithType(event, title, body, NotificationType.EVENT_UPDATED);
    }

    @Override
    public int notifyEventCancelled(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие", eventId));

        // Отменяем все pending уведомления
        cancelNotificationsForEvent(eventId);

        String title = "Мероприятие отменено";
        String body = String.format("К сожалению, мероприятие \"%s\" было отменено.", event.getTitle());

        return sendImmediateNotificationWithType(event, title, body, NotificationType.EVENT_CANCELLED);
    }

    // === Приватные методы ===

    private List<ReminderConfig> buildReminderConfigs(OffsetDateTime eventStart, OffsetDateTime now,
                                                       EventNotificationSettingsDto settings) {
        List<ReminderConfig> reminders = new ArrayList<>();

        if (settings.isReminder24h()) {
            OffsetDateTime sendAt = eventStart.minusHours(24);
            if (sendAt.isAfter(now)) {
                reminders.add(new ReminderConfig(NotificationType.REMINDER_24H, sendAt, "за 24 часа"));
            }
        }

        if (settings.isReminder2h()) {
            OffsetDateTime sendAt = eventStart.minusHours(2);
            if (sendAt.isAfter(now)) {
                reminders.add(new ReminderConfig(NotificationType.REMINDER_2H, sendAt, "через 2 часа"));
            }
        }

        if (settings.isReminder15min()) {
            OffsetDateTime sendAt = eventStart.minusMinutes(15);
            if (sendAt.isAfter(now)) {
                reminders.add(new ReminderConfig(NotificationType.REMINDER_15MIN, sendAt, "через 15 минут"));
            }
        }

        // Кастомные интервалы
        if (settings.getCustomIntervals() != null) {
            for (Integer minutes : settings.getCustomIntervals()) {
                OffsetDateTime sendAt = eventStart.minusMinutes(minutes);
                if (sendAt.isAfter(now)) {
                    reminders.add(new ReminderConfig(NotificationType.CUSTOM, sendAt,
                            "через " + formatMinutes(minutes)));
                }
            }
        }

        return reminders;
    }

    private EventNotification createNotification(Event event, User user, ReminderConfig reminder) {
        EventNotification notification = new EventNotification();
        notification.setEvent(event);
        notification.setUser(user);
        notification.setNotificationType(reminder.type);
        notification.setSendAt(reminder.sendAt);
        notification.setTitle(buildNotificationTitle(event, reminder));
        notification.setBody(buildNotificationBody(event, user, reminder));
        notification.setActionUrl("/events/" + event.getId());
        return notification;
    }

    private String buildNotificationTitle(Event event, ReminderConfig reminder) {
        return String.format("Напоминание: %s", event.getTitle());
    }

    private String buildNotificationBody(Event event, User user, ReminderConfig reminder) {
        String venue = event.getVenue() != null ? event.getVenue() : "место уточняется";
        String formattedDate = event.getStartTime().format(DATE_FORMATTER);

        return String.format("Мероприятие начнётся %s!\n📅 %s\n📍 %s",
                reminder.timeDescription, formattedDate, venue);
    }

    private int sendImmediateNotificationWithType(Event event, String title, String body, NotificationType type) {
        List<EventMember> members = eventMemberRepository.findByEvent_Id(event.getId());
        if (members.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        for (EventMember member : members) {
            PushMessageDto message = PushMessageDto.builder()
                    .title(title)
                    .body(body)
                    .tag("event-" + event.getId() + "-" + type.name().toLowerCase())
                    .actionUrl("/events/" + event.getId())
                    .requireInteraction(type == NotificationType.EVENT_CANCELLED)
                    .build();

            int sent = webPushService.sendPushToUser(member.getUser().getId(), message);
            if (sent > 0) {
                EventNotification notification = new EventNotification();
                notification.setEvent(event);
                notification.setUser(member.getUser());
                notification.setNotificationType(type);
                notification.setTitle(title);
                notification.setBody(body);
                notification.setActionUrl("/events/" + event.getId());
                notification.setSendAt(OffsetDateTime.now());
                notification.setSentAt(OffsetDateTime.now());
                notification.setStatus(Status.SENT);
                notificationRepository.save(notification);

                sentCount++;
            }
        }

        return sentCount;
    }

    private NotificationResponseDto toResponseDto(EventNotification notification) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(notification.getId());
        dto.setEventId(notification.getEvent().getId());
        dto.setEventTitle(notification.getEvent().getTitle());
        dto.setUserId(notification.getUser().getId());
        dto.setSendAt(notification.getSendAt());
        dto.setSentAt(notification.getSentAt());
        dto.setStatus(notification.getStatus());
        dto.setNotificationType(notification.getNotificationType());
        dto.setTitle(notification.getTitle());
        dto.setBody(notification.getBody());
        dto.setActionUrl(notification.getActionUrl());
        dto.setRetryCount(notification.getRetryCount());
        dto.setErrorMessage(notification.getErrorMessage());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }

    private String formatMinutes(int minutes) {
        if (minutes >= 1440) {
            int days = minutes / 1440;
            return days + (days == 1 ? " день" : " дней");
        } else if (minutes >= 60) {
            int hours = minutes / 60;
            return hours + (hours == 1 ? " час" : " часов");
        } else {
            return minutes + " минут";
        }
    }

    /**
     * Конфигурация напоминания
     */
    private record ReminderConfig(NotificationType type, OffsetDateTime sendAt, String timeDescription) {
    }
}
