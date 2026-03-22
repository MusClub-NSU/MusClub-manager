'use client';

import { useEffect, useMemo, useState } from 'react';
import { Button, Icon, Text } from '@gravity-ui/uikit';
import { Bars, Persons, Calendar, House, ArrowRightFromSquare, Xmark } from '@gravity-ui/icons';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import '@gravity-ui/uikit/styles/styles.css';
import { useSidebar } from '../context/SidebarContext';
import { useSession, signOut } from 'next-auth/react';
import { useUsers } from '@/hooks/useApi';

type MenuItem = {
    href: string;
    label: string;
    icon: typeof House;
    matchPrefix?: string;
};

export default function Sidebar() {
    const { visible, setVisible, disabled } = useSidebar();
    const { data: session } = useSession();
    const pathname = usePathname();
    const { users } = useUsers({ page: 0, size: 999 });
    const [drawerWidth, setDrawerWidth] = useState(300);
    const [isResizing, setIsResizing] = useState(false);
    const [resizeStart, setResizeStart] = useState<{ startX: number; startWidth: number } | null>(null);

    const getDefaultWidth = () => {
        if (typeof window === 'undefined') return 320;
        return Math.round(window.innerWidth * 0.2);
    };

    const clampWidth = (width: number) => {
        if (typeof window === 'undefined') return width;
        const min = Math.max(260, Math.round(window.innerWidth * 0.16));
        const max = Math.max(420, Math.round(window.innerWidth * 0.32));
        return Math.max(min, Math.min(max, width));
    };

    useEffect(() => {
        if (typeof window === 'undefined') return;
        // Базовая ширина — 1/5 экрана
        setDrawerWidth(clampWidth(getDefaultWidth()));
    }, []);

    const currentUser = useMemo(() => {
        const email = session?.user?.email?.toLowerCase();
        if (!email) return undefined;
        return users.find((u) => u.email.toLowerCase() === email);
    }, [session?.user?.email, users]);

    const profileHref = currentUser ? `/participants/${currentUser.id}` : '/participants';

    useEffect(() => {
        if (!isResizing || !resizeStart) return;

        const onMouseMove = (event: MouseEvent) => {
            // Небольшой шаг изменения для более "динамического" ощущения
            const delta = (event.clientX - resizeStart.startX) * 0.5;
            setDrawerWidth(clampWidth(Math.round(resizeStart.startWidth + delta)));
        };

        const onMouseUp = () => {
            setIsResizing(false);
            setResizeStart(null);
        };

        document.body.style.userSelect = 'none';
        window.addEventListener('mousemove', onMouseMove);
        window.addEventListener('mouseup', onMouseUp);

        return () => {
            document.body.style.userSelect = '';
            window.removeEventListener('mousemove', onMouseMove);
            window.removeEventListener('mouseup', onMouseUp);
        };
    }, [isResizing, resizeStart]);

    useEffect(() => {
        if (typeof window === 'undefined') return;
        const onResize = () => {
            setDrawerWidth((prev) => clampWidth(prev));
        };

        window.addEventListener('resize', onResize);
        return () => window.removeEventListener('resize', onResize);
    }, []);

    const toggleDrawer = () => {
        if (!disabled) setVisible(!visible);
    };

    const menuItems: MenuItem[] = [
        { href: '/', label: 'Главная', icon: House },
        ...(session
            ? [
                  { href: '/participants', label: 'Участники', icon: Persons, matchPrefix: '/participants' },
                  { href: '/events', label: 'Мероприятия', icon: Calendar, matchPrefix: '/events' },
              ]
            : []),
    ];

    const isItemActive = (item: MenuItem) => {
        if (!pathname) return false;
        if (item.href === '/') return pathname === '/';
        return pathname === item.href || pathname.startsWith(`${item.matchPrefix ?? item.href}/`);
    };

    const getNavItemStyle = (active: boolean) => ({
        background: active ? 'rgba(37, 99, 235, 0.16)' : 'transparent',
        color: active ? 'rgb(37, 99, 235)' : 'var(--foreground)',
        border: active ? '1px solid rgba(37, 99, 235, 0.35)' : '1px solid transparent',
    });

    return (
        <>
            {visible && (
                <div
                    role="presentation"
                    aria-hidden
                    onClick={() => setVisible(false)}
                    style={{
                        position: 'fixed',
                        inset: 0,
                        zIndex: 1400,
                        backgroundColor: 'rgba(0, 0, 0, 0.5)',
                    }}
                />
            )}

            <Button
                view="flat"
                onClick={toggleDrawer}
                aria-label={visible ? 'Скрыть меню навигации' : 'Показать меню навигации'}
                className="sidebar-toggle-button"
                style={{
                    position: 'fixed',
                    top: 12,
                    left: 12,
                    zIndex: 2000,
                    width: 44,
                    height: 44,
                    padding: 0,
                    display: visible ? 'none' : 'inline-flex',
                    opacity: disabled ? 0.5 : 1,
                    pointerEvents: disabled ? 'none' : 'auto',
                }}
            >
                <Icon data={Bars} size={24} />
            </Button>

            <aside
                aria-hidden={!visible}
                style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    bottom: 0,
                    width: drawerWidth,
                    maxWidth: '100vw',
                    zIndex: 1500,
                    background: 'var(--background)',
                    borderRight: '1px solid rgba(128, 128, 128, 0.25)',
                    boxShadow: '4px 0 12px rgba(0,0,0,0.35)',
                    transform: visible ? 'translateX(0)' : 'translateX(-100%)',
                    transition: isResizing ? 'none' : 'transform 160ms ease, width 140ms ease-out',
                    pointerEvents: visible ? 'auto' : 'none',
                }}
            >
                <div
                    style={{
                        padding: '16px 20px 20px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '16px',
                        height: '100%',
                        overflowY: 'auto',
                    }}
                >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                            <div className="sidebar-brand-wrap">
                                <h2 className="sidebar-brand-title">MusClub</h2>
                            </div>
                            <Button
                                view="flat"
                                size="s"
                                aria-label="Закрыть меню"
                                onClick={() => setVisible(false)}
                            >
                                <Icon data={Xmark} size={18} />
                            </Button>
                        </div>

                        {/* Информация о пользователе */}
                        {session?.user && (
                            <Link
                                href={profileHref}
                                onClick={() => setVisible(false)}
                                className="sidebar-account-card"
                                style={{
                                    display: 'block',
                                    textDecoration: 'none',
                                    marginBottom: '8px',
                                    borderRadius: '10px',
                                    border: '1px solid rgba(37, 99, 235, 0.22)',
                                    background: 'rgba(37, 99, 235, 0.08)',
                                    padding: '12px',
                                }}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <div
                                        aria-hidden
                                        style={{
                                            width: '34px',
                                            height: '34px',
                                            borderRadius: '999px',
                                            background: 'rgba(37, 99, 235, 0.2)',
                                            color: 'rgb(37, 99, 235)',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            fontWeight: 700,
                                            flexShrink: 0,
                                        }}
                                    >
                                        {(session.user.name?.[0] ?? session.user.email?.[0] ?? 'U').toUpperCase()}
                                    </div>
                                    <div style={{ minWidth: 0, flex: 1 }}>
                                        <Text
                                            variant="subheader-2"
                                            style={{
                                                display: 'block',
                                                whiteSpace: 'nowrap',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            {session.user.name || 'Пользователь'}
                                        </Text>
                                        <Text
                                            color="secondary"
                                            variant="caption-2"
                                            style={{
                                                display: 'block',
                                                marginTop: '2px',
                                                whiteSpace: 'nowrap',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            {session.user.email}
                                        </Text>
                                        <Text
                                            variant="caption-2"
                                            style={{
                                                display: 'block',
                                                color: 'rgb(37, 99, 235)',
                                                marginTop: '6px',
                                                fontWeight: 600,
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            Перейти в профиль
                                        </Text>
                                    </div>
                                </div>
                            </Link>
                        )}

                        <nav
                            aria-label="Основная навигация"
                            style={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: '10px',
                            }}
                        >
                            {menuItems.map((item) => {
                                const isActive = isItemActive(item);
                                return (
                                    <Link
                                        key={item.href}
                                        href={item.href}
                                        onClick={() => setVisible(false)}
                                        aria-current={isActive ? 'page' : undefined}
                                        className={`sidebar-nav-link${isActive ? ' sidebar-nav-link-active' : ''}`}
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '10px',
                                            textDecoration: 'none',
                                            fontSize: '17px',
                                            padding: '10px 12px',
                                            borderRadius: '10px',
                                            minHeight: '44px',
                                            ...getNavItemStyle(isActive),
                                        }}
                                    >
                                        <Icon data={item.icon} size={20} />
                                        {item.label}
                                    </Link>
                                );
                            })}
                        </nav>

                        <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            {session ? (
                                <Button
                                    view="outlined-danger"
                                    width="max"
                                    onClick={() => signOut({ callbackUrl: '/' })}
                                >
                                    <Icon data={ArrowRightFromSquare} size={16} />
                                    Выйти
                                </Button>
                            ) : null}
                            <Text variant="caption-2" color="secondary" style={{ textAlign: 'center' }}>
                                © MusClub App
                            </Text>
                        </div>
                </div>
                <div
                    className="sidebar-resize-handle"
                    role="presentation"
                    aria-hidden
                    onMouseDown={(event) => {
                        event.preventDefault();
                        setResizeStart({ startX: event.clientX, startWidth: drawerWidth });
                        setIsResizing(true);
                    }}
                    onDoubleClick={() => setDrawerWidth(clampWidth(getDefaultWidth()))}
                />
            </aside>
        </>
    );
}
