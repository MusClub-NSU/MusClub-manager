'use client';

import { useState } from 'react';
import { Drawer, DrawerItem } from '@gravity-ui/navigation';
import { Button, Icon } from '@gravity-ui/uikit';
import { Bars, Persons, Calendar } from '@gravity-ui/icons';
import Link from 'next/link';
import '@gravity-ui/uikit/styles/styles.css';

export default function Sidebar() {
    const [visible, setVisible] = useState(false);

    // Функция переключения состояния (открыть/закрыть)
    const toggleDrawer = () => setVisible((prev) => !prev);

    return (
        <>
            {/* Кнопка-иконка меню */}
            <Button
                view="flat"
                onClick={toggleDrawer}
                style={{
                    position: 'fixed',
                    top: 10,
                    left: 10,
                    zIndex: 1000,
                }}
            >
                <Icon data={Bars} size={24} />
            </Button>

            {/* Выдвижная панель */}
            <Drawer onVeilClick={() => setVisible(false)}>
                <DrawerItem
                    id="main-drawer"
                    visible={visible}
                    direction="left"
                    width={340} // 👈 увеличили ширину меню
                >
                    <div
                        style={{
                            padding: '24px',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '16px',
                            height: '100%',
                            backgroundColor: '#202020',
                        }}
                    >
                        <h2 style={{ marginBottom: '16px' }}></h2>

                        {/* Навигационные ссылки */}
                        <nav
                            style={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: '12px',
                            }}
                        >
                            <Link
                                href="/participants"
                                onClick={() => setVisible(false)}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '10px',
                                    textDecoration: 'none',
                                    color: '#bababa',
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
                                    color: '#bababa',
                                    fontSize: '18px',
                                    padding: '8px 12px',
                                    borderRadius: '8px',
                                    transition: 'background 0.2s',
                                }}
                            >
                                <Icon data={Calendar} size={20} />
                                Мероприятия
                            </Link>
                        </nav>

                        <div style={{ marginTop: 'auto', fontSize: '14px', color: '#777' }}>
                            © MusClub App
                        </div>
                    </div>
                </DrawerItem>
            </Drawer>
        </>
    );
}
