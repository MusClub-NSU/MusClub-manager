'use client';

import { Card, Button, Icon } from '@gravity-ui/uikit';
import { Bookmark, HandPointRight } from '@gravity-ui/icons';
import React from 'react';
import { useSidebar } from '../context/SidebarContext';

interface EventItem {
    title: string;
    date: string;
    time: string;
    location: string;
    status: string;
}

const events: EventItem[] = [
    { title: 'Тренировка', date: '22.10', time: '19:00', location: 'Главный зал', status: 'Анонс' },
    { title: 'Йога', date: '23.10', time: '18:00', location: 'Зал 2', status: 'Анонс' },
    { title: 'Танцы', date: '24.10', time: '20:00', location: 'Зал 3', status: 'Анонс' },
    { title: 'Тренировка', date: '22.10', time: '19:00', location: 'Главный зал', status: 'Анонс' },
];

export default function EventsPage() {
    const { visible } = useSidebar();

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
            {events.map((ev, idx) => (
                <Card
                    key={idx}
                    view="raised"
                    type="container"
                    className="p-6 rounded-2xl shadow-md flex flex-col gap-4 event-card"
                >
                    {/* Заголовок и время */}
                    <div className="flex flex-wrap justify-between items-center gap-2">
                        <h2 className="text-2xl md:text-3xl font-semibold">{ev.title}</h2>
                        <span className="text-base md:text-lg">
                            {ev.date} • {ev.time}
                        </span>
                    </div>

                    {/* Локация */}
                    <div className="flex items-center gap-2 text-base md:text-lg">
                        <Icon data={HandPointRight} size={18} />
                        <span>{ev.location}</span>
                    </div>

                    {/* Статус */}
                    <div className="flex items-center gap-2 text-base md:text-lg">
                        <Icon data={Bookmark} size={18} />
                        <span className="font-bold text-green-600">{ev.status}</span>
                    </div>

                    {/* Кнопка */}
                    <div className="mt-2">
                        <Button
                            view="action"
                            size="m"
                            className="w-full sm:w-auto transition-opacity"
                            onClick={() => alert(`Запись на ${ev.title}`)}
                            disabled={visible}
                        >
                            Записаться
                        </Button>
                    </div>
                </Card>
            ))}
        </div>
    );
}
