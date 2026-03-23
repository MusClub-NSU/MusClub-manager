'use client';

import { Card, Button, Icon, Text, Loader } from '@gravity-ui/uikit';
import { Bookmark, HandPointRight, Plus, Pencil, TrashBin, Xmark } from '@gravity-ui/icons';
import React, { useEffect, useState } from 'react';
import { useSidebar } from '../context/SidebarContext';
import { useEvents, useHybridSearch } from '../../hooks/useApi';
import { Event, SearchResult } from '../../types/api';
import { useRouter } from 'next/navigation';
import { useCurrentUserRole } from '../../hooks/useCurrentUserRole';

export default function EventsPage() {
    const { visible, setDisabled } = useSidebar();
    const { canManageEvents } = useCurrentUserRole();
    const { events, loading, error, createEvent, updateEvent, deleteEvent } = useEvents({ page: 0, size: 20 });
    const { results: searchResults, loading: searchLoading, error: searchError, search } = useHybridSearch();
    const [searchQuery, setSearchQuery] = useState('');
    const [isCreating, setIsCreating] = useState(false);
    const [editingEvent, setEditingEvent] = useState<Event | null>(null);
    const [editFormData, setEditFormData] = useState({
        title: '',
        description: '',
        startTime: '',
        endTime: '',
        venue: ''
    });
    const router = useRouter();

    useEffect(() => {
        const timeout = setTimeout(() => {
            if (searchQuery.trim()) {
                search(searchQuery, ['EVENT'], { page: 0, size: 20 });
            }
        }, 250);
        return () => clearTimeout(timeout);
    }, [searchQuery, search]);

    const handleCreateEvent = async () => {
        if (!canManageEvents) return;
        setIsCreating(true);
        try {
            // Создаем время в будущем (через 1 час от текущего момента)
            const futureTime = new Date();
            futureTime.setHours(futureTime.getHours() + 1);
            
            await createEvent({
                title: 'Новое мероприятие',
                description: 'Описание нового мероприятия',
                startTime: futureTime.toISOString(),
                venue: 'Место проведения'
            });
        } catch (err) {
            console.error('Ошибка создания события:', err);
        } finally {
            setIsCreating(false);
        }
    };

    const handleDeleteEvent = async (id: number) => {
        if (confirm('Вы уверены, что хотите удалить это мероприятие?')) {
            try {
                await deleteEvent(id);
            } catch (err) {
                console.error('Ошибка удаления события:', err);
            }
        }
    };

    const formatDateForInput = (dateString: string): string => {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day}T${hours}:${minutes}`;
    };

    const handleEditEvent = (event: Event) => {
        setEditingEvent(event);
        setEditFormData({
            title: event.title,
            description: event.description || '',
            startTime: event.startTime ? formatDateForInput(event.startTime) : '',
            endTime: event.endTime ? formatDateForInput(event.endTime) : '',
            venue: event.venue || ''
        });
        setDisabled(true);
    };

    const handleCloseEdit = () => {
        setEditingEvent(null);
        setDisabled(false);
    };

    const handleSaveEdit = async () => {
        if (!editingEvent) return;

        try {
            // Преобразуем локальное время в ISO строку
            const startTime = editFormData.startTime 
                ? new Date(editFormData.startTime).toISOString() 
                : undefined;
            const endTime = editFormData.endTime 
                ? new Date(editFormData.endTime).toISOString() 
                : undefined;

            await updateEvent(editingEvent.id, {
                title: editFormData.title,
                description: editFormData.description || undefined,
                startTime: startTime,
                endTime: endTime || undefined,
                venue: editFormData.venue || undefined
            });
            handleCloseEdit();
        } catch (err) {
            console.error('Ошибка обновления события:', err);
        }
    };

    const formatDateTime = (dateTime: string) => {
        const date = new Date(dateTime);
        return {
            date: date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' }),
            time: date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
        };
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen p-4">
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text>Загрузка мероприятий...</Text>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center min-h-screen p-4">
                <Card className="p-6 max-w-md">
                    <Text color="danger">Ошибка: {error}</Text>
                    <Button
                        view="outlined"
                        onClick={() => window.location.reload()}
                        className="mt-4"
                    >
                        Попробовать снова
                    </Button>
                </Card>
            </div>
        );
    }

    return (
        <>
        <main className="min-h-screen overflow-y-auto p-5 sm:p-6 md:p-8 pt-8 sm:pt-10 md:pt-12">
            <div className="w-full flex flex-col gap-4">
            {/* Заголовок и кнопка создания */}
            <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-2">
                <div>
                    <h1 className="text-5xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent mb-1">Мероприятия</h1>
                </div>
                {canManageEvents && (
                    <Button
                        view="action"
                        onClick={handleCreateEvent}
                        disabled={isCreating || visible}
                        size="l"
                        className="px-6 py-3 bg-gradient-to-r from-blue-600 to-blue-500 hover:from-blue-700 hover:to-blue-600 text-white font-semibold shadow-lg hover:shadow-xl transition-all duration-200"
                    >
                        <span className="flex items-center justify-center gap-2">
                            <Icon data={Plus} size={18} />
                            <span>Добавить мероприятие</span>
                        </span>
                    </Button>
                )}
            </div>

            {/* Поиск */}
            <div className="relative group mb-2">
                <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Поиск по названию..."
                    className="w-full px-4 pr-12 py-3 rounded-xl border-2 bg-opacity-50 placeholder-opacity-50 focus:outline-none focus:ring-2 focus:ring-opacity-20 transition-all duration-200 shadow-sm text-base"
                    style={{
                        borderColor: 'var(--color-line-generic)',
                        backgroundColor: 'var(--color-background-secondary)',
                        color: 'var(--color-text-primary)'
                    }}
                />
                {searchQuery.trim() && (
                    <button
                        onClick={() => setSearchQuery('')}
                        className="absolute inset-y-0 right-0 pr-4 flex items-center opacity-40 hover:opacity-70 transition-opacity duration-150"
                    >
                        <Icon data={Xmark} size={16} />
                    </button>
                )}
            </div>
            {searchQuery.trim() && searchLoading && (
                <Text color="secondary" className="text-xs">Поиск...</Text>
            )}
            {searchQuery.trim() && searchError && (
                <Text color="danger" className="text-xs">Ошибка: {searchError}</Text>
            )}

            {searchQuery.trim() && !searchLoading && searchResults.filter((result) => result.entityType === 'EVENT').length === 0 ? (
                <Card className="p-8 text-center">
                    <Text variant="subheader-1" color="secondary">
                        По запросу ничего не найдено
                    </Text>
                </Card>
            ) : !searchQuery.trim() && events.length === 0 && !loading ? (
                <Card className="p-8 text-center">
                    <Text variant="subheader-1" color="secondary">
                        Мероприятия не найдены
                    </Text>
                    <Text color="secondary" className="mt-2">
                        Добавьте первое мероприятие, нажав кнопку выше
                    </Text>
                </Card>
            ) : (
                (searchQuery.trim()
                    ? searchResults.filter((result) => result.entityType === 'EVENT')
                    : events
                ).map((item) => {
                    const isSearchItem = 'entityId' in item;
                    const event = isSearchItem ? null : (item as Event);
                    const result = isSearchItem ? (item as SearchResult) : null;
                    const eventId = isSearchItem ? result.entityId : event!.id;
                    const matchedEvent = isSearchItem ? events.find((e) => e.id === result.entityId) : null;

                    const title = isSearchItem ? (matchedEvent?.title ?? result.title) : event!.title;
                    const description = isSearchItem
                        ? (matchedEvent?.description ?? result.snippet)
                        : event!.description;
                    const startTime = isSearchItem ? (matchedEvent?.startTime ?? null) : event!.startTime;
                    const dateTime = startTime ? formatDateTime(startTime) : null;
                    const venue = isSearchItem ? matchedEvent?.venue : event?.venue;

                    return (
                        <Card
                            key={eventId}
                            view="raised"
                            type="container"
                            className="p-6 rounded-2xl shadow-md flex flex-col gap-4 event-card"
                        >
                            {/* Заголовок и время */}
                            <div className="flex flex-wrap justify-between items-center gap-2">
                                <h2 className="text-2xl md:text-3xl font-semibold">{title}</h2>
                                {dateTime && (
                                    <span className="text-base md:text-lg">
                                        {dateTime.date} • {dateTime.time}
                                    </span>
                                )}
                            </div>

                            {/* Описание */}
                            {description && (
                                <div className="text-base text-[--foreground]/70">
                                    {description}
                                </div>
                            )}

                            {/* Локация */}
                            {venue && (
                                <div className="flex items-center gap-2 text-base md:text-lg">
                                    <Icon data={HandPointRight} size={18} />
                                    <span>{venue}</span>
                                </div>
                            )}

                            {/* Статус и действия */}
                            <div className="flex justify-between items-center">
                                <div className="flex items-center gap-2 text-base md:text-lg">
                                    <Icon data={Bookmark} size={18} />
                                    <span className="font-bold text-green-600">Активно</span>
                                </div>

                                {canManageEvents && (
                                    <div className="flex gap-2">
                                        <Button
                                            view="flat"
                                            size="s"
                                            onClick={() => {
                                                const target = isSearchItem ? matchedEvent : event;
                                                if (target) handleEditEvent(target);
                                            }}
                                            disabled={visible || (isSearchItem && !matchedEvent)}
                                        >
                                            <Icon data={Pencil} size={14} />
                                        </Button>
                                        <Button
                                            view="flat"
                                            size="s"
                                            onClick={() => {
                                                const target = isSearchItem ? matchedEvent : event;
                                                if (target) handleDeleteEvent(target.id);
                                            }}
                                            disabled={visible || (isSearchItem && !matchedEvent)}
                                        >
                                            <Icon data={TrashBin} size={14} />
                                        </Button>
                                    </div>
                                )}
                            </div>

                            {/* Кнопка записи */}
                            <div className="mt-2">
                                <Button
                                    view="action"
                                    size="m"
                                    className="w-full sm:w-auto transition-opacity"
                                    onClick={() => router.push(`/events/${eventId}`)}
                                    disabled = {visible}
                                >
                                    Подробнее
                                </Button>
                            </div>
                        </Card>
                    );
                })
            )}
            </div>
        </main>

            {editingEvent && (
                <div
                    className="fixed inset-0 flex items-center justify-center bg-background/70 backdrop-blur-sm z-50 px-6"
                    onClick={handleCloseEdit}
                >
                    <div
                        className="max-h-[90vh] w-full max-w-2xl overflow-y-auto"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <Card className="relative p-10 rounded-3xl bg-card shadow-2xl flex flex-col gap-6 text-foreground">
                            <Button
                                size="s"
                                view="flat"
                                className="absolute top-5 right-5"
                                onClick={handleCloseEdit}
                            >
                                <Icon data={Xmark} size={22} />
                            </Button>

                            <h2 className="text-3xl font-bold text-center">Редактировать мероприятие</h2>

                            <div className="flex flex-col gap-4">
                                <div>
                                    <label className="block text-sm font-medium mb-2">
                                        Название *
                                    </label>
                                    <input
                                        type="text"
                                        value={editFormData.title}
                                        onChange={(e) => setEditFormData({ ...editFormData, title: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">
                                        Описание
                                    </label>
                                    <textarea
                                        value={editFormData.description}
                                        onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                                        rows={4}
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">
                                        Дата и время начала *
                                    </label>
                                    <input
                                        type="datetime-local"
                                        value={editFormData.startTime}
                                        onChange={(e) => setEditFormData({ ...editFormData, startTime: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">
                                        Дата и время окончания
                                    </label>
                                    <input
                                        type="datetime-local"
                                        value={editFormData.endTime}
                                        onChange={(e) => setEditFormData({ ...editFormData, endTime: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">
                                        Место проведения
                                    </label>
                                    <input
                                        type="text"
                                        value={editFormData.venue}
                                        onChange={(e) => setEditFormData({ ...editFormData, venue: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div className="flex justify-end gap-3 mt-4">
                                    <Button
                                        view="outlined"
                                        onClick={handleCloseEdit}
                                    >
                                        Отмена
                                    </Button>
                                    <Button
                                        view="action"
                                        onClick={handleSaveEdit}
                                    >
                                        Сохранить
                                    </Button>
                                </div>
                            </div>
                        </Card>
                    </div>
                </div>
            )}
        </>

    );
}
