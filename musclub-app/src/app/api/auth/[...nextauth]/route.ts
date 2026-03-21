import NextAuth from 'next-auth';
import CredentialsProvider from 'next-auth/providers/credentials';
import { JWT } from 'next-auth/jwt';

type KeycloakTokenResponse = {
    access_token: string;
    refresh_token?: string;
    expires_in: number;
};

type KeycloakUserInfo = {
    sub?: string;
    email?: string;
    name?: string;
    preferred_username?: string;
};

type AuthUser = {
    id: string;
    name?: string | null;
    email?: string | null;
    username?: string | null;
    accessToken: string;
    refreshToken?: string;
    accessTokenExpires: number;
};

function parseJwtPayload(token: string): Record<string, unknown> {
    const parts = token.split('.');
    if (parts.length < 2) {
        return {};
    }

    const normalized = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    const decoded = Buffer.from(padded, 'base64').toString('utf8');
    return JSON.parse(decoded) as Record<string, unknown>;
}

async function requestTokenByPassword(username: string, password: string): Promise<KeycloakTokenResponse | null> {
    const issuer = process.env.KEYCLOAK_ISSUER;
    const clientId = process.env.KEYCLOAK_CLIENT_ID;

    if (!issuer || !clientId) {
        return null;
    }

    const url = `${issuer}/protocol/openid-connect/token`;
    const body = new URLSearchParams({
        grant_type: 'password',
        client_id: clientId,
        username,
        password,
    });

    if (process.env.KEYCLOAK_CLIENT_SECRET) {
        body.set('client_secret', process.env.KEYCLOAK_CLIENT_SECRET);
    }

    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body,
    });

    if (!response.ok) {
        return null;
    }

    return (await response.json()) as KeycloakTokenResponse;
}

async function requestUserInfo(accessToken: string): Promise<KeycloakUserInfo | null> {
    const issuer = process.env.KEYCLOAK_ISSUER;
    if (!issuer) {
        return null;
    }

    const response = await fetch(`${issuer}/protocol/openid-connect/userinfo`, {
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        return null;
    }

    return (await response.json()) as KeycloakUserInfo;
}

/**
 * Обновляет access token через Keycloak refresh token.
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
    try {
        const url = `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`;
        const body = new URLSearchParams({
            client_id: process.env.KEYCLOAK_CLIENT_ID!,
            grant_type: 'refresh_token',
            refresh_token: token.refreshToken as string,
        });

        if (process.env.KEYCLOAK_CLIENT_SECRET) {
            body.set('client_secret', process.env.KEYCLOAK_CLIENT_SECRET);
        }

        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body,
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
        CredentialsProvider({
            name: 'Credentials',
            credentials: {
                username: { label: 'Username', type: 'text' },
                password: { label: 'Password', type: 'password' },
            },
            async authorize(credentials) {
                const username = credentials?.username?.trim();
                const password = credentials?.password;

                if (!username || !password) {
                    return null;
                }

                const tokenResponse = await requestTokenByPassword(username, password);
                if (!tokenResponse?.access_token) {
                    return null;
                }

                const userInfo = await requestUserInfo(tokenResponse.access_token);
                const payload = parseJwtPayload(tokenResponse.access_token);

                const id = String(
                    userInfo?.sub ??
                    payload.sub ??
                    username
                );

                const name =
                    userInfo?.name ??
                    (typeof payload.name === 'string' ? payload.name : null) ??
                    username;

                const email =
                    userInfo?.email ??
                    (typeof payload.email === 'string' ? payload.email : null) ??
                    null;

                const resolvedUsername =
                    userInfo?.preferred_username ??
                    (typeof payload.preferred_username === 'string' ? payload.preferred_username : null) ??
                    username;

                const user: AuthUser = {
                    id,
                    name,
                    email,
                    username: resolvedUsername,
                    accessToken: tokenResponse.access_token,
                    refreshToken: tokenResponse.refresh_token,
                    accessTokenExpires: Date.now() + tokenResponse.expires_in * 1000,
                };

                return user as never;
            },
        }),
    ],
    pages: {
        signIn: '/login',
    },
    callbacks: {
        async jwt({ token, account, user }) {
            // Первый вход через Credentials
            if (account?.provider === 'credentials' && user) {
                const authUser = user as unknown as AuthUser;
                return {
                    ...token,
                    accessToken: authUser.accessToken,
                    refreshToken: authUser.refreshToken,
                    accessTokenExpires: authUser.accessTokenExpires,
                    sub: authUser.id,
                    name: authUser.name,
                    email: authUser.email,
                    preferred_username: authUser.username,
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
            if (session.user) {
                session.user.name = (token.name as string) || session.user.name;
                session.user.email = (token.email as string) || session.user.email;
            }
            return session;
        },
    },
});

export { handler as GET, handler as POST };
