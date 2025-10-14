'use client';

import { ThemeProvider } from '@gravity-ui/uikit';
import '@gravity-ui/uikit/styles/fonts.css';
import '@gravity-ui/uikit/styles/styles.css';

export default function GravityProvider({
                                            children,
                                        }: {
    children: React.ReactNode;
}) {
    return <ThemeProvider theme="system">{children}</ThemeProvider>;
}
