'use client';

import { useUsers, useEvents } from '../hooks/useApi';
import { Card, Text, Loader } from '@gravity-ui/uikit';
import Link from 'next/link';

export default function Home() {
    const { users, loading: usersLoading, error: usersError } = useUsers({ page: 0, size: 5 });
    const { events, loading: eventsLoading, error: eventsError } = useEvents({ page: 0, size: 5 });

    if (usersLoading || eventsLoading) {
        return (
            <main className="flex items-center justify-center min-h-screen p-4">
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text>Загрузка данных...</Text>
                </div>
            </main>
        );
    }

    return (
        <main className="flex items-center justify-center min-h-screen p-4">
            <div className="w-full max-w-6xl">
                <h1 className="text-4xl font-bold mb-8 text-center">Добро пожаловать в MusClub Manager</h1>
                
                <div className="grid md:grid-cols-2 gap-8">
                    {/* Статистика пользователей */}
                    <Card className="p-6">
                        <h2 className="text-2xl font-semibold mb-4">Участники</h2>
                        {usersError ? (
                            <Text color="danger">Ошибка загрузки: {usersError}</Text>
                        ) : (
                            <>
                                <Text variant="subheader-1" className="mb-3">
                                    Всего участников: {users.length}
                                </Text>
                                <div className="space-y-1">
                                    {users.slice(0, 3).map((user) => (
                                        <div key={user.id} className="flex justify-between items-center">
                                            <Text>{user.username}</Text>
                                            <Text color="secondary" className="text-sm">
                                                {user.role === 'ORGANIZER' ? 'Организатор' : 'Участник'}
                                            </Text>
                                        </div>
                                    ))}
                                </div>
                                <Link href="/participants" className="text-blue-600 hover:underline mt-4 block">
                                    Посмотреть всех участников →
                                </Link>
                            </>
                        )}
                    </Card>

                    {/* Статистика событий */}
                    <Card className="p-6">
                        <h2 className="text-2xl font-semibold mb-4">Мероприятия</h2>
                        {eventsError ? (
                            <Text color="danger">Ошибка загрузки: {eventsError}</Text>
                        ) : (
                            <>
                                <Text variant="subheader-1" className="mb-3">
                                    Всего мероприятий: {events.length}
                                </Text>
                                <div className="space-y-1">
                                    {events.slice(0, 3).map((event) => (
                                        <div key={event.id} className="flex justify-between items-center">
                                            <Text>{event.title}</Text>
                                            <Text color="secondary" className="text-sm">
                                                {new Date(event.startTime).toLocaleDateString('ru-RU')}
                                            </Text>
                                        </div>
                                    ))}
                                </div>
                                <Link href="/events" className="text-blue-600 hover:underline mt-4 block">
                                    Посмотреть все мероприятия →
                                </Link>
                            </>
                        )}
                    </Card>
                </div>

                {/* Быстрые действия */}
                <Card className="p-6 mt-8">
                    <h2 className="text-2xl font-semibold mb-4">Быстрые действия</h2>
                    <div className="grid sm:grid-cols-2 gap-4">
                        <Link 
                            href="/participants" 
                            className="p-4 border rounded-lg hover:bg-gray-50 transition-colors flex flex-col gap-2"
                        >
                            <Text variant="subheader-1" className="block">Управление участниками</Text>
                            <Text color="secondary" className="text-sm block">
                                Добавить, редактировать или удалить участников
                            </Text>
                        </Link>
                        <Link 
                            href="/events" 
                            className="p-4 border rounded-lg hover:bg-gray-50 transition-colors flex flex-col gap-2"
                        >
                            <Text variant="subheader-1" className="block">Управление мероприятиями</Text>
                            <Text color="secondary" className="text-sm block">
                                Создать новое мероприятие или управлять существующими
                            </Text>
                        </Link>
                    </div>
                </Card>
            </div>
        </main>
    );
}
