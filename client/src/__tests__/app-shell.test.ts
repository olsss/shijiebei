import { createPinia, setActivePinia } from 'pinia';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AppShell from '@/layout/AppShell.vue';
import appShellSource from '@/layout/AppShell.vue?raw';
import readonlyNoticeSource from '@/components/common/ReadonlyNotice.vue?raw';
import { useAuthStore } from '@/stores/auth';

const replaceSpy = vi.fn();
const routeState = {
  fullPath: '/',
  meta: {} as Record<string, unknown>,
};

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ replace: replaceSpy }),
}));

const routerLinkStub = {
  props: ['to'],
  template: '<a class="router-link-stub" :href="typeof to === `string` ? to : to.path"><slot /></a>',
};

describe('AppShell', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    replaceSpy.mockReset();
    routeState.fullPath = '/';
    routeState.meta = {};
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
    expect(wrapper.text()).toContain('全部入口');
    expect(wrapper.find('nav[aria-label="主导航"] a[href="/evidence"]').exists()).toBe(true);
    expect(wrapper.find('nav[aria-label="主导航"] a[href="/more"]').exists()).toBe(true);
    expect(wrapper.find('section[aria-label="全站结构"]').exists()).toBe(false);
    expect(wrapper.text()).not.toMatch(/新手|读法|地图/);
    expect(wrapper.text()).toContain('公开只读');
    expect(wrapper.text()).not.toContain('JSON 审核中心');
    expect(wrapper.text()).not.toContain('系统设置');
    expect(wrapper.find('[data-test="mobile-tabbar"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="mobile-tabbar"] a[href="/more"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="mobile-tabbar"]').text()).toContain('更多');
    expect(wrapper.find('a[href="/login"]').classes()).toContain('shell-button--login');
    expect(appShellSource).toContain('.shell-button--login');
    expect(appShellSource).not.toContain('.shell-button {\n  background: var(--wc-accent);');
    expect(readonlyNoticeSource).toContain('readonly-notice__compact');
    expect(readonlyNoticeSource).toMatch(/@media \(max-width: 767px\)[\s\S]*border-radius:\s*999px/);
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

  it('redirects away from protected admin pages after logout', async () => {
    routeState.fullPath = '/admin/import-review';
    routeState.meta = { requiresAdmin: true };
    const auth = useAuthStore();
    auth.setAdmin({ username: 'operator', displayName: 'Operator', authType: 'BASIC' }, 'secret');

    const wrapper = mount(AppShell, {
      slots: { default: '<section><h1>管理页面</h1></section>' },
      global: { stubs: { RouterLink: routerLinkStub } },
    });

    await wrapper.get('button[type="button"]').trigger('click');

    expect(auth.isAuthenticated).toBe(false);
    expect(replaceSpy).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/admin/import-review' },
    });
  });

  it('keeps one main landmark and one page h1 in the shell', () => {
    const wrapper = mount(AppShell, {
      slots: { default: '<section><h1>赛事总览</h1><p>页面内容</p></section>' },
      global: { stubs: { RouterLink: routerLinkStub } },
    });

    expect(wrapper.findAll('main')).toHaveLength(1);
    expect(wrapper.findAll('h1')).toHaveLength(1);
  });
});
