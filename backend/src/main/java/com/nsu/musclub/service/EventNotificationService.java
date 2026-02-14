package com.nsu.musclub.service;

import com.nsu.musclub.dto.push.EventNotificationSettingsDto;
import com.nsu.musclub.dto.push.NotificationResponseDto;
import com.nsu.musclub.dto.push.NotificationStatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventNotificationService {

    int scheduleNotificationsForEvent(Long eventId);

    int scheduleNotificationsForEvent(Long eventId, EventNotificationSettingsDto settings);

    int cancelNotificationsForEvent(Long eventId);

    int sendImmediateNotification(Long eventId, String title, String body);

    Page<NotificationResponseDto> getNotificationsForUser(Long userId, Pageable pageable);

    List<NotificationResponseDto> getNotificationsForEvent(Long eventId);

    NotificationStatsDto getStatsForEvent(Long eventId);

    int notifyEventUpdated(Long eventId);

    int notifyEventCancelled(Long eventId);
}
