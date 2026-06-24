import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import MoreView from '@/views/MoreView.vue';
import { useAuthStore } from '@/stores/auth';

const routerLinkStub = {
  props: ['to'],
  template: '<a class="router-link-stub" :href="typeof to === `string` ? to : to.path"><slot /></a>',
};

describe('MoreView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('acts as the H5 more gateway without exposing admin pages to anonymous visitors', () => {
    const wrapper = mount(MoreView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    });

    expect(wrapper.find('a[href="/evidence/teams"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/players"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/admin/import-review"]').exists()).toBe(false);
    expect(wrapper.find('a[href="/admin/settings"]').exists()).toBe(false);
    expect(wrapper.text()).toContain('管理员登录');
  });

  it('shows admin gateways only for writable Basic admins', () => {
    const auth = useAuthStore();
    auth.setAdmin({ username: 'operator', displayName: 'Operator', authType: 'BASIC' }, 'secret');

    const wrapper = mount(MoreView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    });

    expect(wrapper.find('a[href="/admin/import-review"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/admin/settings"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('数据审核中心');
  });
});
