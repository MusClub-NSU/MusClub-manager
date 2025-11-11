'use client';

import { useParams, useRouter } from 'next/navigation';
import { Button, Icon, Text, Loader, Card} from '@gravity-ui/uikit';
import { Bookmark, HandPointRight, Clock, Persons, Pencil, Check, Xmark, Plus, TrashBin, Person } from '@gravity-ui/icons';
import React, { useState} from 'react';
import { useEvents, useUsers } from '../../../hooks/useApi';
import { useSidebar } from '../../context/SidebarContext';

interface RoleAssignment {
    id: string;
    roleName: string;
    people: string[];
}

export default function EventDetailsPage() {
    const { id } = useParams();
    const router = useRouter();
    const { events, loading, error, updateEvent } = useEvents({ page: 0, size: 100 });
    const { users } = useUsers({ page: 0, size: 100 });
    const { visible } = useSidebar();
    const [activeTab, setActiveTab] = useState('assignments');

    // Состояние для назначения людей
    const [roleAssignments, setRoleAssignments] = useState<RoleAssignment[]>([]);
    const [newRoleName, setNewRoleName] = useState('');
    const [showAddRole, setShowAddRole] = useState(false);
    const [editingRoleId, setEditingRoleId] = useState<string | null>(null);
    const [newPersonName, setNewPersonName] = useState('');
    const [showAddPerson, setShowAddPerson] = useState<string | null>(null);

    const [isEditing, setIsEditing] = useState(false);
    const event = events.find((e) => e.id === Number(id));

    const toLocalInputFormat = (isoString: string) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        const pad = (n: number) => String(n).padStart(2, '0');
        const local = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
        return local;
    };

    const [editData, setEditData] = useState({
        title: event?.title || '',
        description: event?.description || '',
        startTime: toLocalInputFormat(event?.startTime || ''),
        endTime: toLocalInputFormat(event?.endTime || ''),
        venue: event?.venue || ''
    });
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

    const { date, time } = formatDateTime(editData.startTime || event.startTime);

    const handleSave = async () => {
        try {
            // Преобразуем время начала в ISO
            const start = new Date(editData.startTime);
            const startTime = start.toISOString();

            // Автоматически добавляем 2 часа
            const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
            const endTime = end.toISOString();

            await updateEvent(event.id, {
                title: editData.title,
                description: editData.description || undefined,
                startTime,
                endTime,
                venue: editData.venue || undefined
            });

            setIsEditing(false);
        } catch (err) {
            console.error('Ошибка обновления события:', err);
        }
    };

    // Функции для работы с ролями
    const handleAddRole = () => {
        if (newRoleName.trim()) {
            const newRole: RoleAssignment = {
                id: Date.now().toString(),
                roleName: newRoleName.trim(),
                people: []
            };
            setRoleAssignments([...roleAssignments, newRole]);
            setNewRoleName('');
            setShowAddRole(false);
        }
    };

    const handleDeleteRole = (roleId: string) => {
        setRoleAssignments(roleAssignments.filter(r => r.id !== roleId));
    };

    const handleAddPersonToRole = (roleId: string) => {
        if (newPersonName.trim()) {
            setRoleAssignments(roleAssignments.map(role => 
                role.id === roleId 
                    ? { ...role, people: [...role.people, newPersonName.trim()] }
                    : role
            ));
            setNewPersonName('');
            setShowAddPerson(null);
        }
    };

    const handleDeletePerson = (roleId: string, personIndex: number) => {
        setRoleAssignments(roleAssignments.map(role =>
            role.id === roleId
                ? { ...role, people: role.people.filter((_, idx) => idx !== personIndex) }
                : role
        ));
    };

    const tabsItems = [
        { id: 'assignments', title: 'Назначение людей' },
        { id: 'timeline', title: 'Планирование таймлайна' },
        { id: 'program', title: 'Планирование концертной программы' },
    ];

    return (
        <div
            className="
                relative min-h-screen
                px-6 sm:px-10 md:px-20 lg:px-40 xl:px-56
                py-12
                flex flex-col
                gap-6
            "
        >
            {/* Заголовок */}
            <header className="flex flex-col items-center gap-4 text-center">
                {isEditing ? (
                    <input
                        type="text"
                        value={editData.title}
                        onChange={(e) => setEditData({...editData, title: e.target.value})}
                        className="px-3 py-2 border rounded-lg text-center bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] w-full max-w-2xl text-3xl font-bold"
                    />
                ) : (
                    <h1 className="text-5xl font-bold">{event.title}</h1>
                )}

                <div
                    className="flex flex-wrap justify-center items-center gap-4 text-lg text-[var(--g-color-text-secondary)]">
                    <div className="flex items-center gap-2">
                        <Icon data={Clock} size={20}/>
                        {isEditing ? (
                            <input
                                type="datetime-local"
                                value={editData.startTime}
                                onChange={(e) =>
                                    setEditData({...editData, startTime: e.target.value})
                                }
                                min={new Date((Date.now() + 24 * 60 * 60 * 1000)).toISOString().slice(0, 16)} // запрет прошедших дат
                                className="px-3 py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary]"
                            />
                        ) : (
                            <span>{date} • {time}</span>
                        )}
                    </div>
                    <div className="flex items-center gap-2 text-green-600 font-medium">
                        <Icon data={Bookmark} size={18}/>
                        Активно
                    </div>
                </div>
            </header>

            {/* Описание и локация */}
            <section className="flex flex-col gap-6 text-lg leading-relaxed max-w-4xl mx-auto w-full">
                <div className="flex items-center gap-2">
                    <Icon data={HandPointRight} size={20}/>
                    {isEditing ? (
                        <input
                            type="text"
                            value={editData.venue}
                            onChange={(e) => setEditData({...editData, venue: e.target.value})}
                            className="px-3 py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] w-full"
                        />
                    ) : (
                        <span>{event.venue}</span>
                    )}
                </div>

                {isEditing ? (
                    <textarea
                        value={editData.description}
                        onChange={(e) => setEditData({...editData, description: e.target.value})}
                        className="w-full h-[60px] px-3 py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] resize-none overflow-y-auto leading-tight"
                        rows={2}
                    />
                ) : (
                    <p className="text-[var(--g-color-text-secondary)] whitespace-pre-wrap">
                        {event.description}
                    </p>
                )}
            </section>

            {/* Табы */}
            <div className="max-w-6xl mx-auto w-full">
            <div className="border-b border-[--g-color-line-generic] mb-6">
                    <div className="flex gap-4">
                        {tabsItems.map((tab) => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`px-4 py-3 font-medium border-b-2 transition-colors ${
                                    activeTab === tab.id
                                        ? 'border-blue-500 text-blue-600'
                                        : 'border-transparent text-[--g-color-text-secondary] hover:text-[--g-color-text-primary]'
                                }`}
                            >
                                {tab.title}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Таб: Назначение людей */}
                {activeTab === 'assignments' && (
                    <div className="space-y-4">
                        <div className="flex justify-between items-center mb-4">
                            <Text variant="subheader-1">Назначенные роли и люди</Text>
                            {!showAddRole && (
                                <Button
                                    view="action"
                                    size="m"
                                    onClick={() => setShowAddRole(true)}
                                >
                                    <span className="flex items-center justify-center gap-2">
                                        <Plus size={16}/>
                                        <span>Добавить роль</span>
                                    </span>
                                </Button>
                            )}
                        </div>

                        {showAddRole && (
                            <Card className="p-4 mb-4">
                                <div className="flex gap-2 items-center">
                                    <input
                                        type="text"
                                        placeholder="Название роли (например: Режиссер)"
                                        value={newRoleName}
                                        onChange={(e) => setNewRoleName(e.target.value)}
                                        onKeyPress={(e) => e.key === 'Enter' && handleAddRole()}
                                        className="flex-1 px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        autoFocus
                                    />
                                    <Button
                                        view="action"
                                        onClick={handleAddRole}
                                        disabled={!newRoleName.trim()}
                                    >
                                        Добавить
                                    </Button>
                                    <Button
                                        view="flat"
                                        onClick={() => {
                                            setShowAddRole(false);
                                            setNewRoleName('');
                                        }}
                                    >
                                        <Icon data={Xmark} size={16}/>
                                    </Button>
                                </div>
                            </Card>
                        )}

                        {roleAssignments.length === 0 ? (
                            <Card className="p-8 text-center">
                                <Text color="secondary">
                                    Роли не назначены. Добавьте первую роль, нажав кнопку выше.
                                </Text>
                            </Card>
                        ) : (
                            <div className="space-y-4">
                                {roleAssignments.map((role) => (
                                    <Card key={role.id} className="p-6">
                                        <div className="flex justify-between items-start mb-4">
                                            <div className="flex items-center gap-3">
                                                <Text variant="subheader-2" className="font-semibold">
                                                    {role.roleName}
                                                </Text>
                                                <Text color="secondary" className="text-sm">
                                                    ({role.people.length} {role.people.length === 1 ? 'человек' : 'человек'})
                                                </Text>
                                            </div>
                                            <div className="flex gap-2">
                                                {showAddPerson !== role.id && (
                                                    <Button
                                                        view="flat"
                                                        size="s"
                                                        onClick={() => setShowAddPerson(role.id)}
                                                    >
                                                        <span className="flex items-center gap-1">
                                                            <Plus size={14}/>
                                                            <span>Добавить человека</span>
                                                        </span>
                                                    </Button>
                                                )}
                                                <Button
                                                    view="flat"
                                                    size="s"
                                                    onClick={() => handleDeleteRole(role.id)}
                                                >
                                                    <Icon data={TrashBin} size={14}/>
                                                </Button>
                                            </div>
                                        </div>

                                        {showAddPerson === role.id && (
                                            <div
                                                className="flex gap-2 mb-4 p-3 bg-[--g-color-base-generic-hover] rounded-lg">
                                                <input
                                                    type="text"
                                                    placeholder="Имя человека"
                                                    value={newPersonName}
                                                    onChange={(e) => setNewPersonName(e.target.value)}
                                                    onKeyPress={(e) => e.key === 'Enter' && handleAddPersonToRole(role.id)}
                                                    className="flex-1 px-3 py-2 border border-[--foreground]/20 rounded bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                                                    autoFocus
                                                />
                                                <Button
                                                    view="action"
                                                    size="s"
                                                    onClick={() => handleAddPersonToRole(role.id)}
                                                    disabled={!newPersonName.trim()}
                                                >
                                                    Добавить
                                                </Button>
                                                <Button
                                                    view="flat"
                                                    size="s"
                                                    onClick={() => {
                                                        setShowAddPerson(null);
                                                        setNewPersonName('');
                                                    }}
                                                >
                                                    <Icon data={Xmark} size={14}/>
                                                </Button>
                                            </div>
                                        )}

                                        {role.people.length > 0 ? (
                                            <div className="space-y-2">
                                                {role.people.map((person, idx) => (
                                                    <div
                                                        key={idx}
                                                        className="flex justify-between items-center p-3 bg-[--g-color-base-generic-hover] rounded-lg"
                                                    >
                                                        <div className="flex items-center gap-2">
                                                            <Icon data={Person} size={16}/>
                                                            <Text>{person}</Text>
                                                        </div>
                                                        <Button
                                                            view="flat"
                                                            size="s"
                                                            onClick={() => handleDeletePerson(role.id, idx)}
                                                        >
                                                            <Icon data={TrashBin} size={14}/>
                                                        </Button>
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <Text color="secondary" className="text-sm italic">
                                                Пока никто не назначен на эту роль
                                            </Text>
                                        )}
                                    </Card>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* Таб: Планирование таймлайна */}
                {activeTab === 'timeline' && (
                    <div>
                        <Card className="p-8 text-center">
                            <Text variant="subheader-1" color="secondary">
                                Планирование таймлайна
                            </Text>
                            <Text color="secondary" className="mt-2">
                                Функционал в разработке
                            </Text>
                        </Card>
                    </div>
                )}

                {/* Таб: Планирование концертной программы */}
                {activeTab === 'program' && (
                    <div>
                        <Card className="p-8 text-center">
                            <Text variant="subheader-1" color="secondary">
                                Планирование концертной программы
                            </Text>
                            <Text color="secondary" className="mt-2">
                                Функционал в разработке
                            </Text>
                        </Card>
                    </div>
                )}
            </div>

            {/* Кнопка назад */}
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
                        <Button view="outlined" className="min-w-[160px]" size="l" onClick={() => router.back()} disabled={visible}>
                            Назад
                        </Button>
                        <Button
                            view="outlined"
                            className="min-w-[160px]"
                            size="l"
                            onClick={() => {
                                setEditData({
                                    title: event.title || '',
                                    description: event.description || '',
                                    startTime: toLocalInputFormat(event.startTime || ''),
                                    endTime: toLocalInputFormat(event.endTime || ''),
                                    venue: event.venue || ''
                                });
                                setIsEditing(true);
                            }}
                        >
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
