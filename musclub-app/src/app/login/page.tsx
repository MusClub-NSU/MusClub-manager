'use client';

import { useState, useEffect, type FormEvent } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { signIn, useSession } from 'next-auth/react';
import { Button, Card, Loader, Text } from '@gravity-ui/uikit';
import { logoutFromKeycloak } from '@/lib/auth';

function resolveLoginError(error: string | null): string | null {
    switch (error) {
        case 'CredentialsSignin':
            return 'Неверный логин или пароль.';
        case 'OAuthSignin':
        case 'OAuthCallback':
        case 'OAuthCreateAccount':
            return 'Не удалось завершить вход через Keycloak. Попробуйте ещё раз.';
        case 'AccessDenied':
            return 'Keycloak отклонил вход для этого пользователя.';
        case 'RefreshAccessTokenError':
            return 'Сессия истекла. Войдите ещё раз.';
        default:
            return null;
    }
}

export default function LoginPage() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const { status, data: session } = useSession();

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);

    const callbackUrl = searchParams.get('callbackUrl') || '/';
    const loginError = resolveLoginError(searchParams.get('error'));

    // Переносим навигацию в useEffect, чтобы не вызывать router во время render
    useEffect(() => {
        if (status !== 'authenticated') return;
        // Иначе цикл: login → главная → снова login при битом refresh
        if (session?.error === 'RefreshAccessTokenError') {
            void logoutFromKeycloak({ callbackUrl: '/login' });
            return;
        }
        router.replace(callbackUrl);
    }, [status, session?.error, callbackUrl, router]);

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);
        setFormError(null);

        const res = await signIn('credentials', {
            username,
            password,
            redirect: false,
        });

        setIsSubmitting(false);

        if (res?.error) {
            // У CredentialsProvider ошибки обычно приходят как CredentialsSignin.
            setFormError(resolveLoginError(res.error) ?? 'Не удалось войти. Попробуйте ещё раз.');
        }
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
                <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
                    {loginError && (
                        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
                            {loginError}
                        </div>
                    )}
                    {formError && (
                        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
                            {formError}
                        </div>
                    )}

                    <div>
                        <label className="block text-sm font-medium mb-2">Логин</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium mb-2">Пароль</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full px-4 py-2 border border-[--foreground]/20 rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-blue-500"
                            autoComplete="current-password"
                            required
                        />
                    </div>

                    <Button type="submit" view="action" size="l" disabled={isSubmitting}>
                        {isSubmitting ? (
                            <span className="flex items-center gap-2">
                                <Loader size="s" />
                                Выполняем вход...
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

