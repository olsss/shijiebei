import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '@/stores/auth';

describe('auth store', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('stores administrator identity', () => {
    const store = useAuthStore();
    store.setAdmin({ username: 'admin', displayName: 'Admin', authType: 'basic' }, 'admin123456');
    expect(store.isAuthenticated).toBe(true);
    expect(store.canWrite).toBe(true);
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

  it('allows writes for configured basic administrator usernames', () => {
    const store = useAuthStore();
    store.setAdmin({ username: 'operator', displayName: 'Operator', authType: 'BASIC' }, 'secret');
    expect(store.isAdmin).toBe(true);
    expect(store.canWrite).toBe(true);
    expect(store.basicAuthHeader).toBe(`Basic ${btoa('operator:secret')}`);
  });

  it('blocks writes for non-basic auth types even with an explicit password', () => {
    const store = useAuthStore();
    store.setAdmin({ username: 'admin', displayName: 'Admin', authType: 'TOKEN' }, 'secret');
    expect(store.isAdmin).toBe(false);
    expect(store.canWrite).toBe(false);
    expect(store.basicAuthHeader).toBe('');
  });
});
