# Design: 赛前分析作战室与数据完整性中心

## 核心决策

本阶段采用“现有正式业务库 -> 单场比赛聚合 -> 完整性检查 -> 前端作战室展示”的路径。它不新增采集、不新增 AI 生成能力、不新增下注推导，只把已批准入库的数据按比赛维度集中展示，并把缺失、过期、冲突显式暴露出来。

作战室服务定位为只读查询层，新增包 `com.worldcup.prematchworkbench`。它通过 `JdbcTemplate` 查询现有表：`matches`、`teams`、`players`、`team_profile_facts`、`player_profile_facts`、`match_lineups`、`odds_market_snapshots`、`odds_selection_snapshots`、`match_context_factors`、`sentiment_risk_assessments`、`source_evidence`、`data_conflicts`、`analysis_reports`、`bet_plans`、`bet_plan_items`、`bets`。

## 后端 API

### `GET /api/prematch-workbench/matches`

返回比赛列表和作战室概览字段：

- 比赛基础字段：`matchId`、`matchName`、`matchday`、`jcCode`、`competition`、`stage`、`homeTeamName`、`awayTeamName`。
- 数据计数：球队画像、球员画像、阵容、赔率市场、舆情因素、分析报告、AI 下注方案、实际下注。
- 完整性字段：`integrityScore`、`missingCount`、`staleCount`、`conflictCount`。

### `GET /api/prematch-workbench/matches/{matchId}`

返回单场作战室详情：

- `summary`：比赛概览和完整性分数。
- `teams`：主客队基础信息与球队画像事实。
- `lineups`：上阵人员、首发、位置、角色。
- `players`：关联球员状态、伤病、红黄牌、更衣室情况与球员画像事实。
- `oddsMarkets`：赔率市场、盘口、公司、快照类型、抓取时间与选项赔率。
- `sentimentFactors`：舆情与外部因素、时效状态、风险评分。
- `analysisReports`：已批准分析报告摘要与原始 JSON。
- `betPlans`：AI 下注方案归档和方案明细。
- `bets`：实际出票记录。
- `integrityChecks`：完整性检查项。

### `GET /api/prematch-workbench/matches/{matchId}/integrity`

只返回完整性检查项，便于前端刷新检查状态。

所有 API 复用现有 Basic Auth 安全策略。未知比赛返回 404。

## 数据完整性规则

检查项使用统一结构：`code`、`label`、`status`、`severity`、`message`、`evidenceCount`、`lastUpdatedAt`。

状态：

- `PASS`：数据存在且未发现过期或冲突。
- `MISSING`：关键数据缺失。
- `STALE`：数据存在但超过时效阈值。
- `CONFLICT`：存在未解决数据冲突。

规则：

1. `TEAM_PROFILE`：主客队均存在球队画像事实则通过，否则缺失。
2. `PLAYER_PROFILE`：有阵容球员且每名阵容球员至少有一条球员画像事实则通过；无阵容或任一阵容球员缺画像则缺失。
3. `LINEUP`：存在阵容且至少有首发记录则通过，否则缺失。
4. `ODDS_MARKET`：存在赔率市场快照则通过，否则缺失。
5. `LIVE_ODDS_FRESHNESS`：存在 `LIVE` 赔率且最新 live 抓取时间距当前时间不超过 3 小时则通过；存在但超过 3 小时则过期；没有 live 赔率则缺失。
6. `SENTIMENT_FACTOR`：存在舆情/外部因素且未过期则通过；存在但有过期因素则过期；不存在则缺失。
7. `ANALYSIS_REPORT`：存在已入库分析报告则通过，否则缺失。
8. `AI_BET_PLAN`：存在已批准 AI 下注方案则通过，否则缺失。
9. `MULTI_SOURCE_EVIDENCE`：来源证据不少于 2 条则通过；少于 2 条则缺失。
10. `UNRESOLVED_CONFLICT`：无未解决冲突则通过；存在 `PENDING` 或非 `RESOLVED` 冲突则冲突。

完整性分数：`PASS` 项数量 / 检查项总数 * 100，四舍五入为整数。`MISSING`、`STALE`、`CONFLICT` 都不计入通过项。该分数只表示数据准备程度，不表示下注胜率或推荐强度。

## 前端页面

新增 `/prematch-workbench`：

- 顶部提示：只展示已批准 JSON 和正式库聚合，不生成下注建议。
- 左侧/上方比赛列表：显示比赛、竞彩编号、完整性分数、缺失/过期/冲突数。
- 右侧/下方详情标签页：
  - 数据完整性：检查项表格。
  - 比赛与阵容：比赛摘要、首发和上阵人员。
  - 球队与球员画像：球队特性、球员状态、伤病、红黄牌、更衣室情况。
  - 赔率与盘口：各公司、各玩法、选项赔率和 live 时效。
  - 舆情与证据：外部因素、风险评分、来源证据和冲突。
  - AI 分析与下注归档：分析报告、AI 下注方案、实际出票记录。

Dashboard 增加入口卡片，指向 `/prematch-workbench`。

## 边界

- 完整性分数和检查项只用于提示数据准备程度，不代表比赛结果概率。
- Java 不根据赔率、舆情、阵容或 AI 方案生成新投注方向。
- 页面不得输出“推荐下注”“加注”“倍投”“稳赚”“必胜”等新推导文案。
- 如果数据缺失或过期，页面只提示“需补采/需复核/数据不足”，不建议具体投注动作。
