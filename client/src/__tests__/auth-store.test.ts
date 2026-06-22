import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '@/stores/auth';

describe('auth store', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('stores administrator identity', () => {
    const store = useAuthStore();
    store.setAdmin({ username: 'admin', displayName: 'Admin', authType: 'basic' }, 'admin123456');
    expect(store.isAuthenticated).toBe(true);
    expect(store.basicAuthHeader).toBe('Basic YWRtaW46YWRtaW4xMjM0NTY=');
  });

  it('clears administrator identity on logout', () => {
    const store = useAuthStore();
    store.setAdmin({ username: 'admin', displayName: 'Admin', authType: 'basic' }, 'admin123456');
    store.logout();
    expect(store.isAuthenticated).toBe(false);
    expect(store.admin).toBeNull();
  });

  it('does not keep a default admin password', () => {
    const store = useAuthStore();
    store.setAdmin({ username: 'admin', displayName: 'Admin', authType: 'BASIC' });
    expect(store.basicAuthHeader).toBe('');
    expect(store.canWrite).toBe(false);
  });

  it('allows writes only when admin identity and explicit password are present', () => {
    const store = useAuthStore();
    store.setAdmin({ username: 'admin', displayName: 'Admin', authType: 'BASIC' }, 'secret');
    expect(store.isAdmin).toBe(true);
    expect(store.canWrite).toBe(true);
    expect(store.basicAuthHeader).toBe(`Basic ${btoa('admin:secret')}`);
  });
});
