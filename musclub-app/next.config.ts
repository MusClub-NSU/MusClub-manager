// next.config.ts

// eslint-disable-next-line @typescript-eslint/no-require-imports
const withPWA = require("next-pwa")({
    dest: "public",
    register: true,
    skipWaiting: true,
});

import type { NextConfig } from "next";

const backendInternalUrl = process.env.BACKEND_INTERNAL_URL || "http://localhost:8080";

const nextConfig: NextConfig = {
    reactStrictMode: true,
    output: "standalone",
    async rewrites() {
        return [
            {
                // Маршруты NextAuth обрабатываются самим Next.js, не проксируем их на backend
                source: "/api/auth/:path*",
                destination: "/api/auth/:path*",
            },
            {
                // Остальной backend уходит на Spring Boot
                source: "/api/:path*",
                destination: `${backendInternalUrl}/api/:path*`,
            },
        ];
    },
};

export default withPWA(nextConfig);