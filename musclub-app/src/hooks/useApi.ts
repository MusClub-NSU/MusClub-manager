'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../lib/api';
import type { User, Event, Pageable, SearchEntityType, SearchResult } from '../types/api';

const CACHE_TTL_MS = 5 * 60 * 1000;
const CACHE_UPDATED_EVENT = 'musclub-cache-updated';
const USERS_CACHE_SIZES = [20, 100, 999];
const EVENTS_CACHE_SIZES = [20, 100];

type CachedPayload<T> = {
  timestamp: number;
  data: T;
};

function buildCacheKey(prefix: string, pageable?: Pageable): string {
  const page = pageable?.page ?? 0;
  const size = pageable?.size ?? 20;
  const sort = pageable?.sort ?? '';
  return `${prefix}:page=${page}:size=${size}:sort=${sort}`;
}

function readCache<T>(key: string): T | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CachedPayload<T>;
    if (!parsed?.timestamp || !parsed?.data) return null;
    if (Date.now() - parsed.timestamp > CACHE_TTL_MS) return null;
    return parsed.data;
  } catch {
    return null;
  }
}

function writeCache<T>(key: string, data: T): void {
  if (typeof window === 'undefined') return;
  try {
    const payload: CachedPayload<T> = { timestamp: Date.now(), data };
    window.localStorage.setItem(key, JSON.stringify(payload));
  } catch {
    // no-op: cache failures should not break UI
  }
}

function notifyCacheUpdated(): void {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new Event(CACHE_UPDATED_EVENT));
}

export async function syncAllMainDataCaches(): Promise<void> {
  // Синхронизируем ключевые наборы данных для всех основных экранов.
  const [events20, events100, users20, users100, users999] = await Promise.all([
    apiClient.getEvents({ page: 0, size: 20 }),
    apiClient.getEvents({ page: 0, size: 100 }),
    apiClient.getUsers({ page: 0, size: 20 }),
    apiClient.getUsers({ page: 0, size: 100 }),
    apiClient.getUsers({ page: 0, size: 999 }),
  ]);

  writeCache(buildCacheKey('events', { page: 0, size: 20 }), {
    content: events20.content,
    totalElements: events20.totalElements,
    totalPages: events20.totalPages,
  });
  writeCache(buildCacheKey('events', { page: 0, size: 100 }), {
    content: events100.content,
    totalElements: events100.totalElements,
    totalPages: events100.totalPages,
  });

  writeCache(buildCacheKey('users', { page: 0, size: 20 }), {
    content: users20.content,
    totalElements: users20.totalElements,
    totalPages: users20.totalPages,
  });
  writeCache(buildCacheKey('users', { page: 0, size: 100 }), {
    content: users100.content,
    totalElements: users100.totalElements,
    totalPages: users100.totalPages,
  });
  writeCache(buildCacheKey('users', { page: 0, size: 999 }), {
    content: users999.content,
    totalElements: users999.totalElements,
    totalPages: users999.totalPages,
  });

  notifyCacheUpdated();
}

function writeUsersCacheForSize(size: number, payload: { content: User[]; totalElements: number; totalPages: number }): void {
  writeCache(buildCacheKey('users', { page: 0, size }), payload);
}

function writeEventsCacheForSize(size: number, payload: { content: Event[]; totalElements: number; totalPages: number }): void {
  writeCache(buildCacheKey('events', { page: 0, size }), payload);
}

function sortEventsByStartTimeDesc(events: Event[]): Event[] {
  return [...events].sort((a, b) => {
    const aTime = a.startTime ? new Date(a.startTime).getTime() : Number.NEGATIVE_INFINITY;
    const bTime = b.startTime ? new Date(b.startTime).getTime() : Number.NEGATIVE_INFINITY;

    // startTime DESC (nulls last)
    if (!a.startTime && b.startTime) return 1;
    if (a.startTime && !b.startTime) return -1;
    if (aTime !== bTime) return bTime - aTime;

    // fallback: id DESC for stable ordering
    return (b.id ?? 0) - (a.id ?? 0);
  });
}

// Хук для работы с пользователями
export function useUsers(pageable?: Pageable) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const cacheKey = buildCacheKey('users', pageable);

  const loadUsersFromCache = useCallback(() => {
    const cached = readCache<{ content: User[]; totalElements: number; totalPages: number }>(cacheKey);
    if (!cached) return false;
    setUsers(cached.content);
    setTotalElements(cached.totalElements);
    setTotalPages(cached.totalPages);
    return true;
  }, [cacheKey]);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.getUsers(pageable);
      setUsers(response.content);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
      writeCache(cacheKey, {
        content: response.content,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
      });
    } catch (err) {
      const cached = readCache<{ content: User[]; totalElements: number; totalPages: number }>(cacheKey);
      if (cached) {
        setUsers(cached.content);
        setTotalElements(cached.totalElements);
        setTotalPages(cached.totalPages);
        setError('Нет связи с сервером. Показаны сохранённые данные.');
      } else {
        setError(err instanceof Error ? err.message : 'Ошибка загрузки пользователей');
      }
    } finally {
      setLoading(false);
    }
  }, [cacheKey, pageable?.page, pageable?.size, pageable?.sort]);

  useEffect(() => {
    const hasCached = loadUsersFromCache();
    if (!hasCached) {
      void fetchUsers();
    } else {
      setLoading(false);
    }
  }, [fetchUsers, loadUsersFromCache]);

  useEffect(() => {
    const onCacheUpdated = () => {
      if (loadUsersFromCache()) setLoading(false);
    };
    window.addEventListener(CACHE_UPDATED_EVENT, onCacheUpdated);
    return () => window.removeEventListener(CACHE_UPDATED_EVENT, onCacheUpdated);
  }, [loadUsersFromCache]);

  const createUser = async (userData: { username: string; email: string; role: string; password: string }) => {
    try {
      const newUser = await apiClient.createUser(userData);
      setUsers(prev => {
        const next = [newUser, ...prev];
        writeUsersCacheForSize(pageable?.size ?? 20, {
          content: next,
          totalElements: totalElements + 1,
          totalPages,
        });
        return next;
      });
      void syncAllMainDataCaches();
      return newUser;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка создания пользователя');
      throw err;
    }
  };

  const updateUser = async (id: number, userData: Partial<{ username: string; email: string; role: string; password: string }>) => {
    try {
      const updatedUser = await apiClient.updateUser(id, userData);
      setUsers(prev => {
        const next = prev.map(user => user.id === id ? updatedUser : user);
        writeUsersCacheForSize(pageable?.size ?? 20, {
          content: next,
          totalElements,
          totalPages,
        });
        return next;
      });
      void syncAllMainDataCaches();
      return updatedUser;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка обновления пользователя');
      throw err;
    }
  };

  const deleteUser = async (id: number) => {
    try {
      await apiClient.deleteUser(id);
      setUsers(prev => {
        const next = prev.filter(user => user.id !== id);
        writeUsersCacheForSize(pageable?.size ?? 20, {
          content: next,
          totalElements: Math.max(0, totalElements - 1),
          totalPages,
        });
        return next;
      });
      void syncAllMainDataCaches();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления пользователя');
      throw err;
    }
  };

  return {
    users,
    loading,
    error,
    totalElements,
    totalPages,
    createUser,
    updateUser,
    deleteUser,
    refetch: fetchUsers,
  };
}

// Хук для работы с событиями
export function useEvents(pageable?: Pageable) {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const cacheKey = buildCacheKey('events', pageable);

  const loadEventsFromCache = useCallback(() => {
    const cached = readCache<{ content: Event[]; totalElements: number; totalPages: number }>(cacheKey);
    if (!cached) return false;
    setEvents(sortEventsByStartTimeDesc(cached.content));
    setTotalElements(cached.totalElements);
    setTotalPages(cached.totalPages);
    return true;
  }, [cacheKey]);

  const fetchEvents = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.getEvents(pageable);
      setEvents(sortEventsByStartTimeDesc(response.content));
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
      writeCache(cacheKey, {
        content: response.content,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
      });
    } catch (err) {
      const cached = readCache<{ content: Event[]; totalElements: number; totalPages: number }>(cacheKey);
      if (cached) {
        setEvents(sortEventsByStartTimeDesc(cached.content));
        setTotalElements(cached.totalElements);
        setTotalPages(cached.totalPages);
        setError('Нет связи с сервером. Показаны сохранённые данные.');
      } else {
        setError(err instanceof Error ? err.message : 'Ошибка загрузки событий');
      }
    } finally {
      setLoading(false);
    }
  }, [cacheKey, pageable?.page, pageable?.size, pageable?.sort]);

  useEffect(() => {
    const hasCached = loadEventsFromCache();
    if (!hasCached) {
      void fetchEvents();
    } else {
      setLoading(false);
    }
  }, [fetchEvents, loadEventsFromCache]);

  useEffect(() => {
    const onCacheUpdated = () => {
      if (loadEventsFromCache()) setLoading(false);
    };
    window.addEventListener(CACHE_UPDATED_EVENT, onCacheUpdated);
    return () => window.removeEventListener(CACHE_UPDATED_EVENT, onCacheUpdated);
  }, [loadEventsFromCache]);

  const createEvent = async (eventData: { 
    title: string; 
    description?: string; 
    startTime: string; 
    endTime?: string; 
    venue?: string; 
  }) => {
    try {
      const newEvent = await apiClient.createEvent(eventData);
      setEvents(prev => {
        const next = sortEventsByStartTimeDesc([newEvent, ...prev]);
        writeEventsCacheForSize(pageable?.size ?? 20, {
          content: next,
          totalElements: totalElements + 1,
          totalPages,
        });
        return next;
      });
      void syncAllMainDataCaches();
      return newEvent;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка создания события');
      throw err;
    }
  };

  const updateEvent = async (id: number, eventData: Partial<{ 
    title: string; 
    description?: string; 
    startTime: string; 
    endTime?: string; 
    venue?: string; 
  }>) => {
    try {
      const updatedEvent = await apiClient.updateEvent(id, eventData);
      setEvents(prev => {
        const next = sortEventsByStartTimeDesc(prev.map(event => event.id === id ? updatedEvent : event));
        writeEventsCacheForSize(pageable?.size ?? 20, {
          content: next,
          totalElements,
          totalPages,
        });
        return next;
      });
      void syncAllMainDataCaches();
      return updatedEvent;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка обновления события');
      throw err;
    }
  };

  const deleteEvent = async (id: number) => {
    try {
      await apiClient.deleteEvent(id);
      setEvents(prev => {
        const next = prev.filter(event => event.id !== id);
        writeEventsCacheForSize(pageable?.size ?? 20, {
          content: next,
          totalElements: Math.max(0, totalElements - 1),
          totalPages,
        });
        return next;
      });
      void syncAllMainDataCaches();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления события');
      throw err;
    }
  };

  return {
    events,
    loading,
    error,
    totalElements,
    totalPages,
    createEvent,
    updateEvent,
    deleteEvent,
    refetch: fetchEvents,
  };
}

export function useHybridSearch() {
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const search = useCallback(async (
    query: string,
    types: SearchEntityType[] = ['EVENT', 'USER'],
    pageable: Pageable = { page: 0, size: 20 },
  ) => {
    if (!query.trim()) {
      setResults([]);
      setError(null);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.hybridSearch(query.trim(), types, pageable);
      setResults(response.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка поиска');
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    results,
    loading,
    error,
    search,
  };
}

