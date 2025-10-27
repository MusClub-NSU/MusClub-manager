import { User, UserCreateDto, UserUpdateDto, Event, EventCreateDto, EventUpdateDto, Page, Pageable } from '../types/api';

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
      throw new Error(`HTTP error! status: ${response.status}`);
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
}

export const apiClient = new ApiClient();
