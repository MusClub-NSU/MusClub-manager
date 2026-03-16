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
  const { data: session } = useSession();
  const email = session?.user?.email;
  const { users, loading } = useUsers({ page: 0, size: 100 });
  const [backendRole, setBackendRole] = useState<string | null>(null);

  useEffect(() => {
    if (!email || !users.length) return;
    const user = users.find((u) => u.email === email);
    if (user) {
      setBackendRole(user.role);
    }
  }, [email, users]);

  const role: AppRole = useMemo(
    () => (session ? mapBackendRoleToAppRole(backendRole) : 'GUEST'),
    [session, backendRole],
  );

  const canViewAllPages = role === 'MEMBER' || role === 'ADMIN' || role === 'SUPERADMIN';
  const canManageEvents = role === 'ADMIN' || role === 'SUPERADMIN';
  const canManageUsers = role === 'SUPERADMIN';

  return { role, loading, canViewAllPages, canManageEvents, canManageUsers };
}

