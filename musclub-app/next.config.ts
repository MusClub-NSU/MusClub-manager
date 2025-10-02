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
};

export default withPWA(nextConfig);