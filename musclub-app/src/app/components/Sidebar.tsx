'use client';

import { Drawer, DrawerItem } from '@gravity-ui/navigation';
import { Button, Icon, Text } from '@gravity-ui/uikit';
import { Bars, Persons, Calendar, House, ArrowRightFromSquare } from '@gravity-ui/icons';
import Link from 'next/link';
import '@gravity-ui/uikit/styles/styles.css';
import { useSidebar } from '../context/SidebarContext';
import { useSession, signOut } from 'next-auth/react';

export default function Sidebar() {
    const { visible, setVisible, disabled } = useSidebar();
    const { data: session } = useSession();

    const toggleDrawer = () => {
        if (!disabled) setVisible(!visible);
    };

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
                style={{
                    position: 'fixed',
                    top: 12,
                    left: 12,
                    zIndex: 2000,
                    borderRadius: '8px',
                    opacity: disabled ? 0.5 : 1,
                    pointerEvents: disabled ? 'none' : 'auto',
                }}
            >
                <Icon data={Bars} size={24} />
            </Button>

            <Drawer onVeilClick={() => setVisible(false)} hideVeil>
                <DrawerItem
                    id="main-drawer"
                    visible={visible}
                    direction="left"
                    width={340}
                    style={{
                        zIndex: 1500,
                        boxShadow: '4px 0 12px rgba(0,0,0,0.4)',
                    }}
                >
                    <div
                        style={{
                            padding: '24px',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '16px',
                            height: '100%',
                        }}
                    >
                        <h2 style={{ marginBottom: '8px' }}>MusClub</h2>

                        {/* Информация о пользователе */}
                        {session?.user && (
                            <div style={{
                                padding: '12px',
                                borderRadius: '8px',
                                background: 'rgba(0,0,0,0.05)',
                                marginBottom: '8px',
                            }}>
                                <Text variant="subheader-2">{session.user.name}</Text>
                                <br />
                                <Text color="secondary" variant="caption-2">{session.user.email}</Text>
                            </div>
                        )}

                        <nav
                            style={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: '12px',
                            }}
                        >
                            <Link
                                href="/"
                                onClick={() => setVisible(false)}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '10px',
                                    textDecoration: 'none',
                                    fontSize: '18px',
                                    padding: '8px 12px',
                                    borderRadius: '8px',
                                    transition: 'background 0.2s',
                                }}
                            >
                                <Icon data={House} size={20} />
                                Главная
                            </Link>

                            {session && (
                                <>
                                    <Link
                                        href="/participants"
                                        onClick={() => setVisible(false)}
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '10px',
                                            textDecoration: 'none',
                                            fontSize: '18px',
                                            padding: '8px 12px',
                                            borderRadius: '8px',
                                            transition: 'background 0.2s',
                                        }}
                                    >
                                        <Icon data={Persons} size={20} />
                                        Участники
                                    </Link>

                                    <Link
                                        href="/events"
                                        onClick={() => setVisible(false)}
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '10px',
                                            textDecoration: 'none',
                                            fontSize: '18px',
                                            padding: '8px 12px',
                                            borderRadius: '8px',
                                            transition: 'background 0.2s',
                                        }}
                                    >
                                        <Icon data={Calendar} size={20} />
                                        Мероприятия
                                    </Link>
                                </>
                            )}
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
                </DrawerItem>
            </Drawer>
        </>
    );
}
