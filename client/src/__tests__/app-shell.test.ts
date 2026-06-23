import { createPinia, setActivePinia } from 'pinia';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it } from 'vitest';
import AppShell from '@/layout/AppShell.vue';
import { useAuthStore } from '@/stores/auth';

const routerLinkStub = {
  props: ['to'],
  template: '<a class="router-link-stub" :href="typeof to === `string` ? to : to.path"><slot /></a>',
};

describe('AppShell', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('renders public command navigation and readonly state for anonymous users', () => {
    const wrapper = mount(AppShell, {
      slots: { default: '<section data-test="page">页面内容</section>' },
      global: { stubs: { RouterLink: routerLinkStub } },
    });

    expect(wrapper.get('[data-test="app-main"]').attributes('tabindex')).toBe('-1');
    expect(wrapper.get('nav[aria-label="主导航"]').text()).toContain('赛事总览');
    expect(wrapper.text()).toContain('赛前作战');
    expect(wrapper.text()).toContain('证据中心');
    expect(wrapper.text()).toContain('决策复盘');
    expect(wrapper.text()).toContain('公开只读');
    expect(wrapper.text()).not.toContain('JSON 审核中心');
    expect(wrapper.text()).not.toContain('系统设置');
    expect(wrapper.find('[data-test="mobile-tabbar"]').exists()).toBe(true);
  });

  it('shows admin entries and writable status after Basic admin login', () => {
    const auth = useAuthStore();
    auth.setAdmin({ username: 'operator', displayName: 'Operator', authType: 'BASIC' }, 'secret');

    const wrapper = mount(AppShell, {
      slots: { default: '<section>管理内容</section>' },
      global: { stubs: { RouterLink: routerLinkStub } },
    });

    expect(wrapper.text()).toContain('管理员模式');
    expect(wrapper.text()).toContain('JSON 审核中心');
    expect(wrapper.text()).toContain('系统设置');
    expect(wrapper.text()).toContain('Operator');
  });
});
