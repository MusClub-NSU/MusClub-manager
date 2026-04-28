'use client';

import { useSession } from 'next-auth/react';
import { Loader, Text } from '@gravity-ui/uikit';
import { useEffect, useRef } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { logoutFromKeycloak } from '@/lib/auth';

export default function AuthGuard({ children }: { children: React.ReactNode }) {
    const { data: session, status } = useSession();
    const pathname = usePathname();
    const router = useRouter();
    const signOutStarted = useRef(false);

    // Публичные страницы доступны без авторизации
    const isPublicRoute = pathname === '/' || pathname === '/login';

    useEffect(() => {
        if (session?.error !== 'RefreshAccessTokenError') {
            signOutStarted.current = false;
        }
    }, [session?.error]);

    useEffect(() => {
        // Refresh не удался: сессия всё ещё "authenticated", но токен битый.
        // Нельзя только router.replace на /login — страница входа снова уведёт на главную → бесконечный цикл.
        // Сбрасываем локальную сессию и завершаем сессию Keycloak, чтобы повторный вход был предсказуемым.
        if (session?.error === 'RefreshAccessTokenError' && !signOutStarted.current) {
            signOutStarted.current = true;
            void logoutFromKeycloak({
                callbackUrl: `/login?callbackUrl=${encodeURIComponent(pathname || '/')}`,
            });
        }
    }, [session?.error, pathname]);

    // Гость на закрытой странице — сразу на экран входа (без промежуточного экрана с кнопкой)
    useEffect(() => {
        if (status === 'unauthenticated' && !isPublicRoute) {
            router.replace(`/login?callbackUrl=${encodeURIComponent(pathname || '/')}`);
        }
    }, [status, isPublicRoute, pathname, router]);

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
                <div className="flex flex-col items-center gap-4">
                    <Loader size="l" />
                    <Text color="secondary">Переход к авторизации…</Text>
                </div>
            </div>
        );
    }

    return <>{children}</>;
}

