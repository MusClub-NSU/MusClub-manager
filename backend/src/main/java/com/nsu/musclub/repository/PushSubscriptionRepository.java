package com.nsu.musclub.repository;

import com.nsu.musclub.domain.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    /**
     * Найти все активные подписки пользователя
     */
    List<PushSubscription> findByUserIdAndActiveTrue(Long userId);

    /**
     * Найти подписку по endpoint
     */
    Optional<PushSubscription> findByEndpoint(String endpoint);

    /**
     * Проверить существование активной подписки
     */
    boolean existsByUserIdAndEndpointAndActiveTrue(Long userId, String endpoint);

    /**
     * Деактивировать все подписки пользователя
     */
    @Modifying
    @Query("UPDATE PushSubscription p SET p.active = false WHERE p.user.id = :userId")
    void deactivateAllByUserId(@Param("userId") Long userId);

    /**
     * Деактивировать подписку по endpoint
     */
    @Modifying
    @Query("UPDATE PushSubscription p SET p.active = false WHERE p.endpoint = :endpoint")
    void deactivateByEndpoint(@Param("endpoint") String endpoint);

    /**
     * Получить все активные подписки для списка пользователей
     */
    @Query("SELECT p FROM PushSubscription p WHERE p.user.id IN :userIds AND p.active = true")
    List<PushSubscription> findActiveByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * Подсчет активных подписок пользователя
     */
    long countByUserIdAndActiveTrue(Long userId);
}
