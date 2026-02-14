'use client';

import React from 'react';
import { usePushNotifications } from '@/hooks/usePushNotifications';

interface PushNotificationSettingsProps {
  userId: number;
  className?: string;
}

/**
 * Компонент для управления push-уведомлениями
 */
export function PushNotificationSettings({ userId, className = '' }: PushNotificationSettingsProps) {
  const {
    isSupported,
    isSubscribed,
    isLoading,
    error,
    permission,
    subscribe,
    unsubscribe,
    sendTestNotification
  } = usePushNotifications();

  const handleToggle = async () => {
    if (isSubscribed) {
      await unsubscribe();
    } else {
      await subscribe(userId);
    }
  };

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
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-medium text-gray-900">
            Push-уведомления
          </h3>
          <p className="text-sm text-gray-500">
            {isSubscribed
              ? 'Вы получаете уведомления о мероприятиях'
              : 'Включите, чтобы получать напоминания'}
          </p>
        </div>

        <button
          onClick={handleToggle}
          disabled={isLoading}
          className={`
            relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full 
            border-2 border-transparent transition-colors duration-200 ease-in-out 
            focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2
            ${isSubscribed ? 'bg-indigo-600' : 'bg-gray-200'}
            ${isLoading ? 'opacity-50 cursor-wait' : ''}
          `}
          role="switch"
          aria-checked={isSubscribed}
        >
          <span
            className={`
              pointer-events-none inline-block h-5 w-5 transform rounded-full 
              bg-white shadow ring-0 transition duration-200 ease-in-out
              ${isSubscribed ? 'translate-x-5' : 'translate-x-0'}
            `}
          />
        </button>
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

      {isSubscribed && (
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
