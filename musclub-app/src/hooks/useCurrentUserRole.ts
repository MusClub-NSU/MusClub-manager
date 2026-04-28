'use client';

import { useEffect, useMemo, useState } from 'react';
import { useSession } from 'next-auth/react';
import { apiClient } from '@/lib/api';
import { User } from '@/types/api';

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

function mapRealmRolesToAppRole(realmRoles?: string[] | null): AppRole {
  if (!realmRoles?.length) {
    return 'GUEST';
  }

  const normalizedRoles = realmRoles.map((role) => role.toUpperCase());
  if (normalizedRoles.includes('SUPERADMIN') || normalizedRoles.includes('SUPER_ADMIN')) {
    return 'SUPERADMIN';
  }
  if (normalizedRoles.includes('ORGANIZER') || normalizedRoles.includes('ADMIN')) {
    return 'ADMIN';
  }
  if (normalizedRoles.includes('MEMBER')) {
    return 'MEMBER';
  }
  return 'MEMBER';
}

export function useCurrentUserRole() {
  const { data: session, status } = useSession();
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [currentUserLoading, setCurrentUserLoading] = useState(false);

  useEffect(() => {
    if (status === 'unauthenticated') {
      setCurrentUser(null);
      return;
    }

    if (status !== 'authenticated') {
      return;
    }

    let cancelled = false;

    const fetchCurrentUser = async () => {
      setCurrentUserLoading(true);

      try {
        const user = await apiClient.getCurrentUser();
        if (!cancelled) {
          setCurrentUser(user);
        }
      } catch {
        if (!cancelled) {
          setCurrentUser(null);
        }
      } finally {
        if (!cancelled) {
          setCurrentUserLoading(false);
        }
      }
    };

    void fetchCurrentUser();

    return () => {
      cancelled = true;
    };
  }, [status, session?.accessToken]);

  const tokenRole = useMemo(
    () => mapRealmRolesToAppRole(session?.user?.roles),
    [session?.user?.roles],
  );
  const backendRole = currentUser?.role ?? session?.user?.backendRole ?? null;
  const mappedBackendRole = useMemo(
    () => mapBackendRoleToAppRole(backendRole),
    [backendRole],
  );
  const role: AppRole = tokenRole === 'SUPERADMIN' ? 'SUPERADMIN' : tokenRole;

  const canViewAllPages = status === 'authenticated';
  const canManageEvents = role === 'ADMIN' || role === 'SUPERADMIN';
  const canManageUsers = role === 'SUPERADMIN' || mappedBackendRole === 'SUPERADMIN';

  return {
    role,
    loading: status === 'loading' || currentUserLoading,
    currentUser,
    backendRole,
    canViewAllPages,
    canManageEvents,
    canManageUsers,
  };
}

