package com.nsu.musclub.web;

import com.nsu.musclub.dto.push.*;
import com.nsu.musclub.service.EventNotificationService;
import com.nsu.musclub.service.PushSubscriptionService;
import com.nsu.musclub.service.WebPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Push Notifications", description = "Управление push-уведомлениями и подписками")
@RestController
@RequestMapping("/api/push")
public class PushNotificationController {

    private final PushSubscriptionService subscriptionService;
    private final EventNotificationService notificationService;
    private final WebPushService webPushService;

    public PushNotificationController(PushSubscriptionService subscriptionService,
                                      EventNotificationService notificationService,
                                      WebPushService webPushService) {
        this.subscriptionService = subscriptionService;
        this.notificationService = notificationService;
        this.webPushService = webPushService;
    }


    @Operation(summary = "Получить VAPID публичный ключ",
               description = "Возвращает публичный ключ для настройки push-подписки на клиенте")
    @GetMapping("/vapid-public-key")
    public VapidPublicKeyDto getVapidPublicKey() {
        return new VapidPublicKeyDto(webPushService.getVapidPublicKey());
    }

    @Operation(summary = "Подписаться на push-уведомления",
               description = "Создает или обновляет push-подписку для устройства пользователя")
    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    public PushSubscriptionResponseDto subscribe(@RequestBody @Valid PushSubscriptionDto dto) {
        return subscriptionService.subscribe(dto);
    }

    @Operation(summary = "Отписаться от push-уведомлений",
               description = "Деактивирует push-подписку для указанного endpoint")
    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) {
            subscriptionService.unsubscribe(endpoint);
        }
    }

    @Operation(summary = "Получить подписки пользователя")
    @GetMapping("/subscriptions/{userId}")
    public List<PushSubscriptionResponseDto> getUserSubscriptions(
            @Parameter(description = "ID пользователя") @PathVariable Long userId) {
        return subscriptionService.getSubscriptionsForUser(userId);
    }

    @Operation(summary = "Отправить push-уведомление пользователю",
               description = "Отправляет push-уведомление конкретному пользователю")
    @PostMapping("/send")
    public Map<String, Object> sendPushToUser(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String title = (String) body.getOrDefault("title", "Уведомление");
        String bodyText = (String) body.getOrDefault("body", "");
        String icon = (String) body.getOrDefault("icon", "/icon-192x192.png");
        String badge = (String) body.getOrDefault("badge", "/icon-192x192.png");

        PushMessageDto message = PushMessageDto.builder()
                .title(title)
                .body(bodyText)
                .icon(icon)
                .badge(badge)
                .build();

        int sentCount = webPushService.sendPushToUser(userId, message);

        return Map.of(
                "success", sentCount > 0,
                "sentCount", sentCount,
                "message", sentCount > 0 ? "Уведомление отправлено" : "Нет активных подписок"
        );
    }

    @Operation(summary = "Проверить статус подписки пользователя")
    @GetMapping("/subscriptions/{userId}/status")
    public Map<String, Object> getSubscriptionStatus(@PathVariable Long userId) {
        return Map.of(
                "subscribed", subscriptionService.isUserSubscribed(userId),
                "subscriptionCount", subscriptionService.getSubscriptionCount(userId)
        );
    }


    @Operation(summary = "Запланировать уведомления для мероприятия",
               description = "Создает напоминания для всех участников мероприятия (24ч, 2ч, 15мин до начала)")
    @PostMapping("/events/{eventId}/schedule")
    public Map<String, Object> scheduleEventNotifications(
            @Parameter(description = "ID мероприятия") @PathVariable Long eventId,
            @RequestBody(required = false) EventNotificationSettingsDto settings) {

        int count;
        if (settings != null) {
            count = notificationService.scheduleNotificationsForEvent(eventId, settings);
        } else {
            count = notificationService.scheduleNotificationsForEvent(eventId);
        }

        return Map.of(
                "eventId", eventId,
                "scheduledCount", count,
                "message", "Уведомления успешно запланированы"
        );
    }

    @Operation(summary = "Отменить уведомления для мероприятия",
               description = "Отменяет все pending уведомления для указанного мероприятия")
    @DeleteMapping("/events/{eventId}/notifications")
    public Map<String, Object> cancelEventNotifications(@PathVariable Long eventId) {
        int count = notificationService.cancelNotificationsForEvent(eventId);
        return Map.of(
                "eventId", eventId,
                "cancelledCount", count,
                "message", "Уведомления отменены"
        );
    }

    @Operation(summary = "Отправить немедленное уведомление участникам",
               description = "Отправляет push-уведомление всем участникам мероприятия прямо сейчас")
    @PostMapping("/events/{eventId}/send-now")
    public Map<String, Object> sendImmediateNotification(
            @PathVariable Long eventId,
            @RequestBody Map<String, String> body) {

        String title = body.getOrDefault("title", "Уведомление");
        String message = body.getOrDefault("body", "");

        int count = notificationService.sendImmediateNotification(eventId, title, message);
        return Map.of(
                "eventId", eventId,
                "sentCount", count,
                "message", "Уведомления отправлены"
        );
    }

    @Operation(summary = "Получить уведомления мероприятия")
    @GetMapping("/events/{eventId}/notifications")
    public List<NotificationResponseDto> getEventNotifications(@PathVariable Long eventId) {
        return notificationService.getNotificationsForEvent(eventId);
    }

    @Operation(summary = "Получить статистику уведомлений мероприятия")
    @GetMapping("/events/{eventId}/notifications/stats")
    public NotificationStatsDto getEventNotificationStats(@PathVariable Long eventId) {
        return notificationService.getStatsForEvent(eventId);
    }


    @Operation(summary = "Получить уведомления пользователя")
    @GetMapping("/users/{userId}/notifications")
    public Page<NotificationResponseDto> getUserNotifications(
            @PathVariable Long userId,
            Pageable pageable) {
        return notificationService.getNotificationsForUser(userId, pageable);
    }
}
