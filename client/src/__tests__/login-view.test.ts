import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import LoginView from '@/views/LoginView.vue';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe('LoginView', () => {
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
});
