'use client';

import { useEffect, useMemo, useState } from 'react';
import { useSession } from 'next-auth/react';
import { useUsers } from './useApi';

export type AppRole = 'GUEST' | 'MEMBER' | 'ADMIN' | 'SUPERADMIN';

export function mapBackendRoleToAppRole(backendRole?: string | null): AppRole {
  if (!backendRole) return 'MEMBER';

  switch (backendRole.toUpperCase()) {
    case 'ORGANIZER':
    case 'ADMIN':
      return 'ADMIN';
    case 'SUPERADMIN':
    case 'SUPER_ADMIN':
      return 'SUPERADMIN';
    case 'MEMBER':
    default:
      return 'MEMBER';
  }
}

export function useCurrentUserRole() {
  const { data: session, status } = useSession();
  const email = session?.user?.email;
  const { users, loading: usersLoading } = useUsers({ page: 0, size: 100 });
  const [backendRole, setBackendRole] = useState<string | null>(null);
  const [roleResolved, setRoleResolved] = useState(false);

  useEffect(() => {
    if (status === 'unauthenticated') {
      setBackendRole(null);
      setRoleResolved(true);
      return;
    }

    if (!email || usersLoading || !users.length) {
      return;
    }

    const user = users.find((u) => u.email === email);
    
    if (user) {
      setBackendRole(user.role);
      console.log(`[useCurrentUserRole] Найден пользователь: email=${email}, role=${user.role}`);
    } else {
      console.warn(`[useCurrentUserRole] Пользователь с email=${email} НЕ найден в backend БД!`);
      console.log('[useCurrentUserRole] Доступные пользователи:', users.map(u => `${u.email}:${u.role}`));
      setBackendRole(null);
    }
    setRoleResolved(true);
  }, [email, users, usersLoading, status]);

  const role: AppRole = useMemo(
    () => {
      if (!session || !roleResolved) {
        return 'GUEST';
      }
      const mappedRole = mapBackendRoleToAppRole(backendRole);
      console.log(`[useCurrentUserRole] Финальная роль: ${mappedRole} (backendRole=${backendRole})`);
      return mappedRole;
    },
    [session, backendRole, roleResolved],
  );

  const canViewAllPages = role === 'MEMBER' || role === 'ADMIN' || role === 'SUPERADMIN';
  const canManageEvents = role === 'ADMIN' || role === 'SUPERADMIN' || role === 'ORGANIZER';
  const canManageUsers = role === 'SUPERADMIN';

  return { role, loading: !roleResolved || usersLoading, canViewAllPages, canManageEvents, canManageUsers };
}

