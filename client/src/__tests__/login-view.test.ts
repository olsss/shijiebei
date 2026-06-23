import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LoginView from '@/views/LoginView.vue';
import { login } from '@/api/system';

const pushSpy = vi.fn();
const routeState = {
  query: {} as Record<string, unknown>,
};

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushSpy }),
  useRoute: () => routeState,
}));

vi.mock('@/api/system', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/system')>();
  return {
    ...actual,
    login: vi.fn(),
  };
});

describe('LoginView', () => {
  beforeEach(() => {
    pushSpy.mockReset();
    routeState.query = {};
    vi.mocked(login).mockReset();
  });

  it('does not prefill default administrator credentials', () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus, createPinia()],
      },
    });

    const usernameInput = wrapper.find<HTMLInputElement>('input[autocomplete="username"]');
    const passwordInput = wrapper.find<HTMLInputElement>('input[autocomplete="current-password"]');

    expect(usernameInput.exists()).toBe(true);
    expect(passwordInput.exists()).toBe(true);
    expect(usernameInput.element.value).toBe('');
    expect(passwordInput.element.value).toBe('');
    expect(passwordInput.element.value).not.toContain('admin123456');
  });

  it('returns to a safe redirect target after administrator login', async () => {
    routeState.query = { redirect: '/admin/settings' };
    vi.mocked(login).mockResolvedValueOnce({
      success: true,
      data: { username: 'operator', displayName: 'Operator', authType: 'BASIC' },
      message: '',
      timestamp: '',
    });
    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus, createPinia()],
      },
    });

    await wrapper.find<HTMLInputElement>('input[autocomplete="username"]').setValue('operator');
    await wrapper.find<HTMLInputElement>('input[autocomplete="current-password"]').setValue('secret');
    await wrapper.get('.login-button').trigger('click');

    expect(pushSpy).toHaveBeenCalledWith('/admin/settings');
  });

  it('falls back to overview when redirect target is external', async () => {
    routeState.query = { redirect: 'https://evil.example/admin' };
    vi.mocked(login).mockResolvedValueOnce({
      success: true,
      data: { username: 'operator', displayName: 'Operator', authType: 'BASIC' },
      message: '',
      timestamp: '',
    });
    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus, createPinia()],
      },
    });

    await wrapper.find<HTMLInputElement>('input[autocomplete="username"]').setValue('operator');
    await wrapper.find<HTMLInputElement>('input[autocomplete="current-password"]').setValue('secret');
    await wrapper.get('.login-button').trigger('click');

    expect(pushSpy).toHaveBeenCalledWith('/');
  });
});
