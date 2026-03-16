import NextAuth from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';
import { JWT } from 'next-auth/jwt';

/**
 * Обновляет access token через Keycloak refresh token.
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
    try {
        const url = `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`;
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                client_id: process.env.KEYCLOAK_CLIENT_ID!,
                grant_type: 'refresh_token',
                refresh_token: token.refreshToken as string,
            }),
        });

        const refreshed = await response.json();
        if (!response.ok) throw refreshed;

        return {
            ...token,
            accessToken: refreshed.access_token,
            accessTokenExpires: Date.now() + refreshed.expires_in * 1000,
            refreshToken: refreshed.refresh_token ?? token.refreshToken,
        };
    } catch {
        return { ...token, error: 'RefreshAccessTokenError' };
    }
}

const handler = NextAuth({
    providers: [
        KeycloakProvider({
            clientId: process.env.KEYCLOAK_CLIENT_ID!,
            clientSecret: process.env.KEYCLOAK_CLIENT_SECRET ?? '',
            issuer: process.env.KEYCLOAK_ISSUER,
            // Всегда показывать форму логина, даже если есть активная SSO-сессия
            authorization: {
                params: {
                    prompt: 'login',
                },
            },
        }),
    ],
    callbacks: {
        async jwt({ token, account }) {
            // Первый вход — сохраняем токены из Keycloak
            if (account) {
                return {
                    ...token,
                    accessToken: account.access_token,
                    refreshToken: account.refresh_token,
                    accessTokenExpires: account.expires_at
                        ? account.expires_at * 1000
                        : Date.now() + 3600 * 1000,
                };
            }

            // Токен ещё действителен
            if (Date.now() < (token.accessTokenExpires as number)) {
                return token;
            }

            // Токен истёк — обновляем
            return refreshAccessToken(token);
        },
        async session({ session, token }) {
            session.accessToken = token.accessToken as string;
            session.error = token.error as string | undefined;
            return session;
        },
    },
});

export { handler as GET, handler as POST };
