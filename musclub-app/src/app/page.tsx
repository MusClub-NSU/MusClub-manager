'use client';

import { syncAllMainDataCaches, useEvents } from '../hooks/useApi';
import { Card, Text, Loader, Button } from '@gravity-ui/uikit';
import Link from 'next/link';
import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';

export default function Home() {
    const { data: session } = useSession();
    const router = useRouter();
    const { events, loading: eventsLoading, error: eventsError, refetch } = useEvents({ page: 0, size: 100 });

    const handleSyncAll = async () => {
        try {
            await syncAllMainDataCaches();
        } catch {
            await refetch();
        }
    };

    if (eventsLoading && events.length === 0) {
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
        <main className="min-h-screen overflow-y-auto p-5 sm:p-6 md:p-8 pt-8 sm:pt-10 md:pt-12">
            <div className="w-full max-w-5xl mx-auto">
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-8">
                    <div>
                        <h1 className="text-5xl font-bold leading-[1.15] pb-1 bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent mb-1">
                            MusClub Manager
                        </h1>
                    </div>
                    {!session && (
                        <Button
                            view="action"
                            size="l"
                            className="px-6 py-3 bg-gradient-to-r from-blue-600 to-blue-500 hover:from-blue-700 hover:to-blue-600 text-white font-semibold shadow-lg hover:shadow-xl transition-all duration-200"
                            onClick={() => router.push('/login?callbackUrl=/')}
                        >
                            Авторизоваться
                        </Button>
                    )}
                </div>

                <Card className="p-6 rounded-xl border shadow-sm" style={{ borderColor: 'var(--color-line-generic)' }}>
                    <div className="flex items-center justify-between gap-4 mb-4">
                        <h2 className="text-2xl font-semibold">Ближайшие мероприятия</h2>
                        <div className="flex items-center gap-3">
                            <Text color="secondary" className="text-sm whitespace-nowrap">
                                {upcomingEvents.length} шт.
                            </Text>
                            <Button view="outlined" size="s" onClick={() => void handleSyncAll()} loading={eventsLoading} aria-label="Обновить данные" title="Обновить данные">
                                ↻
                            </Button>
                        </div>
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
                                            href={
                                                session
                                                    ? `/events/${event.id}`
                                                    : `/login?callbackUrl=${encodeURIComponent(`/events/${event.id}`)}`
                                            }
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
