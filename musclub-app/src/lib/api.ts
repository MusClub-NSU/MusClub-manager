import { User, UserCreateDto, UserUpdateDto, Event, EventCreateDto, EventUpdateDto, Page, Pageable, EventMember, EventMemberUpsertDto } from '../types/api';

const API_BASE_URL = 'http://localhost:8080/api';

class ApiClient {
  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      // Пытаемся извлечь сообщение об ошибке из ответа
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        if (errorData.message) {
          errorMessage = errorData.message;
        } else if (typeof errorData === 'string') {
          errorMessage = errorData;
        } else if (errorData.error) {
          errorMessage = errorData.error;
        }
      } catch {
        // Если не удалось распарсить JSON, пытаемся прочитать как текст
        try {
          const text = await response.text();
          if (text) {
            errorMessage = text;
          }
        } catch {
          // Оставляем стандартное сообщение
        }
      }
      throw new Error(errorMessage);
    }

    // Если ответ пустой (например, для DELETE запросов)
    if (response.status === 204) {
      return {} as T;
    }

    return response.json();
  }

  // API для пользователей
  async getUsers(pageable?: Pageable): Promise<Page<User>> {
    const params = new URLSearchParams();
    if (pageable) {
      params.append('page', pageable.page.toString());
      params.append('size', pageable.size.toString());
      if (pageable.sort) {
        params.append('sort', pageable.sort);
      }
    }
    const queryString = params.toString();
    return this.request<Page<User>>(`/users${queryString ? `?${queryString}` : ''}`);
  }

  async getUser(id: number): Promise<User> {
    return this.request<User>(`/users/${id}`);
  }

  async createUser(user: UserCreateDto): Promise<User> {
    return this.request<User>('/users', {
      method: 'POST',
      body: JSON.stringify(user),
    });
  }

  async updateUser(id: number, user: UserUpdateDto): Promise<User> {
    return this.request<User>(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(user),
    });
  }

  async deleteUser(id: number): Promise<void> {
    return this.request<void>(`/users/${id}`, {
      method: 'DELETE',
    });
  }

  // API для событий
  async getEvents(pageable?: Pageable): Promise<Page<Event>> {
    const params = new URLSearchParams();
    if (pageable) {
      params.append('page', pageable.page.toString());
      params.append('size', pageable.size.toString());
      if (pageable.sort) {
        params.append('sort', pageable.sort);
      }
    }
    const queryString = params.toString();
    return this.request<Page<Event>>(`/events${queryString ? `?${queryString}` : ''}`);
  }

  async getEvent(id: number): Promise<Event> {
    return this.request<Event>(`/events/${id}`);
  }

  async createEvent(event: EventCreateDto): Promise<Event> {
    return this.request<Event>('/events', {
      method: 'POST',
      body: JSON.stringify(event),
    });
  }

  async updateEvent(id: number, event: EventUpdateDto): Promise<Event> {
    return this.request<Event>(`/events/${id}`, {
      method: 'PUT',
      body: JSON.stringify(event),
    });
  }

  async deleteEvent(id: number): Promise<void> {
    return this.request<void>(`/events/${id}`, {
      method: 'DELETE',
    });
  }

  // API для участников событий
  async getEventMembers(eventId: number): Promise<EventMember[]> {
    return this.request<EventMember[]>(`/events/${eventId}/members`);
  }

  async upsertEventMember(eventId: number, member: EventMemberUpsertDto): Promise<EventMember> {
    return this.request<EventMember>(`/events/${eventId}/members`, {
      method: 'POST',
      body: JSON.stringify(member),
    });
  }

  async removeEventMember(eventId: number, userId: number): Promise<void> {
    return this.request<void>(`/events/${eventId}/members/${userId}`, {
      method: 'DELETE',
    });
  }
}

export const apiClient = new ApiClient();

