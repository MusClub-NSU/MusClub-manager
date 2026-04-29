'use client';

import { syncAllMainDataCaches, useUsers, useHybridSearch } from '../../hooks/useApi';
import { Button, Card, Text, Loader, Icon, Link} from '@gravity-ui/uikit';
import { Plus, Pencil, TrashBin, Xmark } from '@gravity-ui/icons';
import { useEffect, useState } from 'react';
import { User } from '../../types/api';
import { useSidebar } from '../context/SidebarContext';
import { useCurrentUserRole } from '@/hooks/useCurrentUserRole';

export default function ParticipantsPage() {
    const { visible: sidebarVisible } = useSidebar();
    const { canManageUsers } = useCurrentUserRole();
    const { users, loading, error, createUser, updateUser, deleteUser, refetch } = useUsers({ page: 0, size: 20 });
    const { results: searchResults, loading: searchLoading, error: searchError, search } = useHybridSearch();
    const [searchQuery, setSearchQuery] = useState('');
    const [isCreating, setIsCreating] = useState(false);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [createFormData, setCreateFormData] = useState({
        username: '',
        email: '',
        role: 'MEMBER',
        password: '',
    });
    const [editingUser, setEditingUser] = useState<User | null>(null);
    const [editFormData, setEditFormData] = useState({
        username: '',
        email: '',
        role: 'MEMBER',
        password: '',
    });
    const handleSyncAll = async () => {
        try {
            await syncAllMainDataCaches();
        } catch {
            await refetch();
        }
    };

    useEffect(() => {
        const timeout = setTimeout(() => {
            if (searchQuery.trim()) {
                search(searchQuery, ['USER'], { page: 0, size: 20 });
            }
        }, 250);
        return () => clearTimeout(timeout);
    }, [searchQuery, search]);

    const handleCreateUser = () => {
        if (!canManageUsers) return;
        setCreateFormData({
            username: '',
            email: '',
            role: 'MEMBER',
            password: '',
        });
        setIsCreateModalOpen(true);
    };

    const handleCloseCreate = () => {
        setIsCreateModalOpen(false);
    };

    const handleSaveCreate = async () => {
        if (!canManageUsers) return;
        if (!createFormData.username.trim() || !createFormData.email.trim() || !createFormData.password.trim()) {
            alert('Заполните имя, email и пароль');
            return;
        }

        setIsCreating(true);
        try {
            await createUser({
                username: createFormData.username.trim(),
                email: createFormData.email.trim(),
                role: createFormData.role,
                password: createFormData.password,
            });
            await refetch();
            handleCloseCreate();
        } catch (err) {
            const errorMessage = err instanceof Error ? err.message : 'Неизвестная ошибка';
            console.error('Ошибка создания пользователя:', errorMessage);
            alert(`Ошибка создания пользователя: ${errorMessage}`);
        } finally {
            setIsCreating(false);
        }
    };

    const handleDeleteUser = async (id: number) => {
        if (!canManageUsers) return;
        if (confirm('Вы уверены, что хотите удалить этого участника?')) {
            try {
                await deleteUser(id);
            } catch (err) {
                console.error('Ошибка удаления пользователя:', err);
            }
        }
    };

    const handleEditUser = (user: User) => {
        if (!canManageUsers) return;
        setEditingUser(user);
        setEditFormData({
            username: user.username,
            email: user.email,
            role: user.role,
            password: '',
        });
    };

    const handleCloseEdit = () => {
        setEditingUser(null);
    };

    const handleSaveEdit = async () => {
        if (!canManageUsers) return;
        if (!editingUser) return;
        if (editFormData.password.trim() && editFormData.password.trim().length < 8) {
            alert('Новый пароль должен содержать минимум 8 символов');
            return;
        }

        try {
            const password = editFormData.password.trim();
            await updateUser(editingUser.id, {
                username: editFormData.username,
                email: editFormData.email,
                role: editFormData.role,
                ...(password ? { password } : {}),
            });
            handleCloseEdit();
        } catch (err) {
            console.error('Ошибка обновления пользователя:', err);
        }
    };


    if (loading && users.length === 0) {
        return (
            <main className="flex items-center justify-center min-h-screen p-4">
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text>Загрузка участников...</Text>
                </div>
            </main>
        );
    }

    if (error && users.length === 0) {
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
        <>
        <main className="min-h-screen overflow-y-auto p-5 sm:p-6 md:p-8 pt-8 sm:pt-10 md:pt-12">
            <div className="w-full">
                {/* Заголовок и кнопка создания */}
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-8">
                    <div>
                        <h1 className="text-5xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent mb-1">
                            Участники
                        </h1>
                    </div>
                    <div className="flex items-center gap-2">
                        <Button view="outlined" size="l" onClick={() => void handleSyncAll()} loading={loading} disabled={sidebarVisible} aria-label="Обновить данные" title="Обновить данные">
                            ↻
                        </Button>
                        {canManageUsers && (
                            <Button
                                view="action"
                                onClick={handleCreateUser}
                                disabled={isCreating || sidebarVisible}
                                size="l"
                                className="px-6 py-3 bg-gradient-to-r from-blue-600 to-blue-500 hover:from-blue-700 hover:to-blue-600 text-white font-semibold shadow-lg hover:shadow-xl transition-all duration-200"
                            >
                                <span className="flex items-center justify-center gap-2">
                                    <Icon data={Plus} size={18} />
                                    <span>Добавить участника</span>
                                </span>
                            </Button>
                        )}
                    </div>
                </div>

                {/* Поиск с красивым дизайном */}
                <div className="mb-4">
                    <div className="relative group">
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Поиск по имени или email..."
                            className="w-full px-4 pr-12 py-3 rounded-xl border-2 bg-opacity-50 placeholder-opacity-50 focus:outline-none focus:ring-2 focus:ring-opacity-20 transition-all duration-200 shadow-sm text-base"
                            style={{
                                borderColor: 'var(--color-line-generic)',
                                backgroundColor: 'var(--color-background-secondary)',
                                color: 'var(--color-text-primary)'
                            }}
                        />
                        {searchQuery.trim() && (
                            <button
                                onClick={() => {
                                    setSearchQuery('');
                                }}
                                className="absolute inset-y-0 right-0 pr-4 flex items-center opacity-40 hover:opacity-70 transition-opacity duration-150"
                            >
                                <Icon data={Xmark} size={18} />
                            </button>
                        )}
                    </div>
                    {searchQuery.trim() && searchLoading && (
                        <div className="mt-3 text-sm flex items-center gap-2 opacity-70">
                            <Loader size="s" />
                            <span>Поиск результатов...</span>
                        </div>
                    )}
                    {searchQuery.trim() && searchError && (
                        <Text color="danger" className="text-xs mb-2">Ошибка: {searchError}</Text>
                    )}

                    {searchQuery.trim() && !searchLoading && searchResults.filter((result) => result.entityType === 'USER' && (result.score || 0) > 0.05).length === 0 ? (
                        <Card className="p-8 text-center">
                            <Text variant="subheader-1" color="secondary">
                                По запросу ничего не найдено
                            </Text>
                        </Card>
                    ) : !searchQuery.trim() && users.length === 0 && !loading ? (
                        <Card className="p-8 text-center">
                            <Text variant="subheader-1" color="secondary">
                                Участники не найдены
                            </Text>
                            <Text color="secondary" className="mt-2">
                                Добавьте первого участника, нажав кнопку выше
                            </Text>
                        </Card>
                    ) : (
                        <div className="space-y-5">
                        {/* Декоративная карточка с информацией */}
                        {users.length > 0 && (
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mt-2">
                                <Card className="p-4">
                                    <div className="text-center">
                                        <div className="text-2xl font-bold text-blue-600 mb-1">{users.length}</div>
                                        <div className="text-xs opacity-70">Всего пользователей</div>
                                    </div>
                                </Card>
                                <Card className="p-4">
                                    <div className="text-center">
                                        <div className="text-2xl font-bold text-green-600 mb-1">{users.filter(u => u.role === 'ORGANIZER').length}</div>
                                        <div className="text-xs opacity-70">Организаторов</div>
                                    </div>
                                </Card>
                                <Card className="p-4">
                                    <div className="text-center">
                                        <div className="text-2xl font-bold text-purple-600 mb-1">{users.filter(u => u.role !== 'ORGANIZER').length}</div>
                                        <div className="text-xs opacity-70">Участников</div>
                                    </div>
                                </Card>
                            </div>
                        )}

                        {/* Таблица с красивым стилем */}
                        <div className="overflow-hidden rounded-xl border shadow-sm hover:shadow-md transition-shadow" style={{ borderColor: 'var(--color-line-generic)' }}>
                            <div className="overflow-x-auto">
                                <table className="w-full">
                                    <thead>
                                        <tr className="border-b" style={{ backgroundColor: 'var(--color-background-secondary)', borderColor: 'var(--color-line-generic)' }}>
                                            <th className="px-6 py-4 text-left">
                                                <span className="text-sm font-semibold uppercase tracking-wider opacity-70">Имя пользователя</span>
                                            </th>
                                            <th className="px-6 py-4 text-left">
                                                <span className="text-sm font-semibold uppercase tracking-wider opacity-70">Email</span>
                                            </th>
                                            <th className="px-6 py-4 text-left">
                                                <span className="text-sm font-semibold uppercase tracking-wider opacity-70">Роль</span>
                                            </th>
                                            <th className="px-6 py-4 text-left">
                                                <span className="text-sm font-semibold uppercase tracking-wider opacity-70">Дата регистрации</span>
                                            </th>
                                            <th className="px-6 py-4 text-right">
                                                <span className="text-sm font-semibold uppercase tracking-wider opacity-70">Действия</span>
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y" style={{ borderColor: 'var(--color-base-misc)' }}>
                                        {searchQuery.trim() ? (
                                            searchResults
                                                .filter((result) => result.entityType === 'USER' && (result.score || 0) > 0.05)
                                                .map((result) => {
                                                    const user = users.find((u) => u.id === result.entityId);
                                                    return (
                                                        <tr
                                                            key={result.entityId}
                                                            className="hover:opacity-80 transition-opacity duration-200 group"
                                                        >
                                                        <td className="px-6 py-4">
                                                            <Link
                                                                view="normal"
                                                                href={`/participants/${result.entityId}`}
                                                                className="font-medium hover:text-blue-500 transition-colors"
                                                            >
                                                                <span className="group-hover:underline">
                                                                    {user?.username ?? result.title}
                                                                </span>
                                                            </Link>
                                                        </td>
                                                        <td className="px-6 py-4 font-mono text-sm opacity-70">
                                                            {user?.email ?? result.snippet ?? 'Не указано'}
                                                        </td>
                                                        <td className="px-6 py-4">
                                                            {user?.role === 'ORGANIZER' ? (
                                                                <span className="inline-flex items-center px-3 py-1.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-700">
                                                                    Организатор
                                                                </span>
                                                            ) : user?.role === 'MEMBER' ? (
                                                                <span className="inline-flex items-center px-3 py-1.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-700">
                                                                    Участник
                                                                </span>
                                                            ) : (
                                                                <span className="text-sm opacity-70">-</span>
                                                            )}
                                                        </td>
                                                        <td className="px-6 py-4 text-sm opacity-70">
                                                            {user?.createdAt
                                                                ? new Date(user.createdAt).toLocaleDateString('ru-RU', {
                                                                    year: 'numeric',
                                                                    month: 'short',
                                                                    day: 'numeric',
                                                                })
                                                                : '-'}
                                                        </td>
                                                        <td className="px-6 py-4 text-right">
                                                            <div className="flex justify-end gap-2 opacity-100">
                                                                {canManageUsers ? (
                                                                    <>
                                                                        <Button
                                                                            view="flat"
                                                                            size="s"
                                                                            onClick={() => user && handleEditUser(user)}
                                                                            disabled={sidebarVisible || !user}
                                                                            title={!user ? 'Данные пользователя не найдены в текущей выборке' : undefined}
                                                                        >
                                                                            <Icon data={Pencil} size={16} />
                                                                        </Button>
                                                                        <Button
                                                                            view="flat"
                                                                            size="s"
                                                                            onClick={() => handleDeleteUser(result.entityId)}
                                                                            disabled={sidebarVisible}
                                                                        >
                                                                            <Icon data={TrashBin} size={16} />
                                                                        </Button>
                                                                    </>
                                                                ) : null}
                                                            </div>
                                                        </td>
                                                    </tr>
                                                    );
                                                })
                                        ) : (
                                            users.map((user) => (
                                                <tr 
                                                    key={user.id} 
                                                    className="hover:opacity-80 transition-opacity duration-200 group"
                                                >
                                                    <td className="px-6 py-4">
                                                        <Link
                                                            view="normal"
                                                            href={`/participants/${user.id}`}
                                                            className="font-medium hover:text-blue-500 transition-colors"
                                                        >
                                                            <span className="group-hover:underline">{user.username}</span>
                                                        </Link>
                                                    </td>
                                                    <td className="px-6 py-4 font-mono text-sm opacity-70">{user.email}</td>
                                                    <td className="px-6 py-4">
                                                        {user.role === 'ORGANIZER' ? (
                                                            <span className="inline-flex items-center px-3 py-1.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-700">
                                                                Организатор
                                                            </span>
                                                        ) : (
                                                            <span className="inline-flex items-center px-3 py-1.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-700">
                                                                Участник
                                                            </span>
                                                        )}
                                                    </td>
                                                    <td className="px-6 py-4 text-sm opacity-70">
                                                        {user.createdAt ? new Date(user.createdAt).toLocaleDateString('ru-RU', {
                                                            year: 'numeric',
                                                            month: 'short',
                                                            day: 'numeric'
                                                        }) : '-'}
                                                    </td>
                                                    <td className="px-6 py-4 text-right">
                                                        <div className="flex justify-end gap-2 opacity-100">
                                                            {canManageUsers ? (
                                                                <>
                                                                    <Button
                                                                        view="flat"
                                                                        size="s"
                                                                        onClick={() => handleEditUser(user)}
                                                                        disabled={sidebarVisible}
                                                                    >
                                                                        <Icon data={Pencil} size={16} />
                                                                    </Button>
                                                                    <Button
                                                                        view="flat"
                                                                        size="s"
                                                                        onClick={() => handleDeleteUser(user.id)}
                                                                        disabled={sidebarVisible}
                                                                    >
                                                                        <Icon data={TrashBin} size={16} />
                                                                    </Button>
                                                                </>
                                                            ) : null}
                                                        </div>
                                                    </td>
                                                </tr>
                                            ))
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {isCreateModalOpen && canManageUsers && (
                <div
                    className="fixed inset-0 flex items-center justify-center bg-background/70 backdrop-blur-sm z-50 px-6"
                    onClick={handleCloseCreate}
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
                                onClick={handleCloseCreate}
                            >
                                <Icon data={Xmark} size={22} />
                            </Button>

                            <h2 className="text-3xl font-bold text-center">Добавить участника</h2>

                            <div className="flex flex-col gap-5 mt-4">
                                <div className="group">
                                    <label className="block text-sm font-medium mb-2">
                                        Имя пользователя <span className="text-red-500">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={createFormData.username}
                                        onChange={(e) => setCreateFormData({ ...createFormData, username: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div className="group">
                                    <label className="block text-sm font-medium mb-2">
                                        Email <span className="text-red-500">*</span>
                                    </label>
                                    <input
                                        type="email"
                                        value={createFormData.email}
                                        onChange={(e) => setCreateFormData({ ...createFormData, email: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div className="group">
                                    <label className="block text-sm font-medium mb-2">
                                        Пароль <span className="text-red-500">*</span>
                                    </label>
                                    <input
                                        type="password"
                                        value={createFormData.password}
                                        onChange={(e) => setCreateFormData({ ...createFormData, password: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div className="group">
                                    <label className="block text-sm font-medium mb-2">
                                        Роль <span className="text-red-500">*</span>
                                    </label>
                                    <select
                                        value={createFormData.role}
                                        onChange={(e) => setCreateFormData({ ...createFormData, role: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    >
                                        <option value="MEMBER">Участник</option>
                                        <option value="ORGANIZER">Организатор</option>
                                    </select>
                                </div>

                                <div className="group">
                                    <label className="block text-sm font-medium mb-2">
                                        Новый пароль
                                    </label>
                                    <input
                                        type="password"
                                        value={editFormData.password}
                                        onChange={(e) => setEditFormData({ ...editFormData, password: e.target.value })}
                                        placeholder="Оставьте пустым, чтобы не менять"
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                    <Text variant="body-1" color="secondary" className="mt-1">
                                        Минимум 8 символов, если указываете пароль.
                                    </Text>
                                </div>

                                <div className="flex justify-end gap-3 mt-4">
                                    <Button
                                        view="outlined"
                                        onClick={handleCloseCreate}
                                    >
                                        Отмена
                                    </Button>
                                    <Button
                                        view="action"
                                        onClick={handleSaveCreate}
                                        disabled={isCreating}
                                    >
                                        Сохранить
                                    </Button>
                                </div>
                            </div>
                        </Card>
                    </div>
                </div>
            )}

            {/* Модальное окно редактирования */}
            {editingUser && canManageUsers && (
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

                            <div className="flex flex-col gap-5 mt-4">
                                <div className="group">
                                    <label className="block text-sm font-medium mb-2">
                                        Имя пользователя <span className="text-red-500">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={editFormData.username}
                                        onChange={(e) => setEditFormData({ ...editFormData, username: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div className="group">
                                    <label className="block text-sm font-medium mb-2">
                                        Email <span className="text-red-500">*</span>
                                    </label>
                                    <input
                                        type="email"
                                        value={editFormData.email}
                                        onChange={(e) => setEditFormData({ ...editFormData, email: e.target.value })}
                                        className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        required
                                    />
                                </div>

                                <div className="group">
                                    <label className="block text-sm font-medium mb-2">
                                        Роль <span className="text-red-500">*</span>
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
                                        Сохранить изменения
                                    </Button>
                                </div>
                            </div>
                        </Card>
                    </div>
                </div>
            )}
            </div>
        </main>
        </>
    );
}