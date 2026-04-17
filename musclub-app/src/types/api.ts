// Типы для API пользователей
export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  createdAt: string;
  avatarUrl?: string;
}

export interface UserCreateDto {
  username: string;
  email: string;
  role: string;
  password: string;
}

export interface UserUpdateDto {
  username?: string;
  email?: string;
  role?: string;
  password?: string;
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
  aiDescription?: string;
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

// Типы для участников событий
export interface EventMember {
  userId: number;
  username: string;
  email: string;
  role: string;
  addedAt: string;
}

export interface EventMemberUpsertDto {
  userId: number;
  role: string;
}

// Типы для ИИ-описания афиши
export interface PosterDescriptionResponse {
  description: string;
}

export interface EventTimelineItem {
  id: number;
  plannedTime: string;
  description: string;
  position: number;
}

export interface EventTimelineItemCreateDto {
  plannedTime: string;
  description: string;
}

export interface EventTimelineItemUpdateDto {
  plannedTime: string;
  description: string;
}

export interface EventProgramItem {
  id: number;
  title: string;
  artist?: string;
  plannedTime?: string;
  durationText?: string;
  notes?: string;
  position: number;
}

export interface EventProgramItemCreateDto {
  title: string;
  artist?: string;
  plannedTime?: string;
  durationText?: string;
  notes?: string;
}

export interface EventProgramItemUpdateDto {
  title: string;
  artist?: string;
  plannedTime?: string;
  durationText?: string;
  notes?: string;
}

export type SearchEntityType = 'EVENT' | 'USER';

export interface SearchResult {
  entityType: SearchEntityType;
  entityId: number;
  title: string;
  snippet: string;
  score: number;
  lexicalScore: number;
  vectorScore: number;
}

