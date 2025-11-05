'use client';

import React, { createContext, useContext, useState, ReactNode } from 'react';

interface SidebarContextType {
    visible: boolean;
    setVisible: (v: boolean) => void;
    disabled: boolean;
    setDisabled: (v: boolean) => void;
}

const SidebarContext = createContext<SidebarContextType | undefined>(undefined);

export const SidebarProvider = ({ children }: { children: ReactNode }) => {
    const [visible, setVisible] = useState(false);
    const [disabled, setDisabled] = useState(false);

    return (
        <SidebarContext.Provider value={{ visible, setVisible, disabled, setDisabled  }}>
            {children}
        </SidebarContext.Provider>
    );
};

export const useSidebar = () => {
    const ctx = useContext(SidebarContext);
    if (!ctx) {
        throw new Error('useSidebar must be used within SidebarProvider');
    }
    return ctx;
};
