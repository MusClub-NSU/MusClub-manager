'use client';

import { useSession } from 'next-auth/react';
import { Loader, Text, Button } from '@gravity-ui/uikit';
import { useEffect } from 'react';
import { usePathname, useRouter } from 'next/navigation';

export default function AuthGuard({ children }: { children: React.ReactNode }) {
    const { data: session, status } = useSession();
    const pathname = usePathname();
    const router = useRouter();

    useEffect(() => {
        // Если токен истёк с ошибкой — перелогиниваем
        if (session?.error === 'RefreshAccessTokenError') {
            router.push(`/login?callbackUrl=${encodeURIComponent(pathname || '/')}`);
        }
    }, [session, pathname, router]);

    // Публичные страницы доступны без авторизации
    const isPublicRoute = pathname === '/' || pathname === '/login';

    if (status === 'loading') {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text>Проверка авторизации...</Text>
                </div>
            </div>
        );
    }

    if (status === 'unauthenticated' && !isPublicRoute) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="flex flex-col items-center gap-6 text-center p-8">
                    <h1 className="text-3xl font-bold">MusClub Manager</h1>
                    <Text color="secondary">Для доступа к приложению необходимо войти в систему</Text>
                    <Button
                        view="action"
                        size="l"
                        onClick={() => router.push(`/login?callbackUrl=${encodeURIComponent(pathname || '/')}`)}
                    >
                        Войти
                    </Button>
                </div>
            </div>
        );
    }

    return <>{children}</>;
}

