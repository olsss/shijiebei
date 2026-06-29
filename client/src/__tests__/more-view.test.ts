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

    expect(wrapper.find('[data-test="more-beginner-hub"]').exists()).toBe(false);
    expect(wrapper.find('[data-test="more-route-map"]').exists()).toBe(false);
    expect(wrapper.find('[data-test="more-entry-structure"]').exists()).toBe(true);
    expect(wrapper.findAll('[data-test="more-entry-structure"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.text()).toContain('全部功能入口');
    expect(wrapper.text()).toContain('公开功能覆盖');
    expect(wrapper.text()).toContain('公开入口');
    expect(wrapper.text()).toContain('证据子页');
    expect(wrapper.text()).toContain('比赛上下文');
    expect(wrapper.text()).toContain('球队球员');
    expect(wrapper.text()).toContain('管理入口');
    expect(wrapper.text()).toContain('访客状态下隐藏');
    expect(wrapper.text()).toContain('对阵、国旗、比分');
    expect(wrapper.text()).not.toMatch(/新手|读法|阅读地图|路线进度/);
    expect(wrapper.text()).toContain('市场快照');
    expect(wrapper.text()).toContain('资料准备度');
    expect(wrapper.text()).not.toMatch(/盘口|资料健壮度|健壮度条/);
    expect(wrapper.find('a[href="/"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/workbench"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/decisions"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/matches"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/odds"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/sentiment"]').exists()).toBe(true);
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
    expect(wrapper.findAll('[data-test="more-entry-structure"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.text()).toContain('3 项管理功能已显示');
  });
});
