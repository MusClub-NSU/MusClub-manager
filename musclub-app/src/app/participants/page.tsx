'use client';

import { useUsers } from '../../hooks/useApi';
import { Button, Card, Text, Loader } from '@gravity-ui/uikit';
import { Plus, Pencil, TrashBin } from '@gravity-ui/icons';
import { useState } from 'react';

export default function ParticipantsPage() {
    const { users, loading, error, createUser, updateUser, deleteUser } = useUsers({ page: 0, size: 20 });
    const [isCreating, setIsCreating] = useState(false);
    const [editingId, setEditingId] = useState<number | null>(null);

    const handleCreateUser = async () => {
        try {
            await createUser({
                username: 'Новый участник',
                email: 'new@example.com',
                role: 'MEMBER'
            });
        } catch (err) {
            console.error('Ошибка создания пользователя:', err);
        }
    };

    const handleDeleteUser = async (id: number) => {
        if (confirm('Вы уверены, что хотите удалить этого участника?')) {
            try {
                await deleteUser(id);
            } catch (err) {
                console.error('Ошибка удаления пользователя:', err);
            }
        }
    };

    const getRoleDisplayName = (role: string) => {
        switch (role) {
            case 'ORGANIZER':
                return 'Организатор';
            case 'MEMBER':
                return 'Участник';
            default:
                return role;
        }
    };

    if (loading) {
        return (
            <main className="flex items-center justify-center min-h-screen p-4">
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text>Загрузка участников...</Text>
                </div>
            </main>
        );
    }

    if (error) {
        return (
            <main className="flex items-center justify-center min-h-screen p-4">
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
            </main>
        );
    }

    return (
        <main className="flex items-center justify-center min-h-screen p-4">
            <div className="w-full max-w-4xl">
                <div className="flex justify-between items-center mb-6">
                    <h1 className="text-3xl font-bold">Участники</h1>
                    <Button
                        view="action"
                        onClick={handleCreateUser}
                        disabled={isCreating}
                    >
                        <Plus size={16} />
                        Добавить участника
                    </Button>
                </div>

                {users.length === 0 ? (
                    <Card className="p-8 text-center">
                        <Text variant="subheader-1" color="secondary">
                            Участники не найдены
                        </Text>
                        <Text color="secondary" className="mt-2">
                            Добавьте первого участника, нажав кнопку выше
                        </Text>
                    </Card>
                ) : (
                    <div className="overflow-x-auto rounded-lg border border-[--foreground]/20">
                        <table className="min-w-full text-left">
                            <thead className="bg-[--foreground]/5">
                                <tr>
                                    <th className="px-4 py-3 font-semibold">Имя пользователя</th>
                                    <th className="px-4 py-3 font-semibold">Email</th>
                                    <th className="px-4 py-3 font-semibold">Роль</th>
                                    <th className="px-4 py-3 font-semibold">Дата регистрации</th>
                                    <th className="px-4 py-3 font-semibold">Действия</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map((user) => (
                                    <tr key={user.id} className="border-t border-[--foreground]/10">
                                        <td className="px-4 py-3 font-medium">{user.username}</td>
                                        <td className="px-4 py-3 text-[--foreground]/70">{user.email}</td>
                                        <td className="px-4 py-3">
                                            <span className={`px-2 py-1 rounded text-sm ${
                                                user.role === 'ORGANIZER' 
                                                    ? 'bg-blue-100 text-blue-800' 
                                                    : 'bg-gray-100 text-gray-800'
                                            }`}>
                                                {getRoleDisplayName(user.role)}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3 text-[--foreground]/70">
                                            {new Date(user.createdAt).toLocaleDateString('ru-RU')}
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="flex gap-2">
                                                <Button
                                                    view="flat"
                                                    size="s"
                                                    onClick={() => setEditingId(user.id)}
                                                >
                                                    <Pencil size={14} />
                                                </Button>
                                                <Button
                                                    view="flat"
                                                    size="s"
                                                    onClick={() => handleDeleteUser(user.id)}
                                                >
                                                    <TrashBin size={14} />
                                                </Button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </main>
    );
}