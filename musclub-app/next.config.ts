// next.config.ts

// eslint-disable-next-line @typescript-eslint/no-require-imports
const withPWA = require("next-pwa")({
    dest: "public",
    register: true,
    skipWaiting: true,
});

import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    reactStrictMode: true,
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
                destination: "http://localhost:8080/api/:path*",
            },
        ];
    },
};

export default withPWA(nextConfig);