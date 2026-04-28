'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { signIn, useSession } from 'next-auth/react';
import { Button, Card, Loader, Text } from '@gravity-ui/uikit';
import { logoutFromKeycloak } from '@/lib/auth';

function resolveLoginError(error: string | null): string | null {
    switch (error) {
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

    const handleLogin = async () => {
        setIsSubmitting(true);
        await signIn('keycloak', { callbackUrl });
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
                <Text color="secondary">
                    Вход выполняется через Keycloak. После авторизации вы автоматически вернётесь в приложение.
                </Text>

                <div className="mt-6 flex flex-col gap-4">
                    {loginError && (
                        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
                            {loginError}
                        </div>
                    )}

                    <Button type="button" view="action" size="l" disabled={isSubmitting} onClick={handleLogin}>
                        {isSubmitting ? (
                            <span className="flex items-center gap-2">
                                <Loader size="s" />
                                Перенаправляем в Keycloak...
                            </span>
                        ) : (
                            'Войти через Keycloak'
                        )}
                    </Button>

                    <Text variant="body-2" color="secondary">
                        Если сессия уже активна в Keycloak, вход выполнится без повторного ввода пароля.
                    </Text>
                </div>
            </Card>
        </main>
    );
}

