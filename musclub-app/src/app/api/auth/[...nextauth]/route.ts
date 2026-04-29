import NextAuth from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';
import CredentialsProvider from 'next-auth/providers/credentials';
import { JWT } from 'next-auth/jwt';

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

function getRealmRoles(accessToken?: string): string[] {
    if (!accessToken) {
        return [];
    }

    const payload = parseJwtPayload(accessToken);
    const realmAccess = payload.realm_access;
    if (!realmAccess || typeof realmAccess !== 'object' || !('roles' in realmAccess)) {
        return [];
    }

    const roles = (realmAccess as { roles?: unknown }).roles;
    if (!Array.isArray(roles)) {
        return [];
    }

    return roles.filter((role): role is string => typeof role === 'string');
}

function getPublicIssuer(): string {
    return process.env.KEYCLOAK_ISSUER ?? '';
}

function getInternalIssuer(): string {
    return process.env.KEYCLOAK_INTERNAL_ISSUER ?? getPublicIssuer();
}

/**
 * Обновляет access token через Keycloak refresh token.
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
    try {
        const url = `${getInternalIssuer()}/protocol/openid-connect/token`;
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
            roles: getRealmRoles(refreshed.access_token),
            refreshToken: refreshed.refresh_token ?? token.refreshToken,
        };
    } catch {
        return { ...token, error: 'RefreshAccessTokenError' };
    }
}

type TokenResponse = {
    access_token: string;
    refresh_token?: string;
    id_token?: string;
    expires_in: number;
    scope?: string;
};

/**
 * Логин по логин+пароль через password grant в Keycloak.
 * Возвращает OIDC-токены, которые далее кладём в JWT NextAuth.
 */
async function passwordGrantToKeycloak(username: string, password: string): Promise<TokenResponse> {
    const tokenUrl = `${getInternalIssuer()}/protocol/openid-connect/token`;

    const body = new URLSearchParams({
        client_id: process.env.KEYCLOAK_CLIENT_ID!,
        grant_type: 'password',
        username,
        password,
        scope: 'openid profile email',
    });

    if (process.env.KEYCLOAK_CLIENT_SECRET) {
        body.set('client_secret', process.env.KEYCLOAK_CLIENT_SECRET);
    }

    const response = await fetch(tokenUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body,
    });

    const json = (await response.json().catch(() => ({}))) as Partial<TokenResponse> & {
        error?: string;
        error_description?: string;
    };

    if (!response.ok || !json.access_token || !json.expires_in) {
        // Для UI дальше вернём просто null (CredentialsSignin).
        throw new Error(json.error_description || json.error || 'Keycloak token request failed');
    }

    return json as TokenResponse;
}

const handler = NextAuth({
    providers: [
        KeycloakProvider({
            clientId: process.env.KEYCLOAK_CLIENT_ID ?? '',
            clientSecret: process.env.KEYCLOAK_CLIENT_SECRET ?? '',
            issuer: getPublicIssuer(),
            // In containers, NextAuth server cannot resolve localhost:8180.
            // Use internal discovery URL, but keep public issuer for browser redirects/token validation.
            wellKnown: `${getInternalIssuer()}/.well-known/openid-configuration`,
            authorization: {
                url: `${getPublicIssuer()}/protocol/openid-connect/auth`,
                params: { scope: 'openid profile email' },
            },
            token: `${getInternalIssuer()}/protocol/openid-connect/token`,
            userinfo: `${getInternalIssuer()}/protocol/openid-connect/userinfo`,
            client: process.env.KEYCLOAK_CLIENT_SECRET
                ? undefined
                : { token_endpoint_auth_method: 'none' },
        }),
        CredentialsProvider({
            name: 'Keycloak (password)',
            credentials: {
                username: { label: 'Логин', type: 'text' },
                password: { label: 'Пароль', type: 'password' },
            },
            async authorize(credentials, _req) {
                try {
                    const username = credentials?.username;
                    const password = credentials?.password;

                    if (!username || !password) return null;

                    const tokens = await passwordGrantToKeycloak(username, password);

                    const payload = parseJwtPayload(tokens.access_token);
                    const idPayload = tokens.id_token ? parseJwtPayload(tokens.id_token) : {};
                    const userId = typeof payload.sub === 'string' ? payload.sub : username;

                    return {
                        id: userId,
                        sub: userId,
                        accessToken: tokens.access_token,
                        refreshToken: tokens.refresh_token,
                        idToken: tokens.id_token,
                        accessTokenExpires: Date.now() + tokens.expires_in * 1000,
                        roles: getRealmRoles(tokens.access_token),
                        preferred_username:
                            (typeof payload.preferred_username === 'string' ? payload.preferred_username : null) ??
                            (typeof idPayload.preferred_username === 'string' ? idPayload.preferred_username : null) ??
                            undefined,
                        name:
                            (typeof idPayload.name === 'string' ? idPayload.name : null) ??
                            (typeof idPayload.preferred_username === 'string' ? idPayload.preferred_username : null) ??
                            undefined,
                        email: typeof idPayload.email === 'string' ? idPayload.email : undefined,
                    };
                } catch {
                    return null;
                }
            },
        }),
    ],
    secret: process.env.NEXTAUTH_SECRET,
    pages: {
        signIn: '/login',
    },
    callbacks: {
        async jwt({ token, account, user }) {
            if (account?.provider === 'keycloak' && account.access_token) {
                const payload = parseJwtPayload(account.access_token);
                return {
                    ...token,
                    accessToken: account.access_token,
                    idToken: account.id_token,
                    refreshToken: account.refresh_token,
                    accessTokenExpires: (account.expires_at ?? 0) * 1000,
                    roles: getRealmRoles(account.access_token),
                    sub: user?.id ?? token.sub,
                    name: user?.name ?? token.name,
                    email: user?.email ?? token.email,
                    preferred_username:
                        (typeof payload.preferred_username === 'string' ? payload.preferred_username : null) ??
                        user?.name ??
                        token.preferred_username,
                };
            }

            if (account?.provider === 'credentials' && user) {
                const credentialsUser = user as unknown as {
                    sub?: string;
                    accessToken?: string;
                    refreshToken?: string;
                    idToken?: string;
                    accessTokenExpires?: number;
                    roles?: string[];
                    preferred_username?: string;
                    name?: string;
                    email?: string;
                };

                if (credentialsUser.accessToken) {
                    return {
                        ...token,
                        accessToken: credentialsUser.accessToken,
                        idToken: credentialsUser.idToken,
                        refreshToken: credentialsUser.refreshToken,
                        accessTokenExpires: credentialsUser.accessTokenExpires,
                        roles: credentialsUser.roles,
                        sub: credentialsUser.sub ?? token.sub,
                        name: credentialsUser.name ?? token.name,
                        email: credentialsUser.email ?? token.email,
                        preferred_username:
                            credentialsUser.preferred_username ?? token.preferred_username,
                    };
                }
            }

            // Токен ещё действителен
            if (token.accessToken && Date.now() < (token.accessTokenExpires as number)) {
                return token;
            }

            // Токен истёк — обновляем
            return refreshAccessToken(token);
        },
        async session({ session, token }) {
            session.accessToken = token.accessToken as string;
            session.error = token.error as string | undefined;
            session.user.roles = (token.roles as string[] | undefined) ?? [];
            session.user.username = token.preferred_username as string | undefined;
            session.user.profileId = token.profileId as number | undefined;
            session.user.backendRole = token.backendRole as string | undefined;
            if (session.user) {
                session.user.name = (token.name as string) || session.user.name;
                session.user.email = (token.email as string) || session.user.email;
            }
            return session;
        },
    },
});

export { handler as GET, handler as POST };
