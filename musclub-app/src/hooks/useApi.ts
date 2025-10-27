'use client';

import { useState, useEffect } from 'react';
import { apiClient } from '../lib/api';
import { User, Event, Pageable } from '../types/api';

// Хук для работы с пользователями
export function useUsers(pageable?: Pageable) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.getUsers(pageable);
      setUsers(response.content);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки пользователей');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [pageable?.page, pageable?.size, pageable?.sort]);

  const createUser = async (userData: { username: string; email: string; role: string }) => {
    try {
      const newUser = await apiClient.createUser(userData);
      setUsers(prev => [newUser, ...prev]);
      return newUser;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка создания пользователя');
      throw err;
    }
  };

  const updateUser = async (id: number, userData: Partial<{ username: string; email: string; role: string }>) => {
    try {
      const updatedUser = await apiClient.updateUser(id, userData);
      setUsers(prev => prev.map(user => user.id === id ? updatedUser : user));
      return updatedUser;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка обновления пользователя');
      throw err;
    }
  };

  const deleteUser = async (id: number) => {
    try {
      await apiClient.deleteUser(id);
      setUsers(prev => prev.filter(user => user.id !== id));
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

  const fetchEvents = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.getEvents(pageable);
      setEvents(response.content);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки событий');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, [pageable?.page, pageable?.size, pageable?.sort]);

  const createEvent = async (eventData: { 
    title: string; 
    description?: string; 
    startTime: string; 
    endTime?: string; 
    venue?: string; 
  }) => {
    try {
      const newEvent = await apiClient.createEvent(eventData);
      setEvents(prev => [newEvent, ...prev]);
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
      setEvents(prev => prev.map(event => event.id === id ? updatedEvent : event));
      return updatedEvent;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка обновления события');
      throw err;
    }
  };

  const deleteEvent = async (id: number) => {
    try {
      await apiClient.deleteEvent(id);
      setEvents(prev => prev.filter(event => event.id !== id));
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
