# MusClub — Инструкция по развёртыванию на production

## 📋 Краткий старт (5 минут)

```bash
# 1. Клонировать репозиторий
git clone https://github.com/YOUR_USERNAME/musclub.git musclub
cd musclub

# 2. Скопировать шаблон переменных окружения
cp .env.example .env

# 3. Отредактировать .env (заполнить 3 поля ниже)
nano .env

# 4. Запустить контейнеры
docker compose up -d --build
```

---

## 🔧 Конфигурация (.env)

Откройте файл `.env` и **заполните эти поля**:

| Переменная | Описание | Пример |
|---|---|---|
| `HTTP_PORT` | Порт, на котором будет доступен сайт | `80` или `8090` |
| `SERVER_HOST` | IP или доменное имя сервера | `192.168.1.100` или `musclub.example.com` |
| `SERVER_PROTOCOL` | HTTP или HTTPS | `http` (или `https` если настроен SSL) |
| `NEXTAUTH_SECRET` | Секретный ключ для Next.js | Генерируйте: `openssl rand -hex 32` |
| `KEYCLOAK_ADMIN_PASSWORD` | Пароль администратора Keycloak | Любой сложный пароль |
| `POSTGRES_PASSWORD` | Пароль базы данных | Любой сложный пароль |
| `DEEPSEEK_API_KEY` | API ключ для AI функций | Получить на https://platform.deepseek.com |
| `VAPID_*_KEY` | Ключи для push-уведомлений | Генерировать: `npx web-push generate-vapid-keys` |

### Пример заполненного `.env`:

```env
HTTP_PORT=8090
SERVER_HOST=192.168.1.100
SERVER_PROTOCOL=http
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=SecurePassword123!
POSTGRES_DB=musclub
POSTGRES_USER=musclub
POSTGRES_PASSWORD=SecureDBPassword456!
NEXTAUTH_SECRET=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
DEEPSEEK_API_KEY=sk-YOUR_ACTUAL_KEY_HERE
VAPID_PUBLIC_KEY=YOUR_PUBLIC_KEY_HERE
VAPID_PRIVATE_KEY=YOUR_PRIVATE_KEY_HERE
MUSCLUB_DISPLAY_TIMEZONE=Europe/Berlin
```

---

## 🏗️ Архитектура развёртывания

```
┌─────────────────────────────────────────────────┐
│           Интернет / Пользователи               │
└────────────────────┬────────────────────────────┘
                     │ HTTP_PORT:80 или 8090
                     │
        ┌────────────▼───────────┐
        │   NGINX (reverse proxy)  │
        │  :80 → внутренние URL    │
        └────────────┬────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
    ▼                ▼                ▼
/api/* → Spring Boot  /auth/* → Keycloak   /* → Next.js
app:8080             keycloak:8180       frontend:3000
    │                │                │
    └────────────────┼────────────────┘
                     │
            ┌────────▼────────┐
            │  PostgreSQL DB   │
            │  db:5432        │
            │ (не открыта!)    │
            └─────────────────┘
```

**Ключевой момент:** Все сервисы находятся в изолированной Docker-сети. Наружу открыт **только nginx на HTTP_PORT**.

---

## 🚀 Запуск контейнеров

### Проверка предварительных требований:

```bash
# Проверить, установлен ли Docker
docker --version

# Проверить Docker Compose
docker compose version

# Проверить, свободны ли необходимые порты
lsof -i :80       # или ваш HTTP_PORT
```

### Команды управления:

```bash
# Запустить в фоновом режиме с пересборкой образов
docker compose up -d --build

# Просмотреть логи всех контейнеров
docker compose logs -f

# Просмотреть логи конкретного сервиса
docker compose logs -f app
docker compose logs -f keycloak
docker compose logs -f frontend

# Проверить статус контейнеров
docker compose ps

# Остановить контейнеры
docker compose down

# Перезапустить контейнеры (без пересборки)
docker compose restart

# Удалить всё (включая база данных!)
docker compose down -v
```

---

## 📍 Доступ к компонентам

После успешного запуска:

| Компонент | URL | Логин | Пароль |
|---|---|---|---|
| **Сайт MusClub** | `http://<SERVER_HOST>:<HTTP_PORT>` | — | — |
| **Keycloak Admin** | `http://<SERVER_HOST>:<HTTP_PORT>/auth/admin` | admin | `KEYCLOAK_ADMIN_PASSWORD` из .env |
| **Swagger API** | `http://<SERVER_HOST>:<HTTP_PORT>/swagger-ui/` | — | — |
| **API docs** | `http://<SERVER_HOST>:<HTTP_PORT>/v3/api-docs` | — | — |

### Тестовые пользователи (импортируются автоматически):

```
Организатор:
  Email: organizer@example.com
  Пароль: musclub123

Участник:
  Email: member@example.com
  Пароль: musclub123
```
