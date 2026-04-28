/**
 * Хук для управления push-уведомлениями
 */
'use client';

import { useState, useEffect, useCallback } from 'react';

// По умолчанию используем относительный "/api/*" (Next.js proxy -> backend через rewrites).
// Можно переопределить через:
// 1. Переменную окружения NEXT_PUBLIC_API_URL
// 2. window.ENV_API_URL (например, "https://my-backend.example.com")
const API_BASE_URL =
  typeof window !== 'undefined'
    ? (window as unknown as { ENV_API_URL?: string }).ENV_API_URL || 
      (process.env.NEXT_PUBLIC_API_URL || '/api')
    : (process.env.NEXT_PUBLIC_API_URL || '/api');

/** Временная константа userId для авто-подписки (пока без авторизации) */
const DEFAULT_USER_ID = 1;

/** Глобальный флаг, чтобы не дублировать авто-подписку при нескольких экземплярах хука */
let globalHasAttemptedAutoSubscribe = false;

/** Формирует полный URL для push API (с учётом /api в пути) */
function getPushApiUrl(endpoint: string): string {
  if (API_BASE_URL === '/api') return `/api/push/${endpoint}`;
  const base = API_BASE_URL.replace(/\/$/, '');
  const apiBase = base.includes('/api') ? base : `${base}/api`;
  return `${apiBase}/push/${endpoint}`;
}

/** Заголовки для запросов (ngrok free требует ngrok-skip-browser-warning для API) */
const API_HEADERS: HeadersInit = {
  'Content-Type': 'application/json',
  'ngrok-skip-browser-warning': 'true'
};

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
      // Минимум: Service Worker и Notifications API (PushManager проверяется при подписке)
      const hasServiceWorker = typeof navigator !== 'undefined' && 'serviceWorker' in navigator;
      const hasNotification = typeof window !== 'undefined' && 'Notification' in window;
      const isSecureContext = typeof window !== 'undefined' && window.isSecureContext;

      if (!hasServiceWorker || !hasNotification) {
        setState(prev => ({
          ...prev,
          isSupported: false,
          isLoading: false,
          error: 'Push-уведомления не поддерживаются в этом браузере'
        }));
        return;
      }

      if (!isSecureContext) {
        setState(prev => ({
          ...prev,
          isSupported: false,
          isLoading: false,
          error: 'Для push нужен HTTPS или localhost'
        }));
        return;
      }

      // Проверяем текущее разрешение
      const permission = Notification.permission;

      // Регистрируем SW если ещё не зарегистрирован, затем проверяем подписку
      try {
        let registration = navigator.serviceWorker.controller
          ? await navigator.serviceWorker.getRegistration()
          : null;
        if (!registration) {
          registration = await navigator.serviceWorker.register('/push-sw.js', { scope: '/' });
          await navigator.serviceWorker.ready;
        } else {
          await navigator.serviceWorker.ready;
        }
        const pushManager = registration?.pushManager;
        if (!pushManager) {
          setState(prev => ({
            ...prev,
            isSupported: false,
            isLoading: false,
            error: 'Push не поддерживается в этом браузере'
          }));
          return;
        }
        const subscription = await pushManager.getSubscription();
        
        // Если есть подписка в браузере, проверяем, сохранена ли она на сервере
        if (subscription) {
          try {
            const checkUrl = getPushApiUrl(`subscriptions/${DEFAULT_USER_ID}`);
            const checkResponse = await fetch(checkUrl, { headers: API_HEADERS });
            if (checkResponse.ok) {
              const serverSubscriptions = await checkResponse.json().catch(() => []);
              const isOnServer = Array.isArray(serverSubscriptions) && 
                serverSubscriptions.some((sub: { endpoint?: string }) => sub.endpoint === subscription.endpoint);
              
              if (!isOnServer) {
                // Подписка есть в браузере, но не на сервере - синхронизируем
                console.log('[Push] Subscription exists in browser but not on server, syncing...');
                const syncResponse = await fetch(getPushApiUrl('subscribe'), {
                  method: 'POST',
                  headers: API_HEADERS,
                  body: JSON.stringify({
                    userId: DEFAULT_USER_ID,
                    endpoint: subscription.endpoint,
                    p256dh: arrayBufferToBase64Url(subscription.getKey('p256dh')),
                    auth: arrayBufferToBase64Url(subscription.getKey('auth')),
                    userAgent: navigator.userAgent
                  })
                });
                if (syncResponse.ok) {
                  console.log('[Push] Subscription synced to server');
                }
              }
            }
          } catch (e) {
            console.warn('[Push] Failed to check server subscriptions:', e);
          }
        }

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
    try {
      const response = await fetch(getPushApiUrl('vapid-public-key'), { headers: API_HEADERS });
      if (!response.ok) {
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
          const data = await response.json().catch(() => ({} as { message?: string }));
          throw new Error(data.message || 'Не удалось получить VAPID ключ');
        }
        // Для HTML/прокси-ответов не показываем "красную" ошибку Next.js.
        throw new Error('Push-сервис временно недоступен');
      }
      const data = await response.json();
      return data.publicKey;
    } catch {
      throw new Error('Push-сервис временно недоступен');
    }
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
      const response = await fetch(getPushApiUrl('subscribe'), {
        method: 'POST',
        headers: API_HEADERS,
        body: JSON.stringify({
          userId: userId,
          endpoint: subscription.endpoint,
          p256dh: arrayBufferToBase64Url(subscription.getKey('p256dh')),
          auth: arrayBufferToBase64Url(subscription.getKey('auth')),
          userAgent: navigator.userAgent
        })
      });

      if (!response.ok) {
        const contentType = response.headers.get('content-type');
        let errorMessage = 'Не удалось сохранить подписку на сервере';
        
        if (contentType?.includes('application/json')) {
          try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorData.error || errorMessage;
          } catch {
            // ignore
          }
        } else {
          // Если получили HTML (например, страницу ошибки ngrok)
          const errorText = await response.text();
          console.error('Server returned HTML instead of JSON:', errorText.substring(0, 200));
          
          if (errorText.includes('ngrok') || errorText.includes('ERR_NGROK')) {
            errorMessage = 'Бэкенд недоступен. Проверьте подключение к серверу. Для мобильного устройства установите window.ENV_API_URL с URL бэкенда через ngrok.';
          } else {
            errorMessage = `Ошибка сервера (${response.status}): ${errorText.substring(0, 100)}`;
          }
        }
        
        setState(prev => ({
          ...prev,
          isLoading: false,
          error: errorMessage
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
      // Не засоряем консоль "ошибками" в dev, если push endpoint сейчас недоступен.
      console.warn('Push subscription skipped:', error);
      setState(prev => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Ошибка подписки'
      }));
      return false;
    }
  }, [state.isSupported, requestPermission, registerServiceWorker, getVapidPublicKey]);

  // Периодическая проверка подписки (синхронизация при нескольких экземплярах хука)
  useEffect(() => {
    if (!state.isSupported || state.isSubscribed || state.isLoading) return;
    const interval = setInterval(async () => {
      try {
        const reg = await navigator.serviceWorker.ready;
        const sub = await reg.pushManager.getSubscription();
        if (sub) {
          setState(prev => ({ ...prev, isSubscribed: true }));
        }
      } catch {
        // ignore
      }
    }, 1500);
    return () => clearInterval(interval);
  }, [state.isSupported, state.isSubscribed, state.isLoading]);

  // Автоматическая подписка при загрузке (если поддерживается и ещё не подписан)
  useEffect(() => {
    if (
      state.isSupported &&
      !state.isSubscribed &&
      !state.isLoading &&
      state.permission !== 'denied' &&
      !globalHasAttemptedAutoSubscribe
    ) {
      globalHasAttemptedAutoSubscribe = true;
      subscribe(DEFAULT_USER_ID);
    }
  }, [state.isSupported, state.isSubscribed, state.isLoading, state.permission, subscribe]);

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
        await fetch(getPushApiUrl('unsubscribe'), {
          method: 'POST',
          headers: API_HEADERS,
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

  const sendTestNotification = useCallback(async () => {
    setState(prev => ({ ...prev, error: null }));
    try {
      const url = getPushApiUrl('send');
      console.log('[Push] Sending test notification to:', url, 'API_BASE_URL:', API_BASE_URL);
      
      const response = await fetch(url, {
        method: 'POST',
        headers: API_HEADERS,
        body: JSON.stringify({
          userId: DEFAULT_USER_ID,
          title: 'MusClub Manager',
          body: 'Push-уведомления работают!'
        })
      });
      
      console.log('[Push] Response status:', response.status, 'URL:', response.url);
      
      const contentType = response.headers.get('content-type');
      const data = contentType?.includes('application/json')
        ? await response.json().catch(() => ({}))
        : {};
      
      if (!response.ok) {
        const msg = (data as { message?: string }).message;
        const errText = typeof data === 'string' ? data : '';
        const errorDetails = `Ошибка ${response.status}: ${msg || errText || 'Неизвестная ошибка'}. URL: ${url}`;
        console.error('[Push] Error:', errorDetails, data);
        setState(prev => ({
          ...prev,
          error: `Ошибка ${response.status}. ${msg || 'Проверьте консоль браузера для деталей.'}`
        }));
        return;
      }
      const sentCount = (data as { sentCount?: number }).sentCount ?? 0;
      console.log('[Push] Sent to', sentCount, 'devices');
      
      // Проверяем подписки на сервере для отладки
      try {
        const checkUrl = getPushApiUrl(`subscriptions/${DEFAULT_USER_ID}`);
        const checkResponse = await fetch(checkUrl, { headers: API_HEADERS });
        if (checkResponse.ok) {
          const serverSubscriptions = await checkResponse.json().catch(() => []);
          console.log('[Push] Server subscriptions for user:', serverSubscriptions);
        }
      } catch (e) {
        console.warn('[Push] Failed to check server subscriptions:', e);
      }
      
      if (sentCount === 0) {
        setState(prev => ({ ...prev, error: `Нет активных подписок (отправлено: ${sentCount}). Откройте сайт на телефоне и ПК, разрешите уведомления. Проверьте консоль для деталей.` }));
      } else {
        setState(prev => ({ ...prev, error: null }));
        console.log('[Push] ✅ Notification sent successfully to', sentCount, 'device(s)');
      }
    } catch (e) {
      console.error('[Push] Failed to send test notification:', e);
      setState(prev => ({
        ...prev,
        error: `Сеть недоступна: ${e instanceof Error ? e.message : 'Неизвестная ошибка'}. Проверьте интернет и что бэкенд запущен.`
      }));
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
