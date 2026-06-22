import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { buildBasicAuthHeader } from '@/api/http';

export interface AdminIdentity {
  username: string;
  displayName: string;
  authType: string;
}

export const useAuthStore = defineStore('auth', () => {
  const admin = ref<AdminIdentity | null>(null);
  const password = ref('');

  const isAuthenticated = computed(() => admin.value !== null);
  const isAdmin = computed(
    () => admin.value?.authType.toUpperCase() === 'BASIC' && admin.value.username === 'admin',
  );
  const canWrite = computed(() => isAdmin.value && Boolean(password.value));
  const basicAuthHeader = computed(() => {
    if (!canWrite.value || !admin.value) {
      return '';
    }
    return buildBasicAuthHeader(admin.value.username, password.value);
  });

  function setAdmin(identity: AdminIdentity, rawPassword = '') {
    admin.value = identity;
    password.value = rawPassword;
  }

  function logout() {
    admin.value = null;
    password.value = '';
  }

  return {
    admin,
    isAuthenticated,
    isAdmin,
    canWrite,
    basicAuthHeader,
    setAdmin,
    logout,
  };
});
