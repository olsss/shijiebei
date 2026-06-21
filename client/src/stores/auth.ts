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
  const basicAuthHeader = computed(() => {
    if (!admin.value || !password.value) {
      return '';
    }
    return buildBasicAuthHeader(admin.value.username, password.value);
  });

  function setAdmin(identity: AdminIdentity, rawPassword = 'admin123456') {
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
    basicAuthHeader,
    setAdmin,
    logout,
  };
});
