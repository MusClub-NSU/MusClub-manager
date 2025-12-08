'use client';

import { useParams, useRouter } from 'next/navigation';
import { Button, Icon, Text, Loader, Card, Select} from '@gravity-ui/uikit';
import { Bookmark, HandPointRight, Clock, Persons, Pencil, Check, Xmark, Plus, TrashBin, Person, ArrowUp, ArrowDown, MusicNote } from '@gravity-ui/icons';
import React, { useState, useEffect} from 'react';
import { useEvents, useUsers } from '../../../hooks/useApi';
import { useSidebar } from '../../context/SidebarContext';
import { apiClient } from '../../../lib/api';
import { EventMember } from '../../../types/api';

interface TimelineEvent {
    id: string;
    time: string; // формат HH:mm
    description: string;
}

interface ProgramItem {
    id: string;
    title: string;
    artist?: string;
    time?: string; // формат HH:mm
    duration?: string; // формат MM:SS или просто минуты
    notes?: string;
}

export default function EventDetailsPage() {
    const { id } = useParams();
    const router = useRouter();
    const { events, loading, error, updateEvent } = useEvents({ page: 0, size: 100 });
    const { users, loading: usersLoading } = useUsers({ page: 0, size: 100 });
    const { visible } = useSidebar();
    const [activeTab, setActiveTab] = useState('assignments');

    // Состояние для участников события
    const [eventMembers, setEventMembers] = useState<EventMember[]>([]);
    const [membersLoading, setMembersLoading] = useState(false);
    const [newRoleName, setNewRoleName] = useState('');
    const [showAddRole, setShowAddRole] = useState(false);
    const [showAddPerson, setShowAddPerson] = useState<string | null>(null);
    const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
    const [createdRoles, setCreatedRoles] = useState<string[]>([]); // Роли, созданные но без участников

    // Состояние для таймлайна
    const [timelineEvents, setTimelineEvents] = useState<TimelineEvent[]>([]);
    const [showAddTimelineEvent, setShowAddTimelineEvent] = useState(false);
    const [editingTimelineEventId, setEditingTimelineEventId] = useState<string | null>(null);
    const [newTimelineTime, setNewTimelineTime] = useState('09:00');
    const [newTimelineDescription, setNewTimelineDescription] = useState('');
    
    // Состояние для концертной программы
    const [programItems, setProgramItems] = useState<ProgramItem[]>([]);
    const [showAddProgramItem, setShowAddProgramItem] = useState(false);
    const [editingProgramItemId, setEditingProgramItemId] = useState<string | null>(null);
    const [newProgramTitle, setNewProgramTitle] = useState('');
    const [newProgramArtist, setNewProgramArtist] = useState('');
    const [newProgramTime, setNewProgramTime] = useState('');
    const [newProgramDuration, setNewProgramDuration] = useState('');
    const [newProgramNotes, setNewProgramNotes] = useState('');
    

    const [isEditing, setIsEditing] = useState(false);
    const event = events.find((e) => e.id === Number(id));

    // Загрузка участников события
    useEffect(() => {
        // Сбрасываем состояние при переключении между мероприятиями
        setEventMembers([]);
        setSelectedUserId(null);
        setShowAddPerson(null);
        setShowAddRole(false);
        setNewRoleName('');
        setCreatedRoles([]);
        
        if (event?.id) {
            loadEventMembers();
        }
    }, [event?.id]);

    const loadEventMembers = async () => {
        if (!event?.id) return;
        try {
            setMembersLoading(true);
            const members = await apiClient.getEventMembers(event.id);
            setEventMembers(members);
        } catch (err) {
            console.error('Ошибка загрузки участников:', err);
        } finally {
            setMembersLoading(false);
        }
    };

    // Группировка участников по ролям
    const membersByRole = eventMembers.reduce((acc, member) => {
        if (!acc[member.role]) {
            acc[member.role] = [];
        }
        acc[member.role].push(member);
        return acc;
    }, {} as Record<string, EventMember[]>);

    // Объединяем роли из бекенда и созданные локально (но еще без участников)
    const allRoleNames = [...new Set([...Object.keys(membersByRole), ...createdRoles])];
    const roleNames = allRoleNames;

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

    // Функции для работы с ролями и участниками
    const handleCreateRole = () => {
        if (!newRoleName.trim()) return;
        const roleName = newRoleName.trim();
        if (!createdRoles.includes(roleName) && !roleNames.includes(roleName)) {
            setCreatedRoles([...createdRoles, roleName]);
            setShowAddRole(false);
            setShowAddPerson(roleName);
            setNewRoleName('');
        }
    };

    const handleAddPersonToRole = async (roleName: string) => {
        if (!event?.id || !selectedUserId) return;
        try {
            await apiClient.upsertEventMember(event.id, {
                userId: selectedUserId,
                role: roleName.trim()
            });
            // Удаляем роль из списка созданных, так как теперь она есть в бекенде
            setCreatedRoles(createdRoles.filter(r => r !== roleName));
            await loadEventMembers();
            setSelectedUserId(null);
            setShowAddPerson(null);
            setNewRoleName('');
        } catch (err) {
            console.error('Ошибка добавления участника:', err);
        }
    };

    const handleRemoveMember = async (userId: number) => {
        if (!event?.id) return;
        try {
            await apiClient.removeEventMember(event.id, userId);
            await loadEventMembers();
        } catch (err) {
            console.error('Ошибка удаления участника:', err);
        }
    };

    const handleUserClick = (userId: number) => {
        router.push(`/participants/${userId}`);
    };

    // Получение доступных пользователей для выбора (исключая уже назначенных)
    const getAvailableUsers = (roleName: string) => {
        const assignedUserIds = membersByRole[roleName]?.map(m => m.userId) || [];
        return users.filter(user => !assignedUserIds.includes(user.id));
    };

    // Функции для работы с таймлайном
    const handleOpenAddForm = () => {
        setNewTimelineTime('09:00');
        setNewTimelineDescription('');
        setEditingTimelineEventId(null);
        setShowAddTimelineEvent(true);
    };

    const handleEditTimelineEvent = (eventId: string) => {
        const event = timelineEvents.find(e => e.id === eventId);
        if (event) {
            setNewTimelineTime(event.time);
            setNewTimelineDescription(event.description);
            setEditingTimelineEventId(eventId);
            setShowAddTimelineEvent(true);
        }
    };

    const handleSaveTimelineEvent = () => {
        const timeString = newTimelineTime;
        if (newTimelineDescription.trim()) {
            if (editingTimelineEventId) {
                // Обновляем существующее событие
                setTimelineEvents(timelineEvents.map(e => 
                    e.id === editingTimelineEventId 
                        ? { ...e, time: timeString, description: newTimelineDescription.trim() }
                        : e
                ).sort((a, b) => {
                    const [aHours, aMinutes] = a.time.split(':').map(Number);
                    const [bHours, bMinutes] = b.time.split(':').map(Number);
                    const aTotal = aHours * 60 + aMinutes;
                    const bTotal = bHours * 60 + bMinutes;
                    return aTotal - bTotal;
                }));
            } else {
                // Добавляем новое событие
                const newEvent: TimelineEvent = {
                    id: Date.now().toString(),
                    time: timeString,
                    description: newTimelineDescription.trim()
                };
                // Сортируем по времени при добавлении
                const sorted = [...timelineEvents, newEvent].sort((a, b) => {
                    const [aHours, aMinutes] = a.time.split(':').map(Number);
                    const [bHours, bMinutes] = b.time.split(':').map(Number);
                    const aTotal = aHours * 60 + aMinutes;
                    const bTotal = bHours * 60 + bMinutes;
                    return aTotal - bTotal;
                });
                setTimelineEvents(sorted);
            }
            setNewTimelineTime('09:00');
            setNewTimelineDescription('');
            setEditingTimelineEventId(null);
            setShowAddTimelineEvent(false);
        }
    };

    const handleDeleteTimelineEvent = (eventId: string) => {
        setTimelineEvents(timelineEvents.filter(e => e.id !== eventId));
    };

    // Функции для работы с концертной программой
    const handleOpenAddProgramForm = () => {
        setNewProgramTitle('');
        setNewProgramArtist('');
        setNewProgramTime('');
        setNewProgramDuration('');
        setNewProgramNotes('');
        setEditingProgramItemId(null);
        setShowAddProgramItem(true);
    };

    const handleEditProgramItem = (itemId: string) => {
        const item = programItems.find(i => i.id === itemId);
        if (item) {
            setNewProgramTitle(item.title);
            setNewProgramArtist(item.artist || '');
            setNewProgramTime(item.time || '');
            setNewProgramDuration(item.duration || '');
            setNewProgramNotes(item.notes || '');
            setEditingProgramItemId(itemId);
            setShowAddProgramItem(true);
        }
    };

    const handleSaveProgramItem = () => {
        if (newProgramTitle.trim()) {
            if (editingProgramItemId) {
                // Обновляем существующий элемент
                setProgramItems(programItems.map(item => 
                    item.id === editingProgramItemId 
                        ? { 
                            ...item, 
                            title: newProgramTitle.trim(),
                            artist: newProgramArtist.trim() || undefined,
                            time: newProgramTime.trim() || undefined,
                            duration: newProgramDuration.trim() || undefined,
                            notes: newProgramNotes.trim() || undefined
                        }
                        : item
                ));
            } else {
                // Добавляем новый элемент
                const newItem: ProgramItem = {
                    id: Date.now().toString(),
                    title: newProgramTitle.trim(),
                    artist: newProgramArtist.trim() || undefined,
                    time: newProgramTime.trim() || undefined,
                    duration: newProgramDuration.trim() || undefined,
                    notes: newProgramNotes.trim() || undefined
                };
                setProgramItems([...programItems, newItem]);
            }
            setNewProgramTitle('');
            setNewProgramArtist('');
            setNewProgramTime('');
            setNewProgramDuration('');
            setNewProgramNotes('');
            setEditingProgramItemId(null);
            setShowAddProgramItem(false);
        }
    };

    const handleDeleteProgramItem = (itemId: string) => {
        setProgramItems(programItems.filter(item => item.id !== itemId));
    };

    const handleMoveProgramItem = (itemId: string, direction: 'up' | 'down') => {
        const index = programItems.findIndex(item => item.id === itemId);
        if (index === -1) return;
        
        const newIndex = direction === 'up' ? index - 1 : index + 1;
        if (newIndex < 0 || newIndex >= programItems.length) return;
        
        const newItems = [...programItems];
        [newItems[index], newItems[newIndex]] = [newItems[newIndex], newItems[index]];
        setProgramItems(newItems);
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
                px-4 sm:px-6 md:px-10 lg:px-20 xl:px-40 2xl:px-56
                py-6 sm:py-8 md:py-12
                flex flex-col
                gap-4 sm:gap-6
            "
        >
            {/* Заголовок */}
            <header className="flex flex-col items-center gap-3 sm:gap-4 text-center px-4">
                {isEditing ? (
                    <input
                        type="text"
                        value={editData.title}
                        onChange={(e) => setEditData({...editData, title: e.target.value})}
                        className="px-3 py-2 border rounded-lg text-center bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] w-full max-w-2xl text-xl sm:text-3xl font-bold"
                    />
                ) : (
                    <h1 className="text-3xl sm:text-4xl md:text-5xl font-bold break-words">{event.title}</h1>
                )}

                <div className="flex flex-wrap justify-center items-center gap-3 sm:gap-4 text-sm sm:text-base md:text-lg text-[var(--g-color-text-secondary)]">
                    <div className="flex items-center gap-2">
                        <Icon data={Clock} size={18} className="sm:w-5 sm:h-5"/>
                        {isEditing ? (
                            <input
                                type="datetime-local"
                                value={editData.startTime}
                                onChange={(e) =>
                                    setEditData({...editData, startTime: e.target.value})
                                }
                                min={new Date((Date.now() + 24 * 60 * 60 * 1000)).toISOString().slice(0, 16)}
                                className="px-2 py-1 sm:px-3 sm:py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] text-sm sm:text-base"
                            />
                        ) : (
                            <span>{date} • {time}</span>
                        )}
                    </div>
                    <div className="flex items-center gap-2 text-green-600 font-medium">
                        <Icon data={Bookmark} size={16} className="sm:w-[18px] sm:h-[18px]"/>
                        <span>Активно</span>
                    </div>
                </div>
            </header>

            {/* Описание и локация */}
            <section className="flex flex-col gap-4 sm:gap-6 text-base sm:text-lg leading-relaxed max-w-4xl mx-auto w-full px-4">
                <div className="flex items-center gap-2">
                    <Icon data={HandPointRight} size={18} className="sm:w-5 sm:h-5 flex-shrink-0"/>
                    {isEditing ? (
                        <input
                            type="text"
                            value={editData.venue}
                            onChange={(e) => setEditData({...editData, venue: e.target.value})}
                            className="px-3 py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] w-full text-sm sm:text-base"
                        />
                    ) : (
                        <span className="break-words">{event.venue}</span>
                    )}
                </div>

                {isEditing ? (
                    <textarea
                        value={editData.description}
                        onChange={(e) => setEditData({...editData, description: e.target.value})}
                        className="w-full h-[60px] px-3 py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] resize-none overflow-y-auto leading-tight text-sm sm:text-base"
                        rows={2}
                    />
                ) : (
                    <p className="text-[var(--g-color-text-secondary)] whitespace-pre-wrap break-words">
                        {event.description}
                    </p>
                )}
            </section>

            {/* Табы */}
            <div className="max-w-6xl mx-auto w-full">
                <div className="border-b border-[--g-color-line-generic] mb-4 sm:mb-6">
                    <div className="flex gap-2 sm:gap-4 overflow-x-auto scrollbar-hide">
                        {tabsItems.map((tab) => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`px-3 py-2 sm:px-4 sm:py-3 font-medium border-b-2 transition-colors whitespace-nowrap text-sm sm:text-base ${
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
                        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 sm:gap-0 mb-4">
                            <Text variant="subheader-1" className="text-lg sm:text-xl">Назначенные роли и люди</Text>
                            {!showAddRole && (
                                <Button
                                    view="action"
                                    size="m"
                                    onClick={() => {
                                        setShowAddRole(true);
                                        setNewRoleName('');
                                    }}
                                >
                                    <span className="flex items-center justify-center gap-2">
                                        <Icon data={Plus} size={16}/>
                                        <span className="hidden sm:inline">Создать роль</span>
                                        <span className="sm:hidden">Роль</span>
                                    </span>
                                </Button>
                            )}
                        </div>

                        {showAddRole && (
                            <Card className="p-4 mb-4 border-2 border-blue-300">
                                <div className="flex flex-col gap-3">
                                    <Text variant="subheader-2" className="font-semibold">Шаг 1: Создать роль</Text>
                                    <div className="flex flex-col sm:flex-row gap-2">
                                        <input
                                            type="text"
                                            placeholder="Название роли (например: Режиссер)"
                                            value={newRoleName}
                                            onChange={(e) => setNewRoleName(e.target.value)}
                                            onKeyPress={(e) => {
                                                if (e.key === 'Enter' && newRoleName.trim()) {
                                                    handleCreateRole();
                                                }
                                            }}
                                            className="flex-1 px-3 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm sm:text-base"
                                            autoFocus
                                        />
                                        <div className="flex gap-2">
                                            <Button
                                                view="action"
                                                onClick={handleCreateRole}
                                                disabled={!newRoleName.trim()}
                                                className="flex-1 sm:flex-initial"
                                            >
                                                Создать роль
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
                                    </div>
                                </div>
                            </Card>
                        )}

                        {membersLoading ? (
                            <Card className="p-8 text-center">
                                <Loader size="m" />
                                <Text color="secondary" className="mt-2">Загрузка участников...</Text>
                            </Card>
                        ) : roleNames.length === 0 ? (
                            <Card className="p-8 text-center">
                                <Text color="secondary" className="mb-4">
                                    Роли не назначены. Создайте первую роль, нажав кнопку выше.
                                </Text>
                            </Card>
                        ) : (
                            <div className="space-y-4">
                                {roleNames.map((roleName) => {
                                    const members = membersByRole[roleName] || [];
                                    const isNewRole = createdRoles.includes(roleName) && members.length === 0;
                                    const availableUsers = isNewRole 
                                        ? users 
                                        : getAvailableUsers(roleName);
                                    return (
                                        <Card key={roleName} className={`p-4 sm:p-6 ${isNewRole ? 'border-2 border-blue-300' : ''}`}>
                                            <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-3 sm:gap-0 mb-4">
                                                <div className="flex flex-wrap items-center gap-2 sm:gap-3">
                                                    <Text variant="subheader-2" className="font-semibold text-base sm:text-lg">
                                                        {roleName}
                                                    </Text>
                                                    {isNewRole ? (
                                                        <Text color="warning" className="text-xs sm:text-sm font-medium">
                                                            (Роль создана, назначьте участника)
                                                        </Text>
                                                    ) : (
                                                        <Text color="secondary" className="text-xs sm:text-sm">
                                                            ({members.length} {members.length === 1 ? 'человек' : members.length < 5 ? 'человека' : 'человек'})
                                                        </Text>
                                                    )}
                                                </div>
                                                <div className="flex gap-2">
                                                    {showAddPerson !== roleName && (
                                                        <Button
                                                            view="flat"
                                                            size="s"
                                                            onClick={() => {
                                                                setShowAddPerson(roleName);
                                                                setSelectedUserId(null);
                                                            }}
                                                            disabled={usersLoading || users.length === 0}
                                                        >
                                                            <span className="flex items-center gap-1">
                                                                <Icon data={Plus} size={14}/>
                                                                <span className="hidden sm:inline">{isNewRole ? 'Назначить участника' : 'Добавить участника'}</span>
                                                                <span className="sm:hidden">Добавить</span>
                                                            </span>
                                                        </Button>
                                                    )}
                                                </div>
                                            </div>

                                            {showAddPerson === roleName && (
                                                <div className="flex flex-col gap-3 mb-4 p-3 bg-[--g-color-base-generic-hover] rounded-lg">
                                                    {isNewRole && (
                                                        <Text variant="body-2" color="secondary" className="mb-1">
                                                            Шаг 2: Назначьте участника на роль "{roleName}"
                                                        </Text>
                                                    )}
                                                    <div className="flex flex-col sm:flex-row gap-2">
                                                        <Select
                                                            value={selectedUserId ? [selectedUserId.toString()] : []}
                                                            onUpdate={(value) => setSelectedUserId(value[0] ? Number(value[0]) : null)}
                                                            placeholder="Выберите участника"
                                                            size="m"
                                                            className="flex-1"
                                                            disabled={usersLoading || availableUsers.length === 0}
                                                            options={availableUsers.map(user => ({
                                                                value: user.id.toString(),
                                                                content: `${user.username} (${user.email})`
                                                            }))}
                                                        />
                                                        <div className="flex gap-2">
                                                            <Button
                                                                view="action"
                                                                size="s"
                                                                onClick={() => handleAddPersonToRole(roleName)}
                                                                disabled={!selectedUserId}
                                                                className="flex-1 sm:flex-initial"
                                                            >
                                                                {isNewRole ? 'Назначить' : 'Добавить'}
                                                            </Button>
                                                            <Button
                                                                view="flat"
                                                                size="s"
                                                                onClick={() => {
                                                                    setShowAddPerson(null);
                                                                    setSelectedUserId(null);
                                                                    if (isNewRole) {
                                                                        setCreatedRoles(createdRoles.filter(r => r !== roleName));
                                                                    }
                                                                }}
                                                            >
                                                                <Icon data={Xmark} size={14}/>
                                                            </Button>
                                                        </div>
                                                    </div>
                                                </div>
                                            )}

                                            {members.length > 0 ? (
                                                <div className="space-y-2">
                                                    {members.map((member) => (
                                                        <div
                                                            key={member.userId}
                                                            className="flex justify-between items-center p-3 bg-[--g-color-base-generic-hover] rounded-lg hover:bg-[--g-color-base-generic-active] transition-colors cursor-pointer"
                                                            onClick={() => handleUserClick(member.userId)}
                                                        >
                                                            <div className="flex items-center gap-2 flex-1">
                                                                <Icon data={Person} size={16}/>
                                                                <div className="flex flex-col">
                                                                    <Text className="font-medium">{member.username}</Text>
                                                                    <Text color="secondary" className="text-xs">{member.email}</Text>
                                                                </div>
                                                            </div>
                                                            <Button
                                                                view="flat"
                                                                size="s"
                                                                onClick={(e) => {
                                                                    e.stopPropagation();
                                                                    handleRemoveMember(member.userId);
                                                                }}
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
                                    );
                                })}
                            </div>
                        )}
                    </div>
                )}

                {/* Таб: Планирование таймлайна */}
                {activeTab === 'timeline' && (
                    <div className="space-y-6">
                        <div className="flex justify-between items-center">
                            <Text variant="subheader-1" className="font-bold">Планирование таймлайна</Text>
                            {!showAddTimelineEvent && (
                                <Button
                                    view="action"
                                    size="m"
                                    onClick={handleOpenAddForm}
                                >
                                    <span className="flex items-center justify-center gap-2">
                                        <Icon data={Plus} size={16} />
                                        <span>Добавить событие</span>
                                    </span>
                                </Button>
                            )}
                        </div>

                        {showAddTimelineEvent && (
                            <Card className="p-6 mb-4 shadow-lg border-2 border-blue-200 dark:border-blue-800">
                                <div className="flex flex-col gap-4">
                                    <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3">
                                        <label className="text-sm font-semibold sm:min-w-[100px] flex items-center gap-2">
                                            <Icon data={Clock} size={16} />
                                            <span className="hidden sm:inline">Время:</span>
                                            <span className="sm:hidden">Время</span>
                                        </label>
                                        <input
                                            type="time"
                                            value={newTimelineTime}
                                            onChange={(e) => setNewTimelineTime(e.target.value)}
                                            className="flex-1 px-2 py-1 sm:px-3 sm:py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] text-sm sm:text-base focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        />
                                    </div>
                                    <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3">
                                        <label className="text-sm font-semibold sm:min-w-[100px]">Описание:</label>
                                        <input
                                            type="text"
                                            placeholder="Введите описание события..."
                                            value={newTimelineDescription}
                                            onChange={(e) => setNewTimelineDescription(e.target.value)}
                                            onKeyPress={(e) => e.key === 'Enter' && handleSaveTimelineEvent()}
                                            className="flex-1 px-3 sm:px-4 py-2 sm:py-3 border-2 border-[--foreground]/20 rounded-xl bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all text-sm sm:text-base"
                                            autoFocus
                                            required
                                        />
                                    </div>
                                    <div className="flex gap-3 justify-end pt-2">
                                        <Button
                                            view="outlined"
                                            onClick={() => {
                                                setShowAddTimelineEvent(false);
                                                setNewTimelineTime('09:00');
                                                setNewTimelineDescription('');
                                                setEditingTimelineEventId(null);
                                            }}
                                        >
                                            Отмена
                                        </Button>
                                        <Button
                                            view="action"
                                            onClick={handleSaveTimelineEvent}
                                            disabled={!newTimelineDescription.trim()}
                                        >
                                            {editingTimelineEventId ? 'Сохранить' : 'Добавить'}
                                        </Button>
                                    </div>
                                </div>
                            </Card>
                        )}

                        {timelineEvents.length === 0 ? (
                            <Card className="p-12 text-center border-2 border-dashed border-[--g-color-line-generic]">
                                <div className="flex flex-col items-center gap-4">
                                    <div className="w-16 h-16 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
                                        <Icon data={Clock} size={32} className="text-blue-600 dark:text-blue-400" />
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        <Text variant="subheader-1">Нет событий в таймлайне</Text>
                                        <Text color="secondary">
                                            Добавьте первое событие, нажав кнопку выше
                                        </Text>
                                    </div>
                                </div>
                            </Card>
                        ) : (
                            <div className="relative">
                                {/* Вертикальная линия таймлайна - скрыта на мобильных */}
                                <div className="hidden sm:block absolute left-4 md:left-8 top-0 bottom-0 w-1 bg-gradient-to-b from-blue-500 via-blue-400 to-blue-500 rounded-full"></div>
                                
                                <div className="space-y-4">
                                    {timelineEvents.map((event, idx) => (
                                        <div key={event.id} className="relative pl-0 sm:pl-12 md:pl-20 group">
                                            <Card className="p-4 sm:p-5 shadow-md hover:shadow-lg transition-all duration-300 border-l-4 border-l-blue-500 bg-gradient-to-r from-white to-blue-50/30 dark:from-gray-800 dark:to-blue-900/10">
                                                <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 sm:gap-4">
                                                    <div className="flex flex-col sm:flex-row sm:items-start gap-3 sm:gap-4 flex-1">
                                                        {/* Точка таймлайна */}
                                                        <div className="hidden sm:block absolute left-0 top-6 -translate-x-1/2">
                                                            <div className="relative">
                                                                <div className="w-4 h-4 md:w-6 md:h-6 rounded-full bg-blue-500 border-2 md:border-4 border-background shadow-lg flex items-center justify-center">
                                                                    <div className="w-1 h-1 md:w-2 md:h-2 rounded-full bg-white"></div>
                                                                </div>
                                                                {idx < timelineEvents.length - 1 && (
                                                                    <div className="absolute top-4 md:top-6 left-1/2 -translate-x-1/2 w-0.5 h-8 md:h-12 bg-gradient-to-b from-blue-400 to-blue-300"></div>
                                                                )}
                                                            </div>
                                                        </div>
                                                        
                                                        {/* Время */}
                                                        <div className="flex items-center gap-2 sm:gap-3 sm:min-w-[120px]">
                                                            <div className="flex items-center gap-2 px-2 py-1 sm:px-3 sm:py-1.5 rounded-lg bg-blue-100 dark:bg-blue-900/30">
                                                                <Icon data={Clock} size={14} className="text-blue-600 dark:text-blue-400" />
                                                                <Text variant="subheader-2" className="font-bold text-sm sm:text-base text-blue-700 dark:text-blue-300">
                                                                    {event.time}
                                                                </Text>
                                                            </div>
                                                        </div>
                                                        
                                                        {/* Описание события */}
                                                        <div className="flex-1 pt-0 sm:pt-1">
                                                            <Text className="text-base sm:text-lg">{event.description}</Text>
                                                        </div>
                                                    </div>
                                                    
                                                    {/* Кнопки действий */}
                                                    <div className="flex gap-2 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity">
                                                        <Button
                                                            view="flat"
                                                            size="s"
                                                            onClick={() => handleEditTimelineEvent(event.id)}
                                                        >
                                                            <Icon data={Pencil} size={14} />
                                                        </Button>
                                                        <Button
                                                            view="flat"
                                                            size="s"
                                                            onClick={() => handleDeleteTimelineEvent(event.id)}
                                                        >
                                                            <Icon data={TrashBin} size={14} />
                                                        </Button>
                                                    </div>
                                                </div>
                                            </Card>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* Таб: Планирование концертной программы */}
                {activeTab === 'program' && (
                    <div className="space-y-6">
                        <div className="flex justify-between items-center">
                            <Text variant="subheader-1" className="font-bold">Концертная программа</Text>
                            {!showAddProgramItem && (
                                <Button
                                    view="action"
                                    size="m"
                                    onClick={handleOpenAddProgramForm}
                                >
                                    <span className="flex items-center justify-center gap-2">
                                        <Icon data={Plus} size={16} />
                                        <span className="hidden sm:inline">Добавить номер</span>
                                        <span className="sm:hidden">Добавить</span>
                                    </span>
                                </Button>
                            )}
                        </div>

                        {showAddProgramItem && (
                            <Card className="p-4 sm:p-6 mb-4 shadow-lg border-2 border-blue-200 dark:border-blue-800">
                                <div className="flex flex-col gap-4">
                                    <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3">
                                        <label className="text-sm font-semibold sm:min-w-[100px]">Название *:</label>
                                        <input
                                            type="text"
                                            placeholder="Название композиции/номера"
                                            value={newProgramTitle}
                                            onChange={(e) => setNewProgramTitle(e.target.value)}
                                            className="flex-1 px-3 sm:px-4 py-2 sm:py-3 border-2 border-[--foreground]/20 rounded-xl bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all text-sm sm:text-base"
                                            autoFocus
                                            required
                                        />
                                    </div>
                                    <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3">
                                        <label className="text-sm font-semibold sm:min-w-[100px]">Исполнитель:</label>
                                        <input
                                            type="text"
                                            placeholder="Имя исполнителя/группы"
                                            value={newProgramArtist}
                                            onChange={(e) => setNewProgramArtist(e.target.value)}
                                            className="flex-1 px-3 sm:px-4 py-2 sm:py-3 border-2 border-[--foreground]/20 rounded-xl bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all text-sm sm:text-base"
                                        />
                                    </div>
                                    <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3">
                                        <label className="text-sm font-semibold sm:min-w-[100px] flex items-center gap-2">
                                            <Icon data={Clock} size={16} />
                                            <span className="hidden sm:inline">Время:</span>
                                            <span className="sm:hidden">Время</span>
                                        </label>
                                        <input
                                            type="time"
                                            value={newProgramTime}
                                            onChange={(e) => setNewProgramTime(e.target.value)}
                                            className="flex-1 px-2 py-1 sm:px-3 sm:py-2 border rounded-lg bg-[--g-color-base-generic-hover] text-[--g-color-text-primary] text-sm sm:text-base focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        />
                                    </div>
                                    <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3">
                                        <label className="text-sm font-semibold sm:min-w-[100px]">Длительность:</label>
                                        <input
                                            type="text"
                                            placeholder="Например: 3:45 или 4 мин"
                                            value={newProgramDuration}
                                            onChange={(e) => setNewProgramDuration(e.target.value)}
                                            className="flex-1 px-3 sm:px-4 py-2 sm:py-3 border-2 border-[--foreground]/20 rounded-xl bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all text-sm sm:text-base"
                                        />
                                    </div>
                                    <div className="flex flex-col sm:flex-row sm:items-start gap-2 sm:gap-3">
                                        <label className="text-sm font-semibold sm:min-w-[100px] pt-2 sm:pt-0">Примечания:</label>
                                        <textarea
                                            placeholder="Дополнительные заметки..."
                                            value={newProgramNotes}
                                            onChange={(e) => setNewProgramNotes(e.target.value)}
                                            className="flex-1 px-3 sm:px-4 py-2 sm:py-3 border-2 border-[--foreground]/20 rounded-xl bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all text-sm sm:text-base resize-none"
                                            rows={3}
                                        />
                                    </div>
                                    <div className="flex gap-3 justify-end pt-2">
                                        <Button
                                            view="outlined"
                                            onClick={() => {
                                                setShowAddProgramItem(false);
                                                setNewProgramTitle('');
                                                setNewProgramArtist('');
                                                setNewProgramTime('');
                                                setNewProgramDuration('');
                                                setNewProgramNotes('');
                                                setEditingProgramItemId(null);
                                            }}
                                        >
                                            Отмена
                                        </Button>
                                        <Button
                                            view="action"
                                            onClick={handleSaveProgramItem}
                                            disabled={!newProgramTitle.trim()}
                                        >
                                            {editingProgramItemId ? 'Сохранить' : 'Добавить'}
                                        </Button>
                                    </div>
                                </div>
                            </Card>
                        )}

                        {programItems.length === 0 ? (
                            <Card className="p-8 sm:p-12 text-center border-2 border-dashed border-[--g-color-line-generic]">
                                <div className="flex flex-col items-center gap-4">
                                    <div className="w-12 h-12 sm:w-16 sm:h-16 rounded-full bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center">
                                        <Icon data={MusicNote} size={24} className="sm:w-8 sm:h-8 text-purple-600 dark:text-purple-400" />
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        <Text variant="subheader-1">Нет номеров в программе</Text>
                                        <Text color="secondary" className="text-sm sm:text-base">
                                            Добавьте первый номер, нажав кнопку выше
                                        </Text>
                                    </div>
                                </div>
                            </Card>
                        ) : (
                            <div className="space-y-3">
                                {programItems.map((item, idx) => (
                                    <Card key={item.id} className="p-4 sm:p-5 shadow-md hover:shadow-lg transition-all duration-300 border-l-4 border-l-purple-500 bg-gradient-to-r from-white to-purple-50/30 dark:from-gray-800 dark:to-purple-900/10 group">
                                        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 sm:gap-4">
                                            <div className="flex items-start gap-3 sm:gap-4 flex-1">
                                                {/* Номер в программе */}
                                                <div className="flex-shrink-0 w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-purple-500 text-white flex items-center justify-center font-bold text-sm sm:text-base shadow-md">
                                                    {idx + 1}
                                                </div>
                                                
                                                {/* Информация о номере */}
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-3 mb-2">
                                                        <Text variant="subheader-2" className="font-bold text-base sm:text-lg">
                                                            {item.title}
                                                        </Text>
                                                        {item.artist && (
                                                            <Text color="secondary" className="text-sm sm:text-base">
                                                                — {item.artist}
                                                            </Text>
                                                        )}
                                                    </div>
                                                    <div className="flex flex-wrap gap-3 text-sm sm:text-base">
                                                        {item.time && (
                                                            <div className="flex items-center gap-1 px-2 py-1 rounded-lg bg-purple-100 dark:bg-purple-900/30">
                                                                <Icon data={Clock} size={14} className="text-purple-600 dark:text-purple-400" />
                                                                <Text color="secondary" className="font-semibold text-purple-700 dark:text-purple-300">{item.time}</Text>
                                                            </div>
                                                        )}
                                                        {item.duration && (
                                                            <div className="flex items-center gap-1">
                                                                <Text color="secondary">{item.duration}</Text>
                                                            </div>
                                                        )}
                                                    </div>
                                                    {item.notes && (
                                                        <Text color="secondary" className="text-sm sm:text-base mt-2 italic">
                                                            {item.notes}
                                                        </Text>
                                                    )}
                                                </div>
                                            </div>
                                            
                                            {/* Кнопки действий */}
                                            <div className="flex gap-2 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity">
                                                <div className="flex flex-col gap-1">
                                                    <Button
                                                        view="flat"
                                                        size="s"
                                                        onClick={() => handleMoveProgramItem(item.id, 'up')}
                                                        disabled={idx === 0}
                                                    >
                                                        <Icon data={ArrowUp} size={14} />
                                                    </Button>
                                                    <Button
                                                        view="flat"
                                                        size="s"
                                                        onClick={() => handleMoveProgramItem(item.id, 'down')}
                                                        disabled={idx === programItems.length - 1}
                                                    >
                                                        <Icon data={ArrowDown} size={14} />
                                                    </Button>
                                                </div>
                                                <Button
                                                    view="flat"
                                                    size="s"
                                                    onClick={() => handleEditProgramItem(item.id)}
                                                >
                                                    <Icon data={Pencil} size={14} />
                                                </Button>
                                                <Button
                                                    view="flat"
                                                    size="s"
                                                    onClick={() => handleDeleteProgramItem(item.id)}
                                                >
                                                    <Icon data={TrashBin} size={14} />
                                                </Button>
                                            </div>
                                        </div>
                                    </Card>
                                ))}
                            </div>
                        )}
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
