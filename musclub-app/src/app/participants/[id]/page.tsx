'use client';

import { useEffect, useId, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { Card, Button, Text, Icon, Loader, Select } from '@gravity-ui/uikit';
import { Person, LogoTelegram, Calendar, Pencil, TrashBin } from '@gravity-ui/icons';
import { useUsers } from '@/hooks/useApi';
import { useSidebar } from '../../context/SidebarContext';
import { apiClient } from '@/lib/api';
import { useCurrentUserRole } from '@/hooks/useCurrentUserRole';
import { useSession } from 'next-auth/react';
import type { User } from '@/types/api';

const USER_DETAIL_CACHE_TTL_MS = 5 * 60 * 1000;

type UserDetailCachePayload = {
    timestamp: number;
    user: User;
};

function formatRole(role?: string) {
    if (role === 'ORGANIZER') return 'Организатор';
    if (role === 'MEMBER') return 'Участник';
    return 'Не указана';
}

export default function UserDetailsPage() {
    const params = useParams();
    const router = useRouter();
    const { visible } = useSidebar();
    const { canManageUsers, canManageEvents } = useCurrentUserRole();
    const { data: session } = useSession();
    const avatarInputId = useId();

    const userId = Number(params.id);
    const { users, loading, error, updateUser, refetch } = useUsers({ page: 0, size: 999 });
    const getUserCacheKey = (id: number) => `user-details:${id}`;
    const readUserCache = (id: number): User | null => {
        if (typeof window === 'undefined') return null;
        try {
            const raw = window.localStorage.getItem(getUserCacheKey(id));
            if (!raw) return null;
            const parsed = JSON.parse(raw) as UserDetailCachePayload;
            if (!parsed?.timestamp || !parsed?.user) return null;
            if (Date.now() - parsed.timestamp > USER_DETAIL_CACHE_TTL_MS) return null;
            return parsed.user;
        } catch {
            return null;
        }
    };
    const writeUserCache = (userToCache: User) => {
        if (typeof window === 'undefined') return;
        try {
            const payload: UserDetailCachePayload = {
                timestamp: Date.now(),
                user: userToCache,
            };
            window.localStorage.setItem(getUserCacheKey(userToCache.id), JSON.stringify(payload));
        } catch {
            // no-op
        }
    };

    const [cachedUser, setCachedUser] = useState<User | null>(() =>
        Number.isFinite(userId) ? readUserCache(userId) : null,
    );
    const user = users.find((u) => u.id === userId) ?? cachedUser;

    const isSelf = !!session?.user?.email && session.user.email === user?.email;
    const canEditProfile = isSelf || canManageUsers;

    const [isEditing, setIsEditing] = useState(false);
    const [copiedEmail, setCopiedEmail] = useState(false);
    const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
    const [saveError, setSaveError] = useState<string | null>(null);
    const [pushSending, setPushSending] = useState(false);
    const [pushResult, setPushResult] = useState<string | null>(null);
    const [pushError, setPushError] = useState<string | null>(null);
    const [pushMessage, setPushMessage] = useState('');
    const [passwordValue, setPasswordValue] = useState('');
    const [passwordSaving, setPasswordSaving] = useState(false);
    const [passwordSaved, setPasswordSaved] = useState(false);
    const [editData, setEditData] = useState({ username: '', email: '', role: '' });

    useEffect(() => {
        if (!user) return;
        writeUserCache(user);
        setCachedUser(user);
        setEditData({
            username: user.username,
            email: user.email,
            role: user.role,
        });
    }, [user]);

    useEffect(() => {
        if (!isEditing) {
            setPasswordValue('');
            setPasswordSaved(false);
        }
    }, [isEditing]);

    if (loading && !user) {
        return (
            <main className="flex justify-center items-center min-h-screen">
                <Loader size="l" />
            </main>
        );
    }

    if (!Number.isFinite(userId) || !user) {
        return (
            <main className="flex justify-center items-center min-h-screen p-4">
                <Card className="p-6 max-w-md text-center">
                    <Text variant="subheader-2">Пользователь не найден</Text>
                    <Button view="outlined" className="mt-4" onClick={() => router.push('/participants')}>
                        К списку участников
                    </Button>
                </Card>
            </main>
        );
    }

    if (error) {
        return (
            <main className="flex justify-center items-center min-h-screen">
                <Card className="p-6">
                    <Text color="danger">{error}</Text>
                </Card>
            </main>
        );
    }

    const handleSave = async () => {
        if (!canEditProfile) return;
        setSaveError(null);

        try {
            if (canManageUsers) {
                const updatedUser = await updateUser(user.id, {
                    username: editData.username,
                    email: editData.email,
                    role: editData.role,
                });
                writeUserCache(updatedUser);
                setCachedUser(updatedUser);
            } else {
                const updatedUser = await updateUser(user.id, { username: editData.username });
                writeUserCache(updatedUser);
                setCachedUser(updatedUser);
            }

            setIsEditing(false);
            await refetch();
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Не удалось сохранить изменения';
            setSaveError(message);
        }
    };

    const canChangePassword = isSelf || canManageUsers;
    const handleChangePassword = async () => {
        if (!canChangePassword) return;
        const nextPassword = passwordValue.trim();
        if (!nextPassword) {
            setSaveError('Введите новый пароль');
            return;
        }
        if (nextPassword.length < 8) {
            setSaveError('Новый пароль должен содержать минимум 8 символов');
            return;
        }

        setSaveError(null);
        setPasswordSaving(true);
        setPasswordSaved(false);
        try {
            await apiClient.updateUserPassword(user.id, nextPassword);
            setPasswordValue('');
            setPasswordSaved(true);
            setTimeout(() => setPasswordSaved(false), 1500);
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Не удалось сменить пароль';
            setSaveError(message);
        } finally {
            setPasswordSaving(false);
        }
    };

    const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!canEditProfile) return;

        const file = e.target.files?.[0];
        if (!file) return;

        const preview = URL.createObjectURL(file);
        setAvatarPreview(preview);

        try {
            await apiClient.uploadUserAvatar(user.id, file);
            const refreshedUser = await apiClient.getUser(user.id);
            writeUserCache(refreshedUser);
            setCachedUser(refreshedUser);
            await refetch();
        } catch (err) {
            console.error('Ошибка загрузки аватара:', err);
        }
    };

    const handleDeleteAvatar = async () => {
        if (!canEditProfile) return;

        try {
            await apiClient.deleteUserAvatar(user.id);
            setAvatarPreview(null);
            const refreshedUser = await apiClient.getUser(user.id);
            writeUserCache(refreshedUser);
            setCachedUser(refreshedUser);
            await refetch();
        } catch (err) {
            console.error('Ошибка удаления аватара:', err);
        }
    };

    const handleCopyEmail = async () => {
        try {
            await navigator.clipboard.writeText(user.email);
            setCopiedEmail(true);
            setTimeout(() => setCopiedEmail(false), 1400);
        } catch {
            setCopiedEmail(false);
        }
    };

    const handleSendPushToUser = async () => {
        if (!canManageEvents) return;
        setPushSending(true);
        setPushError(null);
        setPushResult(null);

        try {
            const res = await apiClient.sendPushToUser(user.id, {
                title: 'MusClub App',
                body: pushMessage.trim() || `Уведомление для пользователя ${user.username}`,
            });

            if (res.sentCount > 0) {
                setPushResult(res.message);
            } else {
                setPushError(res.message);
            }
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Не удалось отправить push-уведомление';
            setPushError(message);
        } finally {
            setPushSending(false);
        }
    };

    return (
        <main className="min-h-screen overflow-y-auto p-5 sm:p-6 md:p-8 pt-8 sm:pt-10 md:pt-12">
            <div className="w-full max-w-4xl mx-auto flex flex-col gap-6">
                <Card className="p-6 sm:p-8 rounded-2xl border shadow-sm" style={{ borderColor: 'var(--color-line-generic)' }}>
                    <div className="flex flex-col sm:flex-row gap-6 sm:gap-8 items-start">
                        <div className="w-full sm:w-auto flex flex-col items-center gap-3">
                            <div
                                className="relative w-28 h-28 sm:w-32 sm:h-32 rounded-full"
                                style={{
                                    border: '1px solid var(--color-line-generic)',
                                }}
                            >
                                {avatarPreview || user.avatarUrl ? (
                                    <img
                                        src={avatarPreview || user.avatarUrl}
                                        alt="avatar"
                                        className="w-full h-full rounded-full object-cover"
                                    />
                                ) : (
                                    <div
                                        className="w-full h-full rounded-full flex items-center justify-center text-4xl font-semibold"
                                        style={{
                                            backgroundColor: 'rgba(37, 99, 235, 0.14)',
                                            color: 'var(--color-text-secondary)',
                                        }}
                                    >
                                        {user.username[0].toUpperCase()}
                                    </div>
                                )}

                                {isEditing && (
                                    <>
                                        <input
                                            id={avatarInputId}
                                            type="file"
                                            accept="image/*"
                                            onChange={handleAvatarChange}
                                            className="hidden"
                                        />
                                        <div className="absolute -top-1.5 -right-1.5 z-10">
                                            <Button
                                                view="flat"
                                                size="s"
                                                pin="round-round"
                                                onClick={() => document.getElementById(avatarInputId)?.click()}
                                            >
                                                <Icon data={Pencil} size={14} />
                                            </Button>
                                        </div>
                                        {(avatarPreview || user.avatarUrl) && (
                                            <div className="absolute -bottom-1.5 -right-1.5 z-10">
                                                <Button
                                                    view="flat"
                                                    size="s"
                                                    pin="round-round"
                                                    onClick={handleDeleteAvatar}
                                                >
                                                    <Icon data={TrashBin} size={14} />
                                                </Button>
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>

                            <Text variant="caption-2" color="secondary">
                                ID: #{user.id}
                            </Text>
                        </div>

                        <div className="flex-1 min-w-0 flex flex-col gap-4">
                            <Text variant="subheader-1" color="secondary">Профиль участника</Text>

                            {isEditing ? (
                                <input
                                    type="text"
                                    value={editData.username}
                                    onChange={(e) => setEditData({ ...editData, username: e.target.value })}
                                    className="w-full px-3 py-2 border rounded-lg text-3xl font-bold"
                                    style={{
                                        borderColor: 'var(--color-line-generic)',
                                        backgroundColor: 'var(--color-background-secondary)',
                                        color: 'var(--color-text-primary)',
                                    }}
                                />
                            ) : (
                                <h1 className="text-3xl sm:text-4xl font-bold break-words">{user.username}</h1>
                            )}

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                <Card className="p-4 rounded-xl" view="clear" style={{ backgroundColor: 'var(--color-background-secondary)' }}>
                                    <div className="flex flex-col gap-1">
                                        <Text variant="caption-2" color="secondary">Email</Text>
                                    {isEditing && canManageUsers ? (
                                        <input
                                            type="email"
                                            value={editData.email}
                                            onChange={(e) => setEditData({ ...editData, email: e.target.value })}
                                            className="w-full mt-1 px-2 py-1 border rounded"
                                            style={{
                                                borderColor: 'var(--color-line-generic)',
                                                backgroundColor: 'var(--color-background)',
                                                color: 'var(--color-text-primary)',
                                            }}
                                        />
                                    ) : (
                                        <button
                                            onClick={handleCopyEmail}
                                            title="Нажмите, чтобы скопировать"
                                            className="text-left text-base font-medium wrap-break-word hover:text-[--g-color-text-link]"
                                        >
                                            {user.email}
                                        </button>
                                    )}
                                    {copiedEmail && (
                                        <Text variant="caption-2" style={{ color: 'rgb(37, 99, 235)' }}>
                                            Скопировано
                                        </Text>
                                    )}
                                    </div>
                                </Card>

                                <Card className="p-4 rounded-xl" view="clear" style={{ backgroundColor: 'var(--color-background-secondary)' }}>
                                    <div className="flex flex-col gap-1">
                                        <Text variant="caption-2" color="secondary">Роль</Text>
                                    {isEditing && canManageUsers ? (
                                        <Select
                                            value={[editData.role]}
                                            onUpdate={([value]) => setEditData({ ...editData, role: value })}
                                            size="l"
                                            options={[
                                                { value: 'MEMBER', content: 'Участник' },
                                                { value: 'ORGANIZER', content: 'Организатор' },
                                            ]}
                                        />
                                    ) : (
                                        <Text variant="subheader-2">
                                            {formatRole(user.role)}
                                        </Text>
                                    )}
                                    </div>
                                </Card>
                            </div>

                            <div className="flex items-center gap-2 text-[--g-color-text-secondary] text-sm">
                                <Icon data={Calendar} size={16} />
                                <span>Зарегистрирован: {new Date(user.createdAt).toLocaleDateString('ru-RU')}</span>
                            </div>

                            {isEditing && canChangePassword && (
                                <div
                                    className="mt-1 p-4 rounded-xl border"
                                    style={{
                                        borderColor: 'var(--color-line-generic)',
                                        backgroundColor: 'var(--color-background-secondary)',
                                    }}
                                >
                                    <div className="flex flex-col gap-2">
                                        <Text variant="body-2">Новый пароль</Text>
                                        <input
                                            type="password"
                                            value={passwordValue}
                                            onChange={(e) => setPasswordValue(e.target.value)}
                                            placeholder={isSelf ? 'Новый пароль для вашего аккаунта' : 'Новый пароль для пользователя'}
                                            className="w-full px-3 py-2 border rounded-lg"
                                            style={{
                                                borderColor: 'var(--color-line-generic)',
                                                backgroundColor: 'var(--color-background)',
                                                color: 'var(--color-text-primary)',
                                            }}
                                        />
                                        <div className="flex items-center gap-3">
                                            <Button view="outlined" size="m" onClick={handleChangePassword} disabled={passwordSaving || visible}>
                                                {passwordSaving ? 'Сохранение...' : 'Сменить пароль'}
                                            </Button>
                                            {passwordSaved && <Text style={{ color: 'rgb(37, 99, 235)' }}>Готово</Text>}
                                        </div>
                                        <Text variant="caption-2" color="secondary">
                                            Минимум 8 символов.
                                        </Text>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </Card>

                {canManageEvents && (
                    <Card className="p-4 rounded-2xl border shadow-sm" style={{ borderColor: 'var(--color-line-generic)' }}>
                        <div className="flex flex-col gap-3">
                            <Text variant="subheader-2">Отправить push пользователю</Text>
                            <Text variant="body-2" color="secondary">
                                Уведомление придёт только если у пользователя есть активная push-подписка.
                            </Text>

                            <textarea
                                value={pushMessage}
                                onChange={(e) => setPushMessage(e.target.value)}
                                placeholder="Введите текст уведомления..."
                                className="w-full min-h-[88px] px-3 py-2 rounded-lg border resize-y"
                                style={{
                                    borderColor: 'var(--color-line-generic)',
                                    backgroundColor: 'var(--color-background-secondary)',
                                    color: 'var(--color-text-primary)',
                                }}
                            />

                            <Button
                                view="action"
                                className="mt-1"
                                onClick={handleSendPushToUser}
                                disabled={pushSending || visible}
                            >
                                {pushSending ? 'Отправляем...' : 'Отправить push'}
                            </Button>

                            {pushError && (
                                <Text variant="body-2" color="danger">
                                    {pushError}
                                </Text>
                            )}

                            {pushResult && (
                                <Text variant="body-2" color="secondary">
                                    {pushResult}
                                </Text>
                            )}
                        </div>
                    </Card>
                )}

                <div className="flex flex-wrap justify-center gap-3">
                    {saveError && (
                        <div className="w-full">
                            <Card className="p-3">
                                <Text color="danger">{saveError}</Text>
                            </Card>
                        </div>
                    )}
                    {isEditing ? (
                        <>
                            <Button view="outlined" className="min-w-40" size="l" onClick={() => setIsEditing(false)}>
                                Отмена
                            </Button>
                            <Button view="action" className="min-w-40" size="l" onClick={handleSave}>
                                Сохранить
                            </Button>
                        </>
                    ) : (
                        <>
                            <Button view="outlined" className="min-w-40" size="l" onClick={() => router.back()} disabled={visible}>
                                Назад
                            </Button>
                            {canEditProfile && (
                                <Button view="action" className="min-w-40" size="l" onClick={() => setIsEditing(true)} disabled={visible}>
                                    Редактировать
                                </Button>
                            )}
                        </>
                    )}
                </div>
            </div>
        </main>
    );
}
