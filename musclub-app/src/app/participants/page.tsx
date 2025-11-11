'use client';

import { useUsers } from '../../hooks/useApi';
import { Button, Card, Text, Loader, Icon } from '@gravity-ui/uikit';
import { Plus, Pencil, TrashBin, Xmark } from '@gravity-ui/icons';
import { useState } from 'react';
import { User } from '../../types/api';

export default function ParticipantsPage() {
    const { users, loading, error, createUser, updateUser, deleteUser } = useUsers({ page: 0, size: 20 });
    const [isCreating, setIsCreating] = useState(false);
    const [editingUser, setEditingUser] = useState<User | null>(null);
    const [editFormData, setEditFormData] = useState({
        username: '',
        email: '',
        role: 'MEMBER'
    });

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

    const handleEditUser = (user: User) => {
        setEditingUser(user);
        setEditFormData({
            username: user.username,
            email: user.email,
            role: user.role
        });
    };

    const handleCloseEdit = () => {
        setEditingUser(null);
    };

    const handleSaveEdit = async () => {
        if (!editingUser) return;

        try {
            await updateUser(editingUser.id, {
                username: editFormData.username,
                email: editFormData.email,
                role: editFormData.role
            });
            handleCloseEdit();
        } catch (err) {
            console.error('Ошибка обновления пользователя:', err);
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
                        <span className="flex items-center justify-center gap-2">
                        <Plus size={16} />
                            <span>Добавить участника</span>
                        </span>
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
                                                    onClick={() => handleEditUser(user)}
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

            {editingUser && (
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

                            <h2 className="text-3xl font-bold text-center">Редактировать участника</h2>

                            <div className="flex flex-col gap-4">
                                <div>
                                    <label className="block text-sm font-medium mb-2">
                                        Имя пользователя *
                                    </label>
                                    <input
                                        type="text"
                                        value={editFormData.username}
                                        onChange={(e) => setEditFormData({ ...editFormData, username: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">
                                        Email *
                                    </label>
                                    <input
                                        type="email"
                                        value={editFormData.email}
                                        onChange={(e) => setEditFormData({ ...editFormData, email: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">
                                        Роль *
                                    </label>
                                    <select
                                        value={editFormData.role}
                                        onChange={(e) => setEditFormData({ ...editFormData, role: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    >
                                        <option value="MEMBER">Участник</option>
                                        <option value="ORGANIZER">Организатор</option>
                                    </select>
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
        </main>
    );
}