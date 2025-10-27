'use client';

import { Card, Button, Icon, Text, Loader } from '@gravity-ui/uikit';
import { Bookmark, HandPointRight, Plus, Pencil, TrashBin } from '@gravity-ui/icons';
import React, { useState } from 'react';
import { useSidebar } from '../context/SidebarContext';
import { useEvents } from '../../hooks/useApi';

export default function EventsPage() {
    const { visible } = useSidebar();
    const { events, loading, error, createEvent, updateEvent, deleteEvent } = useEvents({ page: 0, size: 20 });
    const [isCreating, setIsCreating] = useState(false);

    const handleCreateEvent = async () => {
        try {
            await createEvent({
                title: 'Новое мероприятие',
                description: 'Описание нового мероприятия',
                startTime: new Date().toISOString(),
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
                    disabled={isCreating}
                >
                    <Plus size={16} />
                    Добавить мероприятие
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
                                        onClick={() => console.log('Edit event', event.id)}
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
                                    onClick={() => alert(`Запись на ${event.title}`)}
                                    disabled={visible}
                                >
                                    Записаться
                                </Button>
                            </div>
                        </Card>
                    );
                })
            )}
        </div>
    );
}
