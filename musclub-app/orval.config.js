// Конфигурация для Orval (генератор API)
// Этот файл показывает, как настроить автоматическую генерацию API клиента

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
