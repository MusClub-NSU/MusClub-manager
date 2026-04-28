'use client';

import { signOut } from 'next-auth/react';

type LogoutOptions = {
    callbackUrl?: string;
};

export async function logoutFromKeycloak(options: LogoutOptions = {}) {
    const callbackUrl = options.callbackUrl ?? '/';

    try {
        const response = await fetch(
            `/api/auth/federated-logout?callbackUrl=${encodeURIComponent(callbackUrl)}`,
        );

        const payload = (await response.json()) as { logoutUrl?: string };

        await signOut({ redirect: false });

        if (payload.logoutUrl) {
            window.location.assign(payload.logoutUrl);
            return;
        }
    } catch {
        // Fallback below clears the local NextAuth session even if logout URL resolution fails.
    }

    await signOut({ callbackUrl });
}
