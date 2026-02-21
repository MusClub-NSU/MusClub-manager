
self.addEventListener('push', (event) => {
  console.log('[SW] Push notification received:', event);
  console.log('[SW] Event.data:', event.data);
  console.log('[SW] Event.data type:', typeof event.data);
  
  let data = {
    title: 'MusClub Manager',
    body: 'Новое уведомление',
    icon: '/icon-192x192.png',
    badge: '/icon-192x192.png',
    tag: 'default',
    requireInteraction: false,
    actionUrl: '/'
  };

  if (event.data) {
    try {
      // Проверяем, есть ли данные
      const hasData = event.data.arrayBuffer || event.data.text || event.data.json;
      console.log('[SW] Has data methods:', { arrayBuffer: !!event.data.arrayBuffer, text: !!event.data.text, json: !!event.data.json });
      
      if (hasData) {
        try {
          const payload = event.data.json();
          console.log('[SW] Parsed payload:', payload);
          if (payload && typeof payload === 'object') {
            data = { ...data, ...payload };
          }
        } catch (jsonError) {
          console.warn('[SW] Failed to parse as JSON, trying text:', jsonError);
          try {
            const text = event.data.text();
            console.log('[SW] Data as text:', text);
            if (text) {
              try {
                const parsed = JSON.parse(text);
                data = { ...data, ...parsed };
              } catch {
                data.body = text;
              }
            }
          } catch (textError) {
            console.error('[SW] Failed to get text:', textError);
          }
        }
      } else {
        console.warn('[SW] Event.data exists but has no data methods');
      }
    } catch (e) {
      console.error('[SW] Failed to parse push data:', e);
    }
  } else {
    console.warn('[SW] No event.data');
  }

  console.log('[SW] Final notification data:', data);

  const options = {
    body: data.body,
    icon: data.icon,
    badge: data.badge,
    tag: data.tag || 'default',
    requireInteraction: data.requireInteraction || false,
    data: {
      actionUrl: data.actionUrl || '/',
      timestamp: Date.now()
    },
    vibrate: [100, 50, 100],
    actions: [
      {
        action: 'open',
        title: 'Открыть'
      },
      {
        action: 'dismiss',
        title: 'Закрыть'
      }
    ]
  };

  console.log('[SW] Showing notification with options:', options);

  event.waitUntil(
    self.registration.showNotification(data.title, options)
      .then(() => {
        console.log('[SW] ✅ Notification shown successfully');
      })
      .catch((error) => {
        console.error('[SW] ❌ Failed to show notification:', error);
      })
  );
});

self.addEventListener('notificationclick', (event) => {
  console.log('[SW] Notification clicked:', event);

  event.notification.close();

  const actionUrl = event.notification.data?.actionUrl || '/';

  if (event.action === 'dismiss') {
    return;
  }

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          client.navigate(actionUrl);
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(actionUrl);
      }
    })
  );
});

self.addEventListener('notificationclose', (event) => {
  console.log('[SW] Notification closed:', event);
});

self.addEventListener('pushsubscriptionchange', (event) => {
  console.log('[SW] Push subscription changed:', event);

  event.waitUntil(
    self.registration.pushManager.subscribe(event.oldSubscription.options)
      .then((subscription) => {
        // Отправить новую подписку на сервер
        return fetch('/api/push/subscribe', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            endpoint: subscription.endpoint,
            p256dh: arrayBufferToBase64(subscription.getKey('p256dh')),
            auth: arrayBufferToBase64(subscription.getKey('auth'))
          })
        });
      })
      .catch((error) => {
        console.error('[SW] Failed to resubscribe:', error);
      })
  );
});

function arrayBufferToBase64(buffer) {
  if (!buffer) return null;
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

self.addEventListener('activate', (event) => {
  console.log('[SW] Push service worker activated');
  event.waitUntil(self.clients.claim());
});

self.addEventListener('install', (event) => {
  console.log('[SW] Push service worker installed');
  self.skipWaiting();
});
