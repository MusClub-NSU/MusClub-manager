'use client';

import { useParams, useRouter } from 'next/navigation';
import { Button, Icon, Text, Loader } from '@gravity-ui/uikit';
import { Bookmark, HandPointRight, Clock, Persons, Ban, Check, Wrench, Person, Xmark } from '@gravity-ui/icons';
import React from 'react';
import { useEvents } from '../../../hooks/useApi';
import { useSidebar } from '../../context/SidebarContext';

export default function EventDetailsPage() {
    const { id } = useParams();
    const router = useRouter();
    const { events, loading, error } = useEvents({ page: 0, size: 100 });
    const { visible } = useSidebar();

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen p-4">
                <Loader size="l" />
                <Text>Загрузка мероприятия...</Text>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <Text color="danger">Ошибка: {error}</Text>
            </div>
        );
    }

    const event = events.find((e) => e.id === Number(id));
    if (!event) {
        return (
            <div className="flex flex-col items-center justify-center min-h-screen">
                <Text variant="header-2">Мероприятие не найдено</Text>
                <Button view="outlined" className="mt-4" onClick={() => router.push('/events')}>
                    Назад к списку
                </Button>
            </div>
        );
    }

    const formatDateTime = (dateTime: string) => {
        const date = new Date(dateTime);
        return {
            date: date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' }),
            time: date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' }),
        };
    };

    const { date, time } = formatDateTime(event.startTime);

    const instruments = [
        { instrument: 'Гитара', player: 'Иван Петров' },
        { instrument: 'Барабаны', player: null },
        { instrument: 'Клавиши', player: 'Анна Смирнова' },
        { instrument: 'Бас-гитара', player: null },
    ];

    return (
        <div
            className="
                relative min-h-screen
                px-6 sm:px-10 md:px-20 lg:px-40 xl:px-56
                py-12
                flex flex-col
                gap-10
            "
        >
            {/* Заголовок */}
            <header className="flex flex-col items-center gap-4 text-center">
                <h1 className="text-5xl font-bold">{event.title}</h1>
                <div
                    className="flex flex-wrap justify-center items-center gap-4 text-lg text-[var(--g-color-text-secondary)]">
                    <div className="flex items-center gap-2">
                        <Icon data={Clock} size={20}/>
                        <span>{date} • {time}</span>
                    </div>
                    <div className="flex items-center gap-2 text-green-600 font-medium">
                        <Icon data={Bookmark} size={18}/>
                        Активно
                    </div>
                </div>
            </header>

            {/* Описание и локация */}
            <section className="flex flex-col gap-6 text-lg leading-relaxed max-w-4xl mx-auto w-full">
                {event.venue && (
                    <div className="flex items-center gap-2">
                        <Icon data={HandPointRight} size={20}/>
                        <span>{event.venue}</span>
                    </div>
                )}

                {event.description && (
                    <p className="text-[var(--g-color-text-secondary)] whitespace-pre-wrap">
                        {event.description}
                    </p>
                )}
            </section>

            {/* Таблица участников */}
            <section className="max-w-4xl mx-auto w-full">
                <h2 className="text-2xl font-semibold mb-4 flex items-center gap-2">
                    <Icon data={Persons} size={22}/>
                    Необходимые участники
                </h2>

                <div className="overflow-x-auto rounded-xl border border-[var(--g-color-line-generic)]">
                    <table className="w-full border-collapse">
                        <thead className="bg-[var(--g-color-base-generic-hover)]">
                        <tr>
                            <th className="p-3 text-left font-medium">
                                <div className="flex items-center gap-2">
                                    <Icon data={Wrench} size={18}/>
                                    Инструмент
                                </div>
                            </th>
                            <th className="p-3 text-left font-medium">
                                <div className="flex items-center gap-2">
                                    <Icon data={Person} size={18}/>
                                    Игрок
                                </div>
                            </th>
                        </tr>
                        </thead>
                        <tbody>
                        {instruments.map((row, idx) => (
                            <tr
                                key={idx}
                                className="hover:bg-[var(--g-color-base-generic-hover)] transition-colors"
                            >
                                <td className="p-3 border-t border-[var(--g-color-line-generic)]">
                                    {row.instrument}
                                </td>
                                <td className="p-3 border-t border-[var(--g-color-line-generic)]">
                                    {row.player ? (
                                        <div className="flex items-center gap-2 text-[var(--g-color-text-success)]">
                                            <Icon data={Check} size={16}/>
                                            <span>{row.player}</span>
                                        </div>
                                    ) : (
                                        <div
                                            className="flex items-center gap-2 italic text-[var(--g-color-text-secondary)]">
                                            <Icon data={Ban} size={16}/>
                                            Нет игрока
                                        </div>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Кнопка */}
            <div className="flex justify-center gap-10 mt-12">
                <Button
                    view="outlined"
                    size="l"
                    className="min-w-[160px]"
                    onClick={() => router.back()}
                    disabled = {visible}
                >
                    Назад
                </Button>

                <Button
                    view="action"
                    size="l"
                    className="min-w-[160px] rounded-xl text-lg"
                    onClick={() => alert(`Вы записались на "${event.title}"`)}
                    disabled = {visible}
                >
                    Записаться
                </Button>
            </div>
            {visible && (
                <div className="fixed inset-0 bg-background/70 z-40" />
            )}
        </div>
    );
}
