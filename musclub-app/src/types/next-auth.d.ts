import 'next-auth';
import 'next-auth/jwt';

declare module 'next-auth' {
    interface User {
        username?: string | null;
        roles?: string[];
        backendRole?: string;
        profileId?: number;
    }

    interface Session {
        accessToken?: string;
        error?: string;
        user: {
            name?: string | null;
            email?: string | null;
            username?: string | null;
            roles?: string[];
            backendRole?: string;
            profileId?: number;
        };
    }
}

declare module 'next-auth/jwt' {
    interface JWT {
        accessToken?: string;
        refreshToken?: string;
        idToken?: string;
        accessTokenExpires?: number;
        error?: string;
        roles?: string[];
        preferred_username?: string;
        backendRole?: string;
        profileId?: number;
    }
}
