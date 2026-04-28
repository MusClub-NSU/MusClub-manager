import NextAuth from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';
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
