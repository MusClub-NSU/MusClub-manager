package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.PushSubscription;
import com.nsu.musclub.domain.User;
import com.nsu.musclub.dto.push.PushSubscriptionDto;
import com.nsu.musclub.dto.push.PushSubscriptionResponseDto;
import com.nsu.musclub.exception.ResourceNotFoundException;
import com.nsu.musclub.repository.PushSubscriptionRepository;
import com.nsu.musclub.repository.UserRepository;
import com.nsu.musclub.service.PushSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Реализация сервиса управления push-подписками
 */
@Service
@Transactional
public class PushSubscriptionServiceImpl implements PushSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionServiceImpl.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public PushSubscriptionServiceImpl(PushSubscriptionRepository subscriptionRepository,
                                       UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PushSubscriptionResponseDto subscribe(PushSubscriptionDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", dto.getUserId()));

        // Проверяем, есть ли уже подписка с таким endpoint
        Optional<PushSubscription> existingSubscription = subscriptionRepository.findByEndpoint(dto.getEndpoint());

        PushSubscription subscription;
        if (existingSubscription.isPresent()) {
            // Обновляем существующую подписку
            subscription = existingSubscription.get();
            subscription.setUser(user);
            subscription.setP256dhKey(dto.getP256dh());
            subscription.setAuthKey(dto.getAuth());
            subscription.setUserAgent(dto.getUserAgent());
            subscription.setActive(true);
            log.info("Updated existing push subscription id={} for user id={}", subscription.getId(), user.getId());
        } else {
            // Создаем новую подписку
            subscription = new PushSubscription();
            subscription.setUser(user);
            subscription.setEndpoint(dto.getEndpoint());
            subscription.setP256dhKey(dto.getP256dh());
            subscription.setAuthKey(dto.getAuth());
            subscription.setUserAgent(dto.getUserAgent());
            log.info("Created new push subscription for user id={}", user.getId());
        }

        subscription = subscriptionRepository.save(subscription);
        return toResponseDto(subscription);
    }

    @Override
    public void unsubscribe(String endpoint) {
        subscriptionRepository.deactivateByEndpoint(endpoint);
        log.info("Deactivated push subscription with endpoint: {}...",
                endpoint.substring(0, Math.min(50, endpoint.length())));
    }

    @Override
    public void unsubscribeAllForUser(Long userId) {
        subscriptionRepository.deactivateAllByUserId(userId);
        log.info("Deactivated all push subscriptions for user id={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PushSubscriptionResponseDto> getSubscriptionsForUser(Long userId) {
        return subscriptionRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserSubscribed(Long userId) {
        return subscriptionRepository.countByUserIdAndActiveTrue(userId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long getSubscriptionCount(Long userId) {
        return subscriptionRepository.countByUserIdAndActiveTrue(userId);
    }

    private PushSubscriptionResponseDto toResponseDto(PushSubscription subscription) {
        PushSubscriptionResponseDto dto = new PushSubscriptionResponseDto();
        dto.setId(subscription.getId());
        dto.setUserId(subscription.getUser().getId());
        dto.setEndpoint(subscription.getEndpoint());
        dto.setActive(subscription.isActive());
        dto.setUserAgent(subscription.getUserAgent());
        dto.setCreatedAt(subscription.getCreatedAt());
        return dto;
    }
}
