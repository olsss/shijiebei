import ElementPlus from 'element-plus';
import { createPinia, setActivePinia } from 'pinia';
import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DashboardView from '@/views/DashboardView.vue';
import dashboardViewSource from '@/views/DashboardView.vue?raw';
import { fetchCoreDataOverview } from '@/api/coreData';
import { fetchPublicOverview } from '@/api/publicOverview';
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

vi.mock('@/api/publicOverview', () => ({
  fetchPublicOverview: vi.fn().mockResolvedValue({
    success: true,
    data: {
      generatedAt: '2026-06-22 00:00',
      upcomingMatches: [
        {
          matchId: 1,
          matchName: 'France vs Brazil',
          matchday: '2026-06-22',
          jcCode: '001',
          competition: 'World Cup',
          stage: 'Group',
          kickoffTime: '2026-06-22 20:00',
          status: 'SCHEDULED',
          integrityScore: 88,
          riskCount: 2,
        },
        {
          matchId: 2,
          matchName: 'Spain vs Japan',
          matchday: '2026-06-23',
          jcCode: '002',
          competition: 'World Cup',
          stage: 'Group',
          kickoffTime: '2026-06-23 18:00',
          status: 'SCHEDULED',
          integrityScore: 76,
          riskCount: 0,
        },
      ],
      riskCounters: {
        highRiskCount: 1,
        mediumRiskCount: 2,
        staleFactorCount: 3,
        unresolvedConflictCount: 4,
      },
      integrityCounters: {
        completeCount: 5,
        partialCount: 6,
        blockedCount: 7,
      },
      oddsFreshness: {
        marketCount: 8,
        liveMarketCount: 9,
        staleLiveMarketCount: 10,
      },
      decisionSummary: {
        reportCount: 11,
        reviewCount: 12,
        latestDecisionAt: '2026-06-22 01:00',
      },
    },
    message: '',
    timestamp: '',
  }),
}));

const routerLinkStub = {
  props: ['to'],
  template: '<a class="router-link-stub" :href="typeof to === `string` ? to : to.path"><slot /></a>',
};

async function mountDashboard() {
  const wrapper = mount(DashboardView, {
    global: {
      plugins: [ElementPlus],
      stubs: { RouterLink: routerLinkStub },
    },
  });
  await flushPromises();
  return wrapper;
}

describe('DashboardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(fetchCoreDataOverview).mockClear();
    vi.mocked(fetchPublicOverview).mockClear();
  });

  it('loads and renders the public overview without authentication', async () => {
    const wrapper = await mountDashboard();

    expect(fetchPublicOverview).toHaveBeenCalledTimes(1);
    expect(fetchCoreDataOverview).not.toHaveBeenCalled();

    expect(wrapper.find('[data-test="public-kpi-upcoming"]').text()).toContain('2');
    expect(wrapper.find('[data-test="public-kpi-high-risk"]').text()).toContain('1');
    expect(wrapper.find('[data-test="public-kpi-live-odds"]').text()).toContain('9');
    expect(wrapper.find('[data-test="public-kpi-reports"]').text()).toContain('11');
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.text()).toContain('JC 001');
    expect(wrapper.text()).toContain('完整度 88');
    expect(wrapper.text()).toContain('风险 2');
    expect(wrapper.text()).toContain('2026-06-22 00:00');
  });

  it('does not show direct admin review links to anonymous visitors', async () => {
    const wrapper = await mountDashboard();

    expect(wrapper.find('a[href="/import-review"]').exists()).toBe(false);
    expect(wrapper.find('a[href="/admin/import-review"]').exists()).toBe(false);
  });

  it('renders readable Chinese labels for public modules', async () => {
    const wrapper = await mountDashboard();

    const pageText = wrapper.text();

    expect(pageText).not.toContain('????');
    expect(pageText).toContain('比赛中心');
    expect(pageText).toContain('赔率中心');
    expect(pageText).toContain('舆情与外部因素中心');
    expect(pageText).toContain('更多入口');
  });

  it('keeps admin review todos off the public homepage even for writable admins', async () => {
    const auth = useAuthStore();
    auth.setAdmin({ username: 'operator', displayName: 'Operator', authType: 'BASIC' }, 'secret');

    const wrapper = await mountDashboard();

    expect(wrapper.find('a[href="/admin/import-review"]').exists()).toBe(false);
    expect(wrapper.text()).not.toContain('JSON 审核中心');
    expect(wrapper.text()).not.toContain('正式业务数据概览');
    expect(fetchCoreDataOverview).not.toHaveBeenCalled();
  });

  it('declares an overflow-safe mobile layout for 375px screens', () => {
    expect(dashboardViewSource).toContain('@media (max-width: 640px)');
    expect(dashboardViewSource).toContain('grid-template-columns: 1fr');
    expect(dashboardViewSource).toContain('overflow-x: hidden');
    expect(dashboardViewSource).not.toMatch(/min-width:\s*(?:[4-9]\d{2,}|\d{4,})px/);
  });
});
