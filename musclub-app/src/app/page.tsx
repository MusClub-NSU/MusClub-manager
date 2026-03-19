'use client';

import { useEvents } from '../hooks/useApi';
import { Card, Text, Loader, Button } from '@gravity-ui/uikit';
import Link from 'next/link';
import { signIn, useSession } from 'next-auth/react';

export default function Home() {
    const { data: session } = useSession();
    const { events, loading: eventsLoading, error: eventsError } = useEvents({ page: 0, size: 100 });

    if (eventsLoading) {
        return (
            <main className="flex items-center justify-center min-h-screen p-4">
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text>Загрузка данных...</Text>
                </div>
            </main>
        );
    }

    const now = new Date();
    const upcomingEvents = [...events]
        .filter((event) => {
            const start = new Date(event.startTime);
            return !Number.isNaN(start.getTime()) && start.getTime() >= now.getTime();
        })
        .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());

    return (
        <main className="min-h-screen">
            <header className="sticky top-0 z-10 py-4 pr-4 pl-14 sm:px-4 bg-background/80 backdrop-blur">
                <div className="mx-auto w-full max-w-5xl flex items-center justify-between gap-4">
                    <h1 className="text-xl sm:text-2xl md:text-3xl font-bold">
                        Добро пожаловать в MusClub Manager
                    </h1>
                    {!session && (
                        <Button
                            view="action"
                            size="m"
                            className="shrink-0"
                            onClick={() => signIn('keycloak')}
                        >
                            Авторизоваться
                        </Button>
                    )}
                </div>
            </header>

            <div className="mx-auto w-full max-w-5xl px-4 py-6">
                <Card className="p-6">
                    <div className="flex items-center justify-between gap-4 mb-4">
                        <h2 className="text-2xl font-semibold">Ближайшие мероприятия</h2>
                        <Text color="secondary" className="text-sm whitespace-nowrap">
                            {upcomingEvents.length} шт.
                        </Text>
                    </div>

                    {eventsError ? (
                        <Text color="danger">Ошибка загрузки: {eventsError}</Text>
                    ) : upcomingEvents.length === 0 ? (
                        <div className="flex flex-col gap-2">
                            <Text color="secondary">Нет ближайших мероприятий.</Text>
                            <Text color="secondary" className="text-sm">
                                Создайте новое мероприятие или проверьте даты существующих.
                            </Text>
                        </div>
                    ) : (
                        <div className="divide-y">
                            {upcomingEvents.map((event) => (
                                <div key={event.id} className="py-4 flex items-start justify-between gap-4">
                                    <div className="min-w-0">
                                        <Link
                                            href={`/events/${event.id}`}
                                            className="font-semibold hover:underline block truncate"
                                            title={event.title}
                                        >
                                            {event.title}
                                        </Link>
                                        {event.venue ? (
                                            <Text color="secondary" className="text-sm truncate" title={event.venue}>
                                                {event.venue}
                                            </Text>
                                        ) : null}
                                    </div>
                                    <Text color="secondary" className="text-sm whitespace-nowrap">
                                        {new Date(event.startTime).toLocaleString('ru-RU', {
                                            day: '2-digit',
                                            month: '2-digit',
                                            year: 'numeric',
                                            hour: '2-digit',
                                            minute: '2-digit',
                                        })}
                                    </Text>
                                </div>
                            ))}
                        </div>
                    )}
                </Card>
            </div>
        </main>
    );
}
