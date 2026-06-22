import ElementPlus from 'element-plus';
import { createPinia, setActivePinia } from 'pinia';
import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SentimentCenterView from '@/views/SentimentCenterView.vue';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/api/sentiment', () => ({
  listSentimentOverview: vi.fn(async () => ({
    data: [
      {
        id: 10,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'WEATHER',
        factorType: 'RAIN',
        title: '预计小雨',
        summary: '小雨可能降低节奏',
        stale: false,
        riskCount: 1,
        highestRiskLevel: 'LOW',
      },
    ],
  })),
  getMatchSentiment: vi.fn(async () => ({
    data: {
      matchId: 99,
      matchName: '联调法国 vs 联调巴西',
      factors: [
        {
          id: 10,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'WEATHER',
          factorType: 'RAIN',
          title: '预计小雨',
          summary: '小雨可能降低节奏',
          stale: false,
        },
      ],
      risks: [
        {
          id: 21,
          matchId: 99,
          factorId: 10,
          riskType: 'RAIN',
          riskLevel: 'LOW',
          riskScore: 22,
          title: '草皮偏滑',
          suggestedAction: 'MONITOR',
        },
        {
          id: 22,
          matchId: 99,
          factorId: undefined,
          riskType: 'PUBLIC_OVERHEAT',
          riskLevel: 'MEDIUM',
          riskScore: 61,
          title: '热门过热',
          suggestedAction: 'REDUCE_STAKE',
        },
      ],
    },
  })),
  listSentimentCategories: vi.fn(async () => ({ data: ['WEATHER'] })),
  listSentimentRiskTypes: vi.fn(async () => ({ data: ['PUBLIC_OVERHEAT', 'RAIN'] })),
}));

describe('SentimentCenterView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    useAuthStore().setAdmin({ username: 'admin', displayName: '系统管理员', authType: 'basic' });
  });

  it('renders match-level risks that are not attached to a specific factor', async () => {
    const wrapper = mount(SentimentCenterView, {
      global: {
        plugins: [ElementPlus],
        mocks: {
          $router: { push: vi.fn() },
        },
      },
    });

    await flushPromises();
    await flushPromises();

    expect(wrapper.text()).toContain('比赛级风险评分');
    expect(wrapper.text()).toContain('热门过热');
    expect(wrapper.text()).toContain('PUBLIC_OVERHEAT');
    expect(wrapper.text()).toContain('MEDIUM');
  });
});
