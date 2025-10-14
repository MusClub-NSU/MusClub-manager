'use client';

import React from 'react';

const events = [
    { title: 'Тренировка', date: '22.10', time: '19:00', location: 'Главный зал', status: 'Анонс' },
    { title: 'Йога', date: '23.10', time: '18:00', location: 'Зал 2', status: 'Анонс' },
    { title: 'Танцы', date: '24.10', time: '20:00', location: 'Зал 3', status: 'Анонс' },
    // можно добавить еще события
];

export default function EventsPage() {
    return (
        <div style={{
            maxHeight: '80vh', // ограничиваем высоту блока
            overflowY: 'auto', // вертикальная прокрутка
            padding: '20px',
        }}>
            {events.map((event, idx) => (
                <div key={idx} style={{
                    backgroundColor: '#9e8f8a',
                    borderRadius: '8px',
                    padding: '10px 15px',
                    marginBottom: '15px',
                    color: '#fff',
                }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                        <span>{event.title}</span>
                        <span>{event.date} {event.time}</span>
                    </div>
                    <div style={{
                        backgroundColor: '#e0e0e0',
                        color: '#000',
                        padding: '5px 10px',
                        borderRadius: '4px',
                        width: 'fit-content',
                        marginBottom: '5px',
                    }}>
                        {event.location}
                    </div>
                    <div style={{
                        backgroundColor: '#00b894',
                        color: '#fff',
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontSize: '12px',
                        display: 'inline-block',
                    }}>
                        {event.status}
                    </div>
                </div>
            ))}
        </div>
    );
}
