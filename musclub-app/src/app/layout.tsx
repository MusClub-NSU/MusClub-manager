import type { Metadata } from "next";
import "./globals.css";
import GravityProvider from "./GravityProvider";
import Sidebar from "./components/Sidebar";
import { SidebarProvider } from './context/SidebarContext';
import { PushNotificationInit } from './components/PushNotificationInit';
import AuthProvider from './components/AuthProvider';
import AuthGuard from './components/AuthGuard';

import '@gravity-ui/uikit/styles/fonts.css';
import '@gravity-ui/uikit/styles/styles.css';

export const metadata: Metadata = {
    title: "MusClub App",
    description: "PWA for MusClub",
    manifest: "/manifest.json",
    themeColor: "#000000",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ru">
      <body className="antialiased">
      <AuthProvider>
        <SidebarProvider>
          <GravityProvider>
            <AuthGuard>
              <PushNotificationInit />
              <Sidebar />
              {children}
            </AuthGuard>
          </GravityProvider>
        </SidebarProvider>
      </AuthProvider>
      </body>
    </html>
  );
}
