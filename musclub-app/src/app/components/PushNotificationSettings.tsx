'use client';

import React from 'react';
import { usePushNotifications } from '@/hooks/usePushNotifications';
import { useCurrentUserRole } from '@/hooks/useCurrentUserRole';

interface PushNotificationSettingsProps {
  className?: string;
}

/**
 * Компонент для отображения статуса push-уведомлений (авто-подписка при загрузке)
 */
export function PushNotificationSettings({ className = '' }: PushNotificationSettingsProps) {
  const {
    isSupported,
    isSubscribed,
    isLoading,
    error,
    permission,
    sendTestNotification
  } = usePushNotifications();
  const { canManageEvents } = useCurrentUserRole();

  if (!isSupported) {
    return (
      <div
        className={`p-4 rounded-lg ${className}`}
        style={{ backgroundColor: 'var(--color-background-secondary)', border: '1px solid var(--color-line-generic)' }}
      >
        <p className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
          Push-уведомления не поддерживаются в вашем браузере
        </p>
      </div>
    );
  }

  return (
    <div
      className={`p-4 rounded-lg shadow-sm ${className}`}
      style={{
        backgroundColor: 'var(--color-background-secondary)',
        border: '1px solid var(--color-line-generic)',
      }}
    >
      <div>
        <h3 className="text-lg font-medium" style={{ color: 'var(--color-text-primary)' }}>
          Push-уведомления
        </h3>
        <p className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
          {isLoading
            ? 'Подписка на уведомления...'
            : isSubscribed
              ? 'Вы получаете уведомления о мероприятиях'
              : 'Ожидание разрешения'}
        </p>
      </div>

      {/* Статус разрешения */}
      {permission === 'denied' && (
        <div className="mt-3 p-2 rounded text-sm" style={{ backgroundColor: 'rgba(220, 38, 38, 0.12)', color: '#dc2626' }}>
          Уведомления заблокированы в настройках браузера.
          Разрешите их в настройках сайта.
        </div>
      )}

      {error && (
        <div className="mt-3 p-2 rounded text-sm" style={{ backgroundColor: 'rgba(220, 38, 38, 0.12)', color: '#dc2626' }}>
          {error}
        </div>
      )}

      {isSubscribed && canManageEvents && (
        <button
          onClick={sendTestNotification}
          className="mt-3 text-sm hover:opacity-80"
          style={{ color: 'rgb(79, 70, 229)' }}
        >
          Отправить тестовое уведомление
        </button>
      )}
    </div>
  );
}

export default PushNotificationSettings;
