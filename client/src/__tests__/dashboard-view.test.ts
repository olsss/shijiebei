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
          status: 'FINISHED',
          resultStatus: 'FINAL',
          homeTeam: {
            teamId: 10,
            teamName: '法国',
            fifaCode: 'FRA',
            countryIso2: 'FR',
          },
          awayTeam: {
            teamId: 20,
            teamName: '巴西',
            fifaCode: 'BRA',
            countryIso2: 'BR',
          },
          scoreboard: {
            homeScore: 2,
            awayScore: 0,
            scoreDisplay: '2 - 0',
            winnerSide: 'HOME',
            resultText: '主队胜',
            scoreSource: 'TEAM_STATS',
          },
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
        marketCount: 18,
        liveMarketCount: 9,
        staleLiveMarketCount: 1,
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
    expect(wrapper.find('[data-test="public-kpi-conflicts"]').text()).toContain('4');
    expect(wrapper.find('[data-test="public-kpi-odds-market"]').text()).toContain('18');
    expect(wrapper.find('[data-test="public-kpi-reports"]').text()).toContain('11');
    expect(wrapper.find('[data-test="odds-freshness-non-live"]').text()).toContain('8');
    expect(wrapper.findAll('.odds-ring-grid .coverage-donut')).toHaveLength(4);
    expect(wrapper.findAll('.decision-ring-grid .coverage-donut')).toHaveLength(4);
    expect(wrapper.find('[data-test="match-intel-panel"]').text()).toContain('赛事情报');
    expect(wrapper.find('[data-test="match-intel-panel"]').text()).toContain('2 - 0');
    expect(wrapper.find('[data-test="match-intel-panel"]').text()).toContain('主队胜');
    expect(wrapper.find('[data-test="match-intel-panel"]').text()).toContain('资料准备度');
    expect(wrapper.find('[data-test="match-intel-panel"]').text()).toContain('风险记录');
    expect(wrapper.find('[data-test="match-intel-panel"]').attributes('tabindex')).toBe('0');
    expect(wrapper.findAll('[data-test="match-intel-panel"] .coverage-donut')).toHaveLength(2);
    expect(wrapper.find('[data-test="match-intel-panel"]').text()).toContain('风险压力');
    expect(wrapper.findAll('.overview-grid .overview-ring-grid .coverage-donut')).toHaveLength(7);
    expect(wrapper.text()).toContain('过期因素');
    expect(wrapper.text()).toContain('待补资料');
    expect(wrapper.text()).not.toMatch(/新手|三步判断|先看这里|新手解释|风险先看|今天先看|提示|引导|读法|数据准备度|风险项/);
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.text()).toContain('竞彩 001');
    expect(wrapper.text()).toContain('准备度 88%');
    expect(wrapper.text()).toContain('资料准备度');
    expect(wrapper.text()).toContain('实时市场');
    expect(wrapper.text()).toContain('非实时快照');
    expect(wrapper.text()).toContain('过期实时');
    expect(wrapper.text()).toContain('复盘条目');
    expect(wrapper.text()).toContain('材料合计');
    expect(wrapper.text()).toContain('最新记录');
    expect(wrapper.text()).toContain('市场价格快照');
    expect(wrapper.text()).toContain('已入库市场快照');
    expect(wrapper.text()).not.toContain('证据完整度');
    expect(wrapper.text()).not.toMatch(/盘口|赔率异动|盘口异动|已入库盘口|资料较完整/);
    expect(wrapper.text()).toContain('风险 2');
    expect(wrapper.text()).toContain('2026-06-22 00:00');
    expect(dashboardViewSource).not.toContain('max-height: min(72dvh, 620px)');
    expect(dashboardViewSource).not.toContain('max-height: min(60dvh, 520px)');
    expect(dashboardViewSource).not.toContain('max-height: min(64dvh, 520px)');
    expect(dashboardViewSource).toContain('max-height: min(42dvh, 320px)');
    expect(dashboardViewSource).toMatch(/\.match-list\s+\{\s+max-height: min\(42dvh, 320px\);\s+overflow: auto;\s+padding-right: 0;\s+scrollbar-gutter: auto;\s+scrollbar-width: none;\s+\}/);
    expect(dashboardViewSource).toMatch(/\.entry-grid,\s+\.overview-ring-grid,\s+\.odds-ring-grid,\s+\.decision-ring-grid\s+\{\s+max-height: none;\s+overflow: visible;\s+padding-right: 0;\s+scrollbar-gutter: auto;\s+\}/);
    expect(dashboardViewSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(dashboardViewSource).not.toContain('当前已入库市场快照主要是赛前或赛后归档记录');
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
