// Мутатор для Orval - кастомная логика для HTTP запросов

interface RequestConfig {
  url: string;
  method: string;
  data?: unknown;
  params?: Record<string, unknown>;
}

export const customInstance = async <T>(config: RequestConfig): Promise<T> => {
  const { url, method, data, params } = config;
  
  try {
    let endpoint = url;
    
    // Добавляем параметры запроса
    if (params) {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          searchParams.append(key, String(value));
        }
      });
      const queryString = searchParams.toString();
      if (queryString) {
        endpoint += `?${queryString}`;
      }
    }

    // Выполняем запрос через наш API клиент
    const response = await fetch(endpoint, {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: data ? JSON.stringify(data) : undefined,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    // Если ответ пустой (например, для DELETE запросов)
    if (response.status === 204) {
      return {} as T;
    }

    return response.json();
  } catch (error) {
    console.error('API request failed:', error);
    throw error;
  }
};

export default customInstance;

