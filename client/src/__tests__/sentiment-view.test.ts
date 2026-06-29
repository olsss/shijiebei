import ElementPlus from 'element-plus';
import { createPinia, setActivePinia } from 'pinia';
import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SentimentCenterView from '@/views/SentimentCenterView.vue';
import sentimentCenterSource from '@/views/SentimentCenterView.vue?raw';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/api/sentiment', () => ({
  listPublicSentimentOverview: vi.fn(async () => ({
    data: [
      {
        id: 10,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        homeTeam: { teamName: '联调法国', fifaCode: 'FRA', countryIso2: 'FR' },
        awayTeam: { teamName: '联调巴西', fifaCode: 'BRA', countryIso2: 'BR' },
        scoreboard: { homeScore: 2, awayScore: 1, scoreDisplay: '2 - 1', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
        factorCategory: 'WEATHER',
        factorType: 'HEAT_THUNDERSTORM',
        title: '预计小雨',
        summary: '小雨可能降低节奏',
        stale: false,
        riskCount: 1,
        highestRiskLevel: 'LOW',
      },
      {
        id: 101,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'ATTENDANCE',
        factorType: 'CROWD',
        title: '上座人数',
        summary: 'ESPN 记录现场上座 68000 人，仅作比赛环境事实。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 102,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'BROADCAST',
        factorType: 'TV_CHANNELS',
        title: '转播平台',
        summary: 'ESPN Scoreboard 记录转播平台 FOX、Peacock。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 11,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'TRAVEL_REST',
        factorType: 'REST_DAYS',
        title: '休息天数',
        summary: '法国多休一天',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'UNKNOWN',
      },
      {
        id: 12,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'MARKET_SIGNAL',
        factorType: 'MARKET_PRICE_SNAPSHOT',
        title: '赛前市场价格快照',
        summary: '胜平负：联调法国胜 -140（58.3%）；总进球2.5：大于 +105（48.8%）、小于 -130（56.5%）。单点赔率市场快照，仅展示市场价格与隐含概率。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'MEDIUM',
      },
      {
        id: 13,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'PUBLIC_SENTIMENT',
        factorType: 'MEDIA_HEAT',
        title: '媒体叙事集中',
        summary: '边路速度话题偏热',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 14,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'TRAVEL',
        factorType: 'TRAVEL_DISTANCE',
        title: '跨城转场距离',
        summary: '主办城市距离较远',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 15,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'TRAVEL',
        factorType: 'VENUE_TRANSITION',
        title: '场馆转换',
        summary: '换城市比赛',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 16,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'SCHEDULE',
        factorType: 'SCHEDULE_DENSITY',
        title: '赛程密度',
        summary: '滚动 7 天内第二场',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 17,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'DISCIPLINE',
        factorType: 'NEXT_MATCH_DISCIPLINE_WATCH',
        title: '下场纪律关注',
        summary: '累计牌面需赛前复核官方名单',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 170,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'REFEREE',
        factorType: 'OFFICIAL',
        title: '裁判信息',
        summary: 'ESPN gameInfo 记录本场主裁判；仅用于身份与来源追溯。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 171,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'KNOCKOUT',
        factorType: 'ROUND_OF_32_STAGE',
        title: '32强淘汰赛阶段',
        summary: '本场为32强淘汰赛；胜者晋级16强，负者结束本届淘汰赛路径；不能代表市场方向或胜率结论。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 18,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'WORKLOAD',
        factorType: 'TEAM_CONSECUTIVE_STARTERS',
        title: '连续首发人数',
        summary: '阵容连续性需关注，但不代表真实疲劳',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 172,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'FORM',
        factorType: 'TEAM_RECENT_RESULTS',
        title: '近期赛果走势',
        summary: '法国赛前本届已完赛3场，2胜1平0负，近3场为胜-平-胜；仅描述已完赛结果。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 173,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'ATTACK_CONTRIBUTION',
        factorType: 'KEY_PLAYER_CONTRIBUTION',
        title: '赛前关键球员进球参与：联调法国',
        summary: '联调法国赛前本届球员统计记录3名球员有进球或助攻参与，主要参与者：Mbappe 2球1助；Griezmann 0球2助。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 19,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'SCORING',
        factorType: 'GOAL_TIME_BUCKET',
        title: '进球时间段',
        summary: '已确认进球时间主要集中在 61-75 分钟',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 20,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'TECHNICAL_STYLE',
        factorType: 'PASSING_PROFILE',
        title: '控球传球画像',
        summary: '平均控球 58%，传球成功率约 88%',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 21,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'GOALKEEPING',
        factorType: 'SAVE_PRESSURE_PROFILE',
        title: '赛前门将扑救与承压画像：联调法国',
        summary: '联调法国赛前本届门将统计扑救10次、失球1球、零封2场；本资料只来自正式库基础统计。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 24,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'INFRACTION_PROFILE',
        factorType: 'FOUL_OFFSIDE_PENALTY_PROFILE',
        title: '赛前犯规/越位基础画像：联调法国',
        summary: '联调法国赛前本届 ESPN 技术样本犯规/越位基础画像：完整技术样本2场，犯规18次、越位3次；点球尝试1次、命中1次。该画像只描述已入库基础技术统计样本。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 22,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'SQUAD_PROFILE',
        factorType: 'TEAM_SIDE_ROSTER_BASELINE',
        title: '赛前阵容年龄经验画像：联调法国',
        summary: '联调法国赛前官方名单阵容结构画像：名单26人，门将3人、后卫8人、中场7人、前锋8人；年龄按2026-06-24计算，平均27.1岁。该画像只描述官方名单基础字段，不作优劣解读。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
      {
        id: 23,
        matchId: 99,
        matchName: '联调法国 vs 联调巴西',
        factorCategory: 'LINEUP_STRUCTURE',
        factorType: 'CONFIRMED_STARTING_XI_STRUCTURE',
        title: '赛前已确认首发位置结构：联调法国',
        summary: '联调法国赛前本届已确认首发位置结构画像：已入库阵容样本3场、确认首发33人次；该画像只描述已完赛确认阵容与位置编码。',
        stale: false,
        riskCount: 0,
        highestRiskLevel: 'LOW',
      },
    ],
  })),
  getPublicMatchSentiment: vi.fn(async () => ({
    data: {
      matchId: 99,
      matchName: '联调法国 vs 联调巴西',
      homeTeam: { teamName: '联调法国', fifaCode: 'FRA', countryIso2: 'FR' },
      awayTeam: { teamName: '联调巴西', fifaCode: 'BRA', countryIso2: 'BR' },
      scoreboard: { homeScore: 2, awayScore: 1, scoreDisplay: '2 - 1', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
      factors: [
        {
          id: 10,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'WEATHER',
          factorType: 'HEAT_THUNDERSTORM',
          title: '预计小雨',
          summary: '小雨可能降低节奏',
          stale: false,
        },
        {
          id: 101,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'ATTENDANCE',
          factorType: 'CROWD',
          title: '上座人数',
          summary: 'ESPN 记录现场上座 68000 人，仅作比赛环境事实。',
          stale: false,
        },
        {
          id: 102,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'BROADCAST',
          factorType: 'TV_CHANNELS',
          title: '转播平台',
          summary: 'ESPN Scoreboard 记录转播平台 FOX、Peacock。',
          stale: false,
        },
        {
          id: 11,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'TRAVEL_REST',
          factorType: 'REST_DAYS',
          title: '休息天数',
          summary: '法国多休一天',
          stale: false,
        },
        {
          id: 12,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'MARKET_SIGNAL',
          factorType: 'MARKET_PRICE_SNAPSHOT',
          title: '赛前市场价格快照',
          summary: '胜平负：联调法国胜 -140（58.3%）；总进球2.5：大于 +105（48.8%）、小于 -130（56.5%）。单点赔率市场快照，仅展示市场价格与隐含概率。',
          stale: false,
        },
        {
          id: 13,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'PUBLIC_SENTIMENT',
          factorType: 'MEDIA_HEAT',
          title: '媒体叙事集中',
          summary: '边路速度话题偏热',
          stale: false,
        },
        {
          id: 14,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'TRAVEL',
          factorType: 'TRAVEL_DISTANCE',
          title: '跨城转场距离',
          summary: '主办城市距离较远',
          stale: false,
        },
        {
          id: 15,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'TRAVEL',
          factorType: 'VENUE_TRANSITION',
          title: '场馆转换',
          summary: '换城市比赛',
          stale: false,
        },
        {
          id: 16,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'SCHEDULE',
          factorType: 'SCHEDULE_DENSITY',
          title: '赛程密度',
          summary: '滚动 7 天内第二场',
          stale: false,
        },
        {
          id: 17,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'DISCIPLINE',
          factorType: 'NEXT_MATCH_DISCIPLINE_WATCH',
          title: '下场纪律关注',
          summary: '累计牌面需赛前复核官方名单',
          stale: false,
        },
        {
          id: 170,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'REFEREE',
          factorType: 'OFFICIAL',
          title: '裁判信息',
          summary: 'ESPN gameInfo 记录本场主裁判；仅用于身份与来源追溯。',
          stale: false,
        },
        {
          id: 171,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'KNOCKOUT',
          factorType: 'ROUND_OF_32_STAGE',
          title: '32强淘汰赛阶段',
          summary: '本场为32强淘汰赛；胜者晋级16强，负者结束本届淘汰赛路径；不能代表市场方向或胜率结论。',
          stale: false,
        },
        {
          id: 18,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'WORKLOAD',
          factorType: 'TEAM_CONSECUTIVE_STARTERS',
          title: '连续首发人数',
          summary: '阵容连续性需关注，但不代表真实疲劳',
          stale: false,
        },
        {
          id: 172,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'FORM',
          factorType: 'TEAM_RECENT_RESULTS',
          title: '近期赛果走势',
          summary: '法国赛前本届已完赛3场，2胜1平0负，近3场为胜-平-胜；仅描述已完赛结果。',
          stale: false,
        },
        {
          id: 173,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'ATTACK_CONTRIBUTION',
          factorType: 'KEY_PLAYER_CONTRIBUTION',
          title: '赛前关键球员进球参与：联调法国',
          summary: '联调法国赛前本届球员统计记录3名球员有进球或助攻参与，主要参与者：Mbappe 2球1助；Griezmann 0球2助。',
          stale: false,
        },
        {
          id: 19,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'SCORING',
          factorType: 'GOAL_TIME_BUCKET',
          title: '进球时间段',
          summary: '已确认进球时间主要集中在 61-75 分钟',
          stale: false,
        },
        {
          id: 20,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'TECHNICAL_STYLE',
          factorType: 'PASSING_PROFILE',
          title: '控球传球画像',
          summary: '平均控球 58%，传球成功率约 88%',
          stale: false,
        },
        {
          id: 21,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'GOALKEEPING',
          factorType: 'SAVE_PRESSURE_PROFILE',
          title: '赛前门将扑救与承压画像：联调法国',
          summary: '联调法国赛前本届门将统计扑救10次、失球1球、零封2场；本资料只来自正式库基础统计。',
          stale: false,
        },
        {
          id: 24,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'INFRACTION_PROFILE',
          factorType: 'FOUL_OFFSIDE_PENALTY_PROFILE',
          title: '赛前犯规/越位基础画像：联调法国',
          summary: '联调法国赛前本届 ESPN 技术样本犯规/越位基础画像：完整技术样本2场，犯规18次、越位3次；点球尝试1次、命中1次。该画像只描述已入库基础技术统计样本。',
          stale: false,
        },
        {
          id: 22,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'SQUAD_PROFILE',
          factorType: 'TEAM_SIDE_ROSTER_BASELINE',
          title: '赛前阵容年龄经验画像：联调法国',
          summary: '联调法国赛前官方名单阵容结构画像：名单26人，门将3人、后卫8人、中场7人、前锋8人；年龄按2026-06-24计算，平均27.1岁。该画像只描述官方名单基础字段，不作优劣解读。',
          stale: false,
        },
        {
          id: 23,
          matchId: 99,
          matchName: '联调法国 vs 联调巴西',
          factorCategory: 'LINEUP_STRUCTURE',
          factorType: 'CONFIRMED_STARTING_XI_STRUCTURE',
          title: '赛前已确认首发位置结构：联调法国',
          summary: '联调法国赛前本届已确认首发位置结构画像：已入库阵容样本3场、确认首发33人次；该画像只描述已完赛确认阵容与位置编码。',
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
  listPublicSentimentCategories: vi.fn(async () => ({ data: ['WEATHER', 'ATTENDANCE', 'TRAVEL_REST', 'TRAVEL', 'SCHEDULE', 'REFEREE', 'DISCIPLINE', 'KNOCKOUT', 'ADVANCEMENT', 'GROUP_QUALIFICATION', 'FORM', 'ATTACK_CONTRIBUTION', 'WORKLOAD', 'SCORING', 'TECHNICAL_STYLE', 'GOALKEEPING', 'INFRACTION_PROFILE', 'SQUAD_PROFILE', 'LINEUP_STRUCTURE', 'MARKET_SIGNAL', 'PUBLIC_SENTIMENT'] })),
  listPublicSentimentRiskTypes: vi.fn(async () => ({ data: ['PUBLIC_OVERHEAT', 'RAIN'] })),
}));

describe('SentimentCenterView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    useAuthStore().setAdmin({ username: 'admin', displayName: 'Admin', authType: 'basic' });
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
    expect(wrapper.text()).toContain('2 - 1');
    expect(wrapper.text()).toContain('AI 分析的 16 类外部因素');
    expect(wrapper.find('[data-test="sentiment-command-board"]').exists()).toBe(true);
    expect(wrapper.findAll('.coverage-donut').length).toBeGreaterThanOrEqual(2);
    expect(wrapper.findAll('[data-test="sentiment-command-board"] .global-sentiment-rings .coverage-donut')).toHaveLength(5);
    expect(sentimentCenterSource).not.toContain('max-height: min(46dvh, 360px)');
    expect(sentimentCenterSource).not.toContain('max-height: min(52dvh, 420px)');
    expect(sentimentCenterSource).not.toContain('max-height: min(52dvh, 430px)');
    expect(sentimentCenterSource).not.toContain('max-height: min(46dvh, 380px)');
    expect(sentimentCenterSource).toContain('max-height: min(62dvh, 520px)');
    expect(sentimentCenterSource).toContain('grid-template-columns: minmax(0, 1fr) 78px minmax(0, 1fr);');
    expect(sentimentCenterSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(sentimentCenterSource).toMatch(/\.filter-panel,\s+\.side-panel,\s+\.detail-panel,\s+\.intelligence-board,\s+\.priority-gap-panel,\s+\.detail-card-grid\s+\{\s+max-height: none;\s+overflow: visible;\s+scrollbar-gutter: auto;\s+\}/);
    expect(sentimentCenterSource).toMatch(/\.factor-list-scroll\s+\{\s+max-height: min\(50dvh, 420px\);\s+overflow: auto;\s+padding-right: 0;\s+scrollbar-gutter: auto;\s+scrollbar-width: none;\s+\}/);
    expect(sentimentCenterSource).toContain('.factor-list-scroll::-webkit-scrollbar');
    expect(sentimentCenterSource).toContain('flex: 0 0 20px;');
    expect(sentimentCenterSource).toMatch(/\.priority-gap-grid,\s+\.factor-card-grid,\s+\.risk-grid\s+\{\s+max-height: none;\s+overflow: visible;\s+padding-right: 0;\s+scrollbar-gutter: auto;\s+\}/);
    expect(wrapper.find('[data-test="sentiment-command-board"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="sentiment-dimension-panel"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.side-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.detail-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('外因覆盖与缺口');
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('已覆盖维度');
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('高风险因素');
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('过期因素');
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('高等级缺口');
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('主要外因缺口');
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('缺失维度');
    expect(wrapper.text()).toContain('外部情报准备度');
    expect(wrapper.text()).toContain('赔率/市场价格');
    expect(wrapper.text()).toContain('市场价格快照');
    expect(wrapper.text()).toContain('隐含概率');
    expect(wrapper.text()).toContain('证据把握分');
    expect(wrapper.text()).toContain('来源可靠分');
    expect(wrapper.text()).toContain('主要缺口');
    expect(wrapper.text()).toContain('裁判与判罚');
    expect(wrapper.text()).toContain('旅行/休息');
    expect(wrapper.text()).toContain('市场价格信号');
    expect(wrapper.text()).toContain('公众舆情');
    expect(wrapper.text()).toContain('赛程、旅行与休息');
    expect(wrapper.text()).toContain('高温雷暴');
    expect(wrapper.text()).toContain('上座/观众');
    expect(wrapper.text()).toContain('现场上座');
    expect(wrapper.text()).toContain('转播信息');
    expect(wrapper.text()).toContain('转播平台 FOX');
    expect(wrapper.text()).toContain('旅行距离');
    expect(wrapper.text()).toContain('场馆转换');
    expect(wrapper.text()).toContain('赛程密度');
    expect(wrapper.text()).toContain('纪律/牌面');
    expect(wrapper.text()).toContain('下场纪律关注');
    expect(wrapper.text()).toContain('裁判信息');
    expect(wrapper.text()).toContain('身份与来源追溯');
    expect(wrapper.text()).toContain('淘汰赛阶段');
    expect(wrapper.text()).toContain('32强赛阶段');
    expect(wrapper.text()).toContain('胜者晋级16强');
    expect(wrapper.text()).toContain('近期赛果/状态走势');
    expect(wrapper.text()).toContain('近期赛果');
    expect(wrapper.text()).toContain('近3场为胜-平-胜');
    expect(wrapper.text()).toContain('关键球员进球参与');
    expect(wrapper.text()).toContain('主要参与者');
    expect(wrapper.text()).toContain('出场负荷');
    expect(wrapper.text()).toContain('连续首发人数');
    expect(wrapper.text()).toContain('进球方式与时间');
    expect(wrapper.text()).toContain('进球时间段');
    expect(wrapper.text()).toContain('基础技术风格');
    expect(wrapper.text()).toContain('传球画像');
    expect(wrapper.text()).toContain('门将/防守承压');
    expect(wrapper.text()).toContain('扑救与承压画像');
    expect(wrapper.text()).toContain('赛前门将扑救与承压画像');
    expect(wrapper.text()).toContain('犯规/越位样本');
    expect(wrapper.text()).toContain('犯规/越位/点球尝试画像');
    expect(wrapper.text()).toContain('阵容结构/年龄经验');
    expect(wrapper.text()).toContain('球队名单结构画像');
    expect(wrapper.text()).toContain('赛前阵容年龄经验画像');
    expect(wrapper.text()).toContain('首发位置结构');
    expect(wrapper.text()).toContain('已确认首发位置结构');
    expect(wrapper.text()).toContain('赛前已确认首发位置结构');
    expect(wrapper.text()).toContain('热门过热');
    expect(wrapper.text()).toContain('热度过高');
    expect(wrapper.text()).toContain('中风险');
    expect(wrapper.text()).not.toContain('外部情报完整度');
    expect(wrapper.text()).not.toMatch(/不是胜率|不等于胜率|这个分数不是/);
    expect(wrapper.text()).not.toMatch(/新手|读法|先按|先确认|不要只看|提示/);
    expect(wrapper.text()).not.toMatch(/REDUCE_STAKE|TRAVEL_REST|MARKET_SIGNAL|MARKET_PRICE_SNAPSHOT|PUBLIC_SENTIMENT|HEAT_THUNDERSTORM|TRAVEL_DISTANCE|VENUE_TRANSITION|SCHEDULE_DENSITY|NEXT_MATCH_DISCIPLINE_WATCH|OFFICIAL|CROWD|BROADCAST|REFEREE_ASSIGNMENT|REFEREE_SOURCE_AUDIT|KNOCKOUT|ROUND_OF_32_STAGE|FORM|TEAM_RECENT_RESULTS|ATTACK_CONTRIBUTION|KEY_PLAYER_CONTRIBUTION|GOAL_CONTRIBUTION|TOP_SCORER_CONTRIBUTION|ASSIST_CONTRIBUTION|CONTRIBUTION_DISTRIBUTION|TEAM_CONSECUTIVE_STARTERS|SCORING|GOAL_TIME_BUCKET|SPECIAL_SCORING_PROFILE|TECHNICAL_STYLE|PASSING_PROFILE|SHOT_PROFILE|DEFENSIVE_ACTION_PROFILE|GOALKEEPING|SAVE_PRESSURE_PROFILE|GOALKEEPING_SAVE_PROFILE|GOALKEEPER_SAVE_PROFILE|INFRACTION_PROFILE|FOUL_OFFSIDE_PENALTY_PROFILE|FOUL_OFFSIDE_PENALTY_PROFILE_DERIVED|SQUAD_PROFILE|TEAM_SIDE_ROSTER_BASELINE|SQUAD_LIST|LINEUP_STRUCTURE|CONFIRMED_STARTING_XI_STRUCTURE|LINEUP_STRUCTURE_PROFILE|LINEUP_STRUCTURE_DERIVED|\bstake\b|下注|投注|资金|置信分|盘口变动/i);
  });
});
