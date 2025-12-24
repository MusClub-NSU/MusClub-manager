'use client';

import { Card, Button, Icon, Text, Loader } from '@gravity-ui/uikit';
import { Bookmark, HandPointRight, Plus, Pencil, TrashBin, Xmark, Clock, Persons, Ban, Check, Wrench, Person} from '@gravity-ui/icons';
import React, { useState } from 'react';
import { useSidebar } from '../context/SidebarContext';
import { useEvents } from '../../hooks/useApi';
import { Event } from '../../types/api';
import { useRouter } from 'next/navigation';

export default function EventsPage() {
    const { visible, setDisabled } = useSidebar();
    const { events, loading, error, createEvent, updateEvent, deleteEvent } = useEvents({ page: 0, size: 20 });
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

    const handleCreateEvent = async () => {
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
        <div
            className="
                overflow-y-auto
                flex flex-col gap-6
                px-5 py-6
                sm:px-10 sm:py-8
                md:px-20
                lg:px-40
                xl:px-60
                2xl:px-72
            "
        >
            <div className="flex justify-between items-center">
                <h1 className="text-3xl font-bold">Мероприятия</h1>
                <Button
                    view="action"
                    onClick={handleCreateEvent}
                    disabled={isCreating || visible}
                >
                    <span className="flex items-center justify-center gap-2">
                        <Plus size={16} />
                        <span>Добавить мероприятие</span>
                    </span>
                </Button>
            </div>

            {events.length === 0 ? (
                <Card className="p-8 text-center">
                    <Text variant="subheader-1" color="secondary">
                        Мероприятия не найдены
                    </Text>
                    <Text color="secondary" className="mt-2">
                        Добавьте первое мероприятие, нажав кнопку выше
                    </Text>
                </Card>
            ) : (
                events.map((event) => {
                    const { date, time } = formatDateTime(event.startTime);
                    return (
                        <Card
                            key={event.id}
                            view="raised"
                            type="container"
                            className="p-6 rounded-2xl shadow-md flex flex-col gap-4 event-card"
                        >
                            {/* Заголовок и время */}
                            <div className="flex flex-wrap justify-between items-center gap-2">
                                <h2 className="text-2xl md:text-3xl font-semibold">{event.title}</h2>
                                <span className="text-base md:text-lg">
                                    {date} • {time}
                                </span>
                            </div>

                            {/* Описание */}
                            {event.description && (
                                <div className="text-base text-[--foreground]/70">
                                    {event.description}
                                </div>
                            )}

                            {/* Локация */}
                            {event.venue && (
                                <div className="flex items-center gap-2 text-base md:text-lg">
                                    <Icon data={HandPointRight} size={18} />
                                    <span>{event.venue}</span>
                                </div>
                            )}

                            {/* Статус и действия */}
                            <div className="flex justify-between items-center">
                                <div className="flex items-center gap-2 text-base md:text-lg">
                                    <Icon data={Bookmark} size={18} />
                                    <span className="font-bold text-green-600">Активно</span>
                                </div>

                                <div className="flex gap-2">
                                    <Button
                                        view="flat"
                                        size="s"
                                        onClick={() => handleEditEvent(event)}
                                    >
                                        <Pencil size={14} />
                                    </Button>
                                    <Button
                                        view="flat"
                                        size="s"
                                        onClick={() => handleDeleteEvent(event.id)}
                                    >
                                        <TrashBin size={14} />
                                    </Button>
                                </div>
                            </div>

                            {/* Кнопка записи */}
                            <div className="mt-2">
                                <Button
                                    view="action"
                                    size="m"
                                    className="w-full sm:w-auto transition-opacity"
                                    onClick={() => router.push(`/events/${event.id}`)}
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
