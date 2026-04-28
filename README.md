# MusClub Manager

MusClub Manager - веб-приложение для управления мероприятиями музыкального клуба: участники, события, программа, таймлайн, push-уведомления и гибридный поиск.

## Что входит в проект
- `musclub-app` - фронтенд на Next.js 15 / React / TypeScript / PWA.
- `backend` - Spring Boot 3.3 API на Java 21.
- PostgreSQL 16 + `pgvector`.
- Keycloak 24 для аутентификации и ролей.

## Локальный запуск через Docker Compose

### Что нужно
- Docker Desktop
- Docker Compose

### Один запуск для всего стека
Из корня репозитория:

```bash
docker compose -f backend/docker-compose.yml up --build
```

Или из папки `backend`:

```bash
docker compose up --build
```

После старта будут доступны:
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger`
- Keycloak: `http://localhost:8180`
- PostgreSQL: `localhost:5433`

### Что поднимется
- `frontend` - production-сборка Next.js в контейнере
- `app` - Spring Boot backend
- `keycloak` - локальный сервер авторизации с импортом realm
- `db` - PostgreSQL с `pgvector` (внутри создаются отдельные базы `musclub` и `keycloak`)

### Локальные папки для данных
Compose использует локальные директории репозитория:
- `backend/data/postgres` - данные PostgreSQL
- `backend/keycloak` - realm import для Keycloak

Если нужен полностью чистый запуск, остановите контейнеры и удалите `backend/data/postgres`.

## Keycloak и вход в систему

При старте автоматически импортируется realm `musclub` из `backend/keycloak/musclub-realm.json`.

### Данные Keycloak
- Realm: `musclub`
- Client: `musclub-frontend`
- Keycloak admin console: `http://localhost:8180/admin`
- Администратор Keycloak: `admin / admin`

### Демо-пользователи приложения
Эти пользователи импортируются в Keycloak и одновременно добавляются в базу приложения:

- `organizer / musclub123` - роль `ORGANIZER`
- `member / musclub123` - роль `MEMBER`

### Сценарий проверки
1. Откройте `http://localhost:3000`.
2. Нажмите `Войти через Keycloak`.
3. Войдите под `organizer / musclub123`.
4. После успешного логина вы вернетесь в приложение.
5. Проверьте создание участников и мероприятий.

## Как добавить демонстрационные данные

Есть два простых пути:
- войти под `organizer` и создать участников/мероприятия через UI;
- при необходимости зайти в Keycloak Admin Console и посмотреть/изменить пользователей realm `musclub`.

Приложение уже синхронизирует пользовательские записи между своей БД и Keycloak, поэтому для обычной демонстрации удобнее добавлять пользователей из интерфейса приложения.

## Переменные окружения

Примеры:
- `backend/.env.example`
- `musclub-app/.env.example`

Ключевые переменные для локального запуска уже прописаны в `backend/docker-compose.yml`, поэтому отдельные `.env` для старта не обязательны.

## Локальная разработка без Docker

Если нужно запускать сервисы по отдельности:

1. Поднимите `db` и `keycloak` через compose.
2. Запустите backend из `backend`.
3. Запустите frontend из `musclub-app`.

Важно:
- фронтенд использует стандартный OIDC login flow через Keycloak;
- backend валидирует JWT, выданные realm `musclub`;
- для браузера и для Docker-сети используются разные внутренние URL, это уже учтено в compose-конфиге.

## Полезные разделы проекта
- Push-уведомления через Web Push API
- Гибридный поиск `FTS + pgvector`
- Swagger / OpenAPI для тестирования API

## Частые проблемы

### Порт уже занят
Проверьте, что порты `3000`, `5433`, `8080`, `8180` свободны.

Если `3000` уже занят локальным dev-сервером или другим процессом, контейнер `frontend` не сможет стартовать. Освободите порт или смените опубликованный порт в compose и одновременно обновите:
- `NEXTAUTH_URL`
- redirect URI / web origin у клиента `musclub-frontend` в Keycloak

### Keycloak не принимает логин после изменения конфигурации
Сделайте чистый перезапуск:

```bash
docker compose -f backend/docker-compose.yml down
docker compose -f backend/docker-compose.yml up --build
```

Если меняли realm/import и хотите гарантированно пересоздать базу:

```bash
docker compose -f backend/docker-compose.yml down
```

затем удалите `backend/data/postgres` и запустите compose заново.

### Frontend открылся, но вход не работает
Проверьте:
- что `keycloak` поднялся на `http://localhost:8180`;
- что realm `musclub` импортировался;
- что вход выполняется через пользователя из списка выше.
