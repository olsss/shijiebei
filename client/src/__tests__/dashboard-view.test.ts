import ElementPlus from 'element-plus';
import { createPinia, setActivePinia } from 'pinia';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DashboardView from '@/views/DashboardView.vue';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/api/coreData', () => ({
  fetchCoreDataOverview: vi.fn().mockResolvedValue({
    success: true,
    data: {
      teams: 0,
      players: 0,
      matches: 0,
      analysisReports: 0,
      bets: 0,
      oddsSnapshots: 0,
      evidence: 0,
      mappings: 0,
    },
    message: '',
    timestamp: '',
  }),
}));

const routerLinkStub = {
  props: ['to'],
  template: '<a class="router-link-stub" :href="typeof to === `string` ? to : to.path"><slot /></a>',
};

function mountDashboard() {
  return mount(DashboardView, {
    global: {
      plugins: [ElementPlus],
      stubs: { RouterLink: routerLinkStub },
    },
  });
}

describe('DashboardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('does not show direct admin review links to anonymous visitors', () => {
    const wrapper = mountDashboard();

    expect(wrapper.find('a[href="/import-review"]').exists()).toBe(false);
    expect(wrapper.find('a[href="/admin/import-review"]').exists()).toBe(false);
  });

  it('renders readable Chinese labels for public modules', () => {
    const wrapper = mountDashboard();

    const pageText = wrapper.text();

    expect(pageText).not.toContain('????');
    expect(pageText).toContain('比赛中心');
    expect(pageText).toContain('赔率中心');
    expect(pageText).toContain('舆情与外部因素中心');
    expect(pageText).toContain('更多入口');
  });

  it('shows the JSON review entry only for writable admins', () => {
    const auth = useAuthStore();
    auth.setAdmin({ username: 'operator', displayName: 'Operator', authType: 'BASIC' }, 'secret');

    const wrapper = mountDashboard();

    expect(wrapper.find('a[href="/admin/import-review"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('JSON 审核中心');
  });
});
