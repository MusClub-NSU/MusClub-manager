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
      <div className={`p-4 bg-gray-100 rounded-lg ${className}`}>
        <p className="text-gray-500 text-sm">
          Push-уведомления не поддерживаются в вашем браузере
        </p>
      </div>
    );
  }

  return (
    <div className={`p-4 bg-white rounded-lg shadow ${className}`}>
      <div>
        <h3 className="text-lg font-medium text-gray-900">
          Push-уведомления
        </h3>
        <p className="text-sm text-gray-500">
          {isLoading
            ? 'Подписка на уведомления...'
            : isSubscribed
              ? 'Вы получаете уведомления о мероприятиях'
              : 'Ожидание разрешения'}
        </p>
      </div>

      {/* Статус разрешения */}
      {permission === 'denied' && (
        <div className="mt-3 p-2 bg-red-50 rounded text-sm text-red-600">
          Уведомления заблокированы в настройках браузера.
          Разрешите их в настройках сайта.
        </div>
      )}

      {error && (
        <div className="mt-3 p-2 bg-red-50 rounded text-sm text-red-600">
          {error}
        </div>
      )}

      {isSubscribed && canManageEvents && (
        <button
          onClick={sendTestNotification}
          className="mt-3 text-sm text-indigo-600 hover:text-indigo-800"
        >
          Отправить тестовое уведомление
        </button>
      )}
    </div>
  );
}

export default PushNotificationSettings;
