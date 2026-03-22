'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { signIn, useSession } from 'next-auth/react';
import { Button, Card, Loader, Text } from '@gravity-ui/uikit';

export default function LoginPage() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const { status } = useSession();

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const callbackUrl = searchParams.get('callbackUrl') || '/';

    // Переносим навигацию в useEffect, чтобы не вызывать router во время render
    useEffect(() => {
        if (status === 'authenticated') {
            router.replace(callbackUrl);
        }
    }, [status, callbackUrl, router]);

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError(null);
        setIsSubmitting(true);

        const result = await signIn('credentials', {
            redirect: false,
            username,
            password,
            callbackUrl,
        });

        setIsSubmitting(false);

        if (!result || result.error) {
            setError('Неверный логин или пароль');
            return;
        }

        router.push(result.url || callbackUrl);
    };

    // Во время проверки сессии показываем загрузку
    if (status === 'loading') {
        return (
            <main className="flex min-h-screen items-center justify-center p-4">
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text>Проверка сессии...</Text>
                </div>
            </main>
        );
    }

    // Если уже аутентифицирован, редиректится через useEffect выше
    if (status === 'authenticated') {
        return (
            <main className="flex min-h-screen items-center justify-center p-4">
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text>Перенаправляем...</Text>
                </div>
            </main>
        );
    }

    return (
        <main className="flex min-h-screen items-center justify-center p-4">
            <Card className="w-full max-w-md p-6">
                <h1 className="mb-2 text-2xl font-bold">Вход в MusClub Manager</h1>
                <Text color="secondary">Введите логин и пароль</Text>

                <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit}>
                    <label className="flex flex-col gap-1">
                        <span className="text-sm">Логин</span>
                        <input
                            type="text"
                            autoComplete="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="w-full rounded-md border border-[--g-color-line-generic] bg-[--g-color-base-background] px-3 py-2"
                            required
                        />
                    </label>

                    <label className="flex flex-col gap-1">
                        <span className="text-sm">Пароль</span>
                        <input
                            type="password"
                            autoComplete="current-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full rounded-md border border-[--g-color-line-generic] bg-[--g-color-base-background] px-3 py-2"
                            required
                        />
                    </label>

                    {error && (
                        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
                            {error}
                        </div>
                    )}

                    <Button type="submit" view="action" size="l" disabled={isSubmitting}>
                        {isSubmitting ? (
                            <span className="flex items-center gap-2">
                                <Loader size="s" />
                                Входим...
                            </span>
                        ) : (
                            'Войти'
                        )}
                    </Button>
                </form>
            </Card>
        </main>
    );
}

