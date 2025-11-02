// Типы для API пользователей
export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  createdAt: string;
}

export interface UserCreateDto {
  username: string;
  email: string;
  role: string;
}

export interface UserUpdateDto {
  username?: string;
  email?: string;
  role?: string;
}

// Типы для API событий
export interface Event {
  id: number;
  title: string;
  description?: string;
  startTime: string;
  endTime?: string;
  venue?: string;
  createdAt: string;
}

export interface EventCreateDto {
  title: string;
  description?: string;
  startTime: string;
  endTime?: string;
  venue?: string;
}

export interface EventUpdateDto {
  title?: string;
  description?: string;
  startTime?: string;
  endTime?: string;
  venue?: string;
}

// Типы для пагинации
export interface Pageable {
  page: number;
  size: number;
  sort?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

