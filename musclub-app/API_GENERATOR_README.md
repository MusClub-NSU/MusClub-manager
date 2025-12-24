# Генератор API для MusClub Manager

## Что такое генератор API?

Генератор API автоматически создает TypeScript типы и методы для работы с бэкенд API на основе OpenAPI/Swagger спецификации.

## Преимущества использования:

1. **Автоматическая синхронизация** - типы всегда соответствуют бэкенду
2. **Типобезопасность** - TypeScript типы генерируются автоматически  
3. **Меньше ошибок** - нет ручного написания API клиента
4. **Быстрая разработка** - изменения в API автоматически отражаются во фронтенде

## Настройка Orval

### 1. Установка зависимостей

```bash
npm install --save-dev orval
# или с флагом для решения конфликтов зависимостей:
npm install --save-dev orval --legacy-peer-deps
```

### 2. Конфигурация

Файл `orval.config.js` уже настроен для работы с вашим API:

```javascript
export default {
  musclub: {
    input: {
      target: 'http://localhost:8080/api/docs', // Swagger UI endpoint
    },
    output: {
      target: './src/generated/api.ts',
      client: 'react-query', // или 'axios', 'fetch'
      override: {
        mutator: {
          path: './src/lib/api-mutator.ts',
          name: 'customInstance',
        },
      },
    },
  },
};
```

### 3. Генерация API клиента

```bash
npm run generate-api
```

Эта команда:
- Получит OpenAPI спецификацию с `http://localhost:8080/api/docs`
- Сгенерирует TypeScript типы и методы в `src/generated/api.ts`
- Использует кастомный мутатор из `src/lib/api-mutator.ts`

## Использование сгенерированного API

После генерации вы сможете использовать API так:

```typescript
import { useGetUsers, useCreateUser } from '../generated/api';

function UsersPage() {
  const { data: users, isLoading } = useGetUsers();
  const createUserMutation = useCreateUser();

  const handleCreateUser = () => {
    createUserMutation.mutate({
      username: 'Новый пользователь',
      email: 'user@example.com',
      role: 'MEMBER'
    });
  };

  if (isLoading) return <div>Загрузка...</div>;

  return (
    <div>
      {users?.content.map(user => (
        <div key={user.id}>{user.username}</div>
      ))}
      <button onClick={handleCreateUser}>Добавить пользователя</button>
    </div>
  );
}
```

## Альтернативы Orval

Если Orval не подходит, можно использовать:

1. **OpenAPI Generator** - универсальный генератор
2. **Swagger Codegen** - классический генератор  
3. **Ручное написание** - как мы сделали сейчас

## Текущее состояние

Сейчас API интегрирован вручную:
- ✅ Типы TypeScript созданы в `src/types/api.ts`
- ✅ API клиент создан в `src/lib/api.ts`
- ✅ React хуки созданы в `src/hooks/useApi.ts`
- ✅ Компоненты обновлены для использования API

Для автоматизации можно установить Orval и использовать команду `npm run generate-api`.

