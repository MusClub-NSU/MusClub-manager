'use client';

import { usePushNotifications } from '@/hooks/usePushNotifications';
import { useCurrentUserRole } from '@/hooks/useCurrentUserRole';

/**
 * Невидимый компонент для авто-подписки на push при загрузке любой страницы
 */
export function PushNotificationInit() {
  const { currentUser } = useCurrentUserRole();
  usePushNotifications(currentUser?.id);
  return null;
}
