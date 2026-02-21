'use client';

import { usePushNotifications } from '@/hooks/usePushNotifications';

/**
 * Невидимый компонент для авто-подписки на push при загрузке любой страницы
 */
export function PushNotificationInit() {
  usePushNotifications();
  return null;
}
