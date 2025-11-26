'use client';

import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { Card, Button, Text, Icon, Loader, Select} from '@gravity-ui/uikit';
import { Person, Envelope, Calendar, Tags, Pencil } from '@gravity-ui/icons';
import { useUsers } from '../../../hooks/useApi';
import { useSidebar } from '../../context/SidebarContext';

export default function UserDetailsPage() {
    const params = useParams();
    const router = useRouter();
    const { visible } = useSidebar();

    const userId = Number(params.id);

    const { users, loading, error, updateUser} = useUsers({ page: 0, size: 999 });
    const user = users.find((u) => u.id === userId);

    const [isEditing, setIsEditing] = useState(false);
    const [editData, setEditData] = useState({
        username: '',
        email: '',
        role: ''
    });

    const [avatarPreview, setAvatarPreview] = useState<string | null>(null);

    useEffect(() => {
        if (user) {
            setEditData({
                username: user.username,
                email: user.email,
                role: user.role
            });
        }
    }, [user]);

    if (loading || !user) {
        return (
            <main className="flex justify-center items-center min-h-screen">
                <Loader size="l" />
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
        await updateUser(user.id, editData);
        setIsEditing(false);
    };

    const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        const preview = URL.createObjectURL(file);
        setAvatarPreview(preview);
    };

    return (
        <div
            className="
                relative min-h-screen
                px-4 sm:px-6 md:px-10 lg:px-20 xl:px-40 2xl:px-56
                py-6 sm:py-8 md:py-12
                flex flex-col
                gap-6
            "
        >
            {/* Заголовок */}
            <header className="flex flex-col items-center gap-4 text-center px-4">
                {isEditing ? (
                    <input
                        type="text"
                        value={editData.username}
                        onChange={(e) => setEditData({...editData, username: e.target.value})}
                        className="px-3 py-2 border rounded-lg text-center bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] w-full max-w-xl text-3xl font-bold"
                    />
                ) : (
                    <h1 className="text-3xl sm:text-4xl md:text-5xl font-bold break-words">
                        {user.username}
                    </h1>
                )}

                <div className="flex items-center gap-2 text-[--g-color-text-secondary] text-sm sm:text-base">
                    <Icon data={Calendar} size={18}/>
                    <span>Зарегистрирован: {new Date(user.createdAt).toLocaleDateString('ru-RU')}</span>
                </div>
            </header>

            {/* Основная информация */}
            <section className="max-w-4xl mx-auto w-full px-4">
                <Card className="p-6 rounded-2xl flex flex-col sm:flex-row gap-8">

                    {/* Левая часть: фото + email */}
                    <div className="flex flex-col items-center sm:items-start gap-4 w-full sm:w-1/3">

                        {/* Аватар */}
                        <div className="relative w-32 h-32">
                            {avatarPreview ? (
                                <img
                                    src={avatarPreview}
                                    alt="avatar"
                                    className="w-32 h-32 rounded-full object-cover border"
                                />
                            ) : (
                                <div
                                    className="w-32 h-32 rounded-full bg-[--g-color-base-generic-hover] flex items-center justify-center text-[--g-color-text-secondary] text-5xl">
                                    {user.username[0].toUpperCase()}
                                </div>
                            )}

                            {isEditing && (
                                <>
                                    <input
                                        id="avatar-input"
                                        type="file"
                                        accept="image/*"
                                        onChange={handleAvatarChange}
                                        className="hidden"
                                    />
                                    <div className="absolute top-[-6px] right-[-6px] z-10">
                                        <Button
                                            view="flat"
                                            size="s"
                                            pin="round-round"
                                            onClick={() => document.getElementById('avatar-input')?.click()}
                                        >
                                            <Icon data={Pencil} size={16}/>
                                        </Button>
                                    </div>
                                </>
                            )}
                        </div>

                        {/* Email */}
                        <div className="flex items-center gap-3 w-full">
                            <Icon data={Envelope} size={20} className="shrink-0"/>
                            {isEditing ? (
                                <input
                                    type="email"
                                    value={editData.email}
                                    onChange={(e) => setEditData({...editData, email: e.target.value})}
                                    className="flex-1 px-3 py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] text-lg sm:text-xl font-medium"
                                />
                            ) : (
                                <button
                                    onClick={() => navigator.clipboard.writeText(user.email)}
                                    className="
                break-words text-lg sm:text-xl font-medium text-left
                hover:text-[--g-color-text-link] transition
                cursor-pointer
            "
                                >
                                    {user.email}
                                </button>
                            )}
                        </div>
                    </div>

                    {/* Правая часть: статус и теги */}
                    <div className="flex flex-col gap-6 w-full sm:w-2/3">

                        {/* Роль пользователя */}
                        <div className="flex items-center gap-3 w-full">
                            <Icon data={Person} size={20} className="shrink-0"/>
                            {isEditing ? (
                                <Select
                                    value={[editData.role]}
                                    onUpdate={([value]) => setEditData({...editData, role: value})}
                                    size="xl"
                                    className="w-full text-lg sm:text-xl font-semibold"
                                    options={[
                                        {value: 'MEMBER', content: 'Участник'},
                                        {value: 'ORGANIZER', content: 'Организатор'},
                                    ]}>
                                </Select>
                            ) : (
                                <span className="text-lg sm:text-xl font-semibold">
                {user.role === 'ORGANIZER' ? 'Организатор' : 'Участник'}
            </span>
                            )}
                        </div>

                        {/* Теги */}
                        <div className="flex flex-wrap gap-2">
                            <Icon data={Tags} size={20}/>
                            <span className="px-3 py-1 rounded-full text-sm bg-blue-200 text-blue-800 font-medium">Гитара</span>
                            <span className="px-3 py-1 rounded-full text-sm bg-green-200 text-green-800 font-medium">Фортепиано</span>
                            <span className="px-3 py-1 rounded-full text-sm bg-purple-200 text-purple-800 font-medium">Виолончель</span>
                        </div>
                    </div>

                </Card>
            </section>


            {/* Действия */}
            <div className="flex justify-center gap-6 mt-8">
                {isEditing ? (
                    <>
                        <Button view="outlined" className="min-w-[160px]" size="l" onClick={() => setIsEditing(false)}>
                            Отмена
                        </Button>
                        <Button view="action" className="min-w-[160px]" size="l" onClick={handleSave}>
                            Сохранить
                        </Button>
                    </>
                ) : (
                    <>
                        <Button view="outlined" className="min-w-[160px]" size="l" onClick={() => router.back()}
                                disabled={visible}>
                            Назад
                        </Button>

                        <Button view="outlined" className="min-w-[160px]" size="l" onClick={() => setIsEditing(true)}
                                disabled={visible}>
                            Редактировать
                        </Button>
                    </>
                )}
            </div>

            {visible && (
                <div className="fixed inset-0 bg-background/70 z-40"/>
            )}
        </div>
    );
}
