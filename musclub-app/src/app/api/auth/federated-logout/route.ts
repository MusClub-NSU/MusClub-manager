import { getToken } from 'next-auth/jwt';
import { NextRequest, NextResponse } from 'next/server';

function getPublicIssuer() {
    return process.env.KEYCLOAK_ISSUER ?? '';
}

export async function GET(request: NextRequest) {
    const callbackUrlParam = request.nextUrl.searchParams.get('callbackUrl') ?? '/';
    // В docker `request.nextUrl.origin` может быть `http://0.0.0.0:3000`, а Keycloak разрешает только `NEXTAUTH_URL` (обычно `http://localhost:3000`).
    // Поэтому строим origin из `NEXTAUTH_URL`, чтобы `post_logout_redirect_uri` точно совпадал с allowed в realm.
    const nextAuthUrl = process.env.NEXTAUTH_URL;
    const origin = nextAuthUrl ? new URL(nextAuthUrl).origin : request.nextUrl.origin;
    const callbackUrl = new URL(callbackUrlParam, origin).toString();
    const publicIssuer = getPublicIssuer();

    if (!publicIssuer) {
        return NextResponse.json({ logoutUrl: callbackUrl });
    }

    const token = await getToken({
        req: request,
        secret: process.env.NEXTAUTH_SECRET,
    });

    const logoutUrl = new URL(`${publicIssuer}/protocol/openid-connect/logout`);
    logoutUrl.searchParams.set('post_logout_redirect_uri', callbackUrl);
    logoutUrl.searchParams.set('client_id', process.env.KEYCLOAK_CLIENT_ID ?? '');

    if (token?.idToken && typeof token.idToken === 'string') {
        logoutUrl.searchParams.set('id_token_hint', token.idToken);
    }

    return NextResponse.json({ logoutUrl: logoutUrl.toString() });
}
