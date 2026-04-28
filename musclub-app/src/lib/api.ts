import { getSession } from 'next-auth/react';
import {
  User,
  UserCreateDto,
  UserUpdateDto,
  Event,
  EventCreateDto,
  EventUpdateDto,
  Page,
  Pageable,
  EventMember,
  EventMemberUpsertDto,
  PosterDescriptionResponse,
  EventTimelineItem,
  EventTimelineItemCreateDto,
  EventTimelineItemUpdateDto,
  EventProgramItem,
  EventProgramItemCreateDto,
  EventProgramItemUpdateDto,
  SearchEntityType,
  SearchResult,
} from '@/types/api';

// По умолчанию ходим на "/api" (Next.js proxy -> backend через rewrites в next.config.ts).
const API_BASE_URL =
  typeof window !== 'undefined'
    ? (window as unknown as { ENV_API_URL?: string }).ENV_API_URL || '/api'
    : '/api';

class ApiClient {
  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;

    // Получаем access token из сессии NextAuth и добавляем в заголовок
    const session = await getSession();
    const authHeaders: Record<string, string> = {};
    if (session?.accessToken) {
      authHeaders['Authorization'] = `Bearer ${session.accessToken}`;
    }

    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders,
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

  private async requestWithoutJsonContentType<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;
    const response = await fetch(url, options);

    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        if (errorData.message) {
          errorMessage = errorData.message;
        }
      } catch {
        try {
          const text = await response.text();
          if (text) {
            errorMessage = text;
          }
        } catch {
        }
      }
      throw new Error(errorMessage);
    }

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

  async getCurrentUser(): Promise<User> {
    return this.request<User>('/users/me');
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

  async updateUserPassword(id: number, password: string): Promise<void> {
    return this.request<void>(`/users/${id}/password`, {
      method: 'PUT',
      body: JSON.stringify({ password }),
    });
  }

  async deleteUser(id: number): Promise<void> {
    return this.request<void>(`/users/${id}`, {
      method: 'DELETE',
    });
  }

  async uploadUserAvatar(id: number, file: File): Promise<User> {
    const formData = new FormData();
    formData.append('file', file);
    return this.requestWithoutJsonContentType<User>(
      `/users/${id}/avatar`,
      {
        method: 'POST',
        body: formData,
      } as RequestInit,
    );
  }

  async deleteUserAvatar(id: number): Promise<void> {
    return this.request<void>(`/users/${id}/avatar`, {
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

  // AI-построение описания афиши
  async generatePosterDescription(eventId: number, save: boolean = false): Promise<PosterDescriptionResponse> {
    return this.request<PosterDescriptionResponse>(`/events/${eventId}/poster-description/ai?save=${save}`, {
      method: 'POST',
    });
  }

  async getEventTimeline(eventId: number): Promise<EventTimelineItem[]> {
    return this.request<EventTimelineItem[]>(`/events/${eventId}/timeline`);
  }

  async createEventTimelineItem(eventId: number, dto: EventTimelineItemCreateDto): Promise<EventTimelineItem> {
    return this.request<EventTimelineItem>(`/events/${eventId}/timeline`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  async updateEventTimelineItem(eventId: number, itemId: number, dto: EventTimelineItemUpdateDto): Promise<EventTimelineItem> {
    return this.request<EventTimelineItem>(`/events/${eventId}/timeline/${itemId}`, {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
  }

  async deleteEventTimelineItem(eventId: number, itemId: number): Promise<void> {
    return this.request<void>(`/events/${eventId}/timeline/${itemId}`, {
      method: 'DELETE',
    });
  }

  async reorderEventTimeline(eventId: number, itemIds: number[]): Promise<EventTimelineItem[]> {
    return this.request<EventTimelineItem[]>(`/events/${eventId}/timeline/reorder`, {
      method: 'PUT',
      body: JSON.stringify(itemIds),
    });
  }

  async getEventProgram(eventId: number): Promise<EventProgramItem[]> {
    return this.request<EventProgramItem[]>(`/events/${eventId}/program`);
  }

  async createEventProgramItem(eventId: number, dto: EventProgramItemCreateDto): Promise<EventProgramItem> {
    return this.request<EventProgramItem>(`/events/${eventId}/program`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  async updateEventProgramItem(eventId: number, itemId: number, dto: EventProgramItemUpdateDto): Promise<EventProgramItem> {
    return this.request<EventProgramItem>(`/events/${eventId}/program/${itemId}`, {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
  }

  async deleteEventProgramItem(eventId: number, itemId: number): Promise<void> {
    return this.request<void>(`/events/${eventId}/program/${itemId}`, {
      method: 'DELETE',
    });
  }

  async reorderEventProgram(eventId: number, itemIds: number[]): Promise<EventProgramItem[]> {
    return this.request<EventProgramItem[]>(`/events/${eventId}/program/reorder`, {
      method: 'PUT',
      body: JSON.stringify(itemIds),
    });
  }

  async hybridSearch(
    query: string,
    types: SearchEntityType[] = ['EVENT', 'USER'],
    pageable: Pageable = { page: 0, size: 20 },
  ): Promise<Page<SearchResult>> {
    const params = new URLSearchParams();
    params.append('q', query);
    params.append('page', pageable.page.toString());
    params.append('size', pageable.size.toString());
    if (pageable.sort) {
      params.append('sort', pageable.sort);
    }
    types.forEach((type) => params.append('types', type));
    return this.request<Page<SearchResult>>(`/search/hybrid?${params.toString()}`);
  }
}

export const apiClient = new ApiClient();

