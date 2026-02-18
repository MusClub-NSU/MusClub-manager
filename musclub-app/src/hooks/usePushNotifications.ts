/**
 * Хук для управления push-уведомлениями
 */
'use client';

import { useState, useEffect, useCallback } from 'react';

// По умолчанию используем относительный "/api/*" (Next.js proxy -> backend через rewrites).
// Можно переопределить в рантайме, установив window.ENV_API_URL (например, "https://my-backend.example.com").
const API_BASE_URL =
  typeof window !== 'undefined'
    ? (window as unknown as { ENV_API_URL?: string }).ENV_API_URL || ''
    : '';

interface PushSubscriptionState {
  isSupported: boolean;
  isSubscribed: boolean;
  isLoading: boolean;
  error: string | null;
  permission: NotificationPermission | 'default';
}

interface UsePushNotificationsReturn extends PushSubscriptionState {
  subscribe: (userId: number) => Promise<boolean>;
  unsubscribe: () => Promise<boolean>;
  requestPermission: () => Promise<NotificationPermission>;
  sendTestNotification: () => void;
}

// Утилита для конвертации ArrayBuffer в Base64 URL-safe
function arrayBufferToBase64Url(buffer: ArrayBuffer | null): string {
  if (!buffer) return '';
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

// Утилита для конвертации Base64 URL-safe в Uint8Array
function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding)
    .replace(/-/g, '+')
    .replace(/_/g, '/');

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export function usePushNotifications(): UsePushNotificationsReturn {
  const [state, setState] = useState<PushSubscriptionState>({
    isSupported: false,
    isSubscribed: false,
    isLoading: true,
    error: null,
    permission: 'default'
  });

  // Проверка поддержки push-уведомлений
  useEffect(() => {
    const checkSupport = async () => {
      // Проверяем поддержку Service Worker и Push API
      const isSupported = 'serviceWorker' in navigator &&
                          'PushManager' in window &&
                          'Notification' in window;

      if (!isSupported) {
        setState(prev => ({
          ...prev,
          isSupported: false,
          isLoading: false,
          error: 'Push-уведомления не поддерживаются в этом браузере'
        }));
        return;
      }

      // Проверяем текущее разрешение
      const permission = Notification.permission;

      // Проверяем текущую подписку
      try {
        const registration = await navigator.serviceWorker.ready;
        const subscription = await registration.pushManager.getSubscription();

        setState({
          isSupported: true,
          isSubscribed: !!subscription,
          isLoading: false,
          error: null,
          permission
        });
      } catch (error) {
        console.error('Error checking push subscription:', error);
        setState({
          isSupported: true,
          isSubscribed: false,
          isLoading: false,
          error: null,
          permission
        });
      }
    };

    checkSupport();
  }, []);

  // Регистрация service worker для push
  const registerServiceWorker = useCallback(async (): Promise<ServiceWorkerRegistration> => {
    // Регистрируем наш push service worker
    const registration = await navigator.serviceWorker.register('/push-sw.js', {
      scope: '/'
    });

    // Ждем активации
    await navigator.serviceWorker.ready;

    return registration;
  }, []);

  // Запрос разрешения на уведомления
  const requestPermission = useCallback(async (): Promise<NotificationPermission> => {
    if (!state.isSupported) {
      return 'denied';
    }

    const permission = await Notification.requestPermission();
    setState(prev => ({ ...prev, permission }));
    return permission;
  }, [state.isSupported]);

  // Получение VAPID ключа с сервера
  const getVapidPublicKey = useCallback(async (): Promise<string> => {
    const response = await fetch(`${API_BASE_URL}/api/push/vapid-public-key`);
    if (!response.ok) {
      throw new Error('Не удалось получить VAPID ключ');
    }
    const data = await response.json();
    return data.publicKey;
  }, []);

  const subscribe = useCallback(async (userId: number): Promise<boolean> => {
    if (!state.isSupported) {
      setState(prev => ({ ...prev, error: 'Push-уведомления не поддерживаются' }));
      return false;
    }

    setState(prev => ({ ...prev, isLoading: true, error: null }));

    try {
      // Запрашиваем разрешение
      const permission = await requestPermission();
      if (permission !== 'granted') {
        setState(prev => ({
          ...prev,
          isLoading: false,
          error: 'Разрешение на уведомления не получено'
        }));
        return false;
      }

      // Регистрируем service worker
      const registration = await registerServiceWorker();

      // Получаем VAPID ключ
      const vapidPublicKey = await getVapidPublicKey();

      // Подписываемся на push
      const applicationServerKey = urlBase64ToUint8Array(vapidPublicKey);
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: applicationServerKey.buffer as ArrayBuffer
      });

      // Отправляем подписку на сервер
      const response = await fetch(`${API_BASE_URL}/api/push/subscribe`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: userId,
          endpoint: subscription.endpoint,
          p256dh: arrayBufferToBase64Url(subscription.getKey('p256dh')),
          auth: arrayBufferToBase64Url(subscription.getKey('auth')),
          userAgent: navigator.userAgent
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error('Server error:', errorText);
        setState(prev => ({
          ...prev,
          isLoading: false,
          error: 'Не удалось сохранить подписку на сервере'
        }));
        return false;
      }

      setState(prev => ({
        ...prev,
        isSubscribed: true,
        isLoading: false,
        error: null
      }));

      return true;
    } catch (error) {
      console.error('Push subscription error:', error);
      setState(prev => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Ошибка подписки'
      }));
      return false;
    }
  }, [state.isSupported, requestPermission, registerServiceWorker, getVapidPublicKey]);

  // Отписка от push-уведомлений
  const unsubscribe = useCallback(async (): Promise<boolean> => {
    setState(prev => ({ ...prev, isLoading: true, error: null }));

    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();

      if (subscription) {
        // Отменяем подписку в браузере
        await subscription.unsubscribe();

        // Отменяем подписку на сервере
        await fetch(`${API_BASE_URL}/api/push/unsubscribe`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            endpoint: subscription.endpoint
          })
        });
      }

      setState(prev => ({
        ...prev,
        isSubscribed: false,
        isLoading: false,
        error: null
      }));

      return true;
    } catch (error) {
      console.error('Push unsubscribe error:', error);
      setState(prev => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Ошибка отписки'
      }));
      return false;
    }
  }, []);

  const sendTestNotification = useCallback(() => {
    if (Notification.permission === 'granted') {
      new Notification('MusClub Manager', {
        body: 'Push-уведомления работают!',
        icon: '/icon-192x192.png'
      });
    }
  }, []);

  return {
    ...state,
    subscribe,
    unsubscribe,
    requestPermission,
    sendTestNotification
  };
}

export default usePushNotifications;
