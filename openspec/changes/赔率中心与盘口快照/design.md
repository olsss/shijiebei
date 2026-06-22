# Design: 赔率中心与盘口快照

## 核心决策

赔率保存采用三层结构：

1. `odds_snapshots`：保留旧快照批次/兼容统计。
2. `odds_market_snapshots`：某比赛、某公司、某玩法、某盘口线、某快照时间。
3. `odds_selection_snapshots`：该玩法下每个选项赔率。

这样可以保存“胜平负三项”“让球胜平负三项”“比分多项”“总进球 0-7+”“半全场 9 项”等完整赔率。

## 数据模型

### odds_market_snapshots

字段：`id`、`import_item_id`、`odds_snapshot_id`、`match_id`、`bookmaker`、`market_code`、`market_name`、`snapshot_type`、`handicap_line`、`line_value`、`captured_at`、`source_ref`、`raw_payload`、`created_at`。

### odds_selection_snapshots

字段：`id`、`market_snapshot_id`、`selection_code`、`selection_name`、`odds_value`、`implied_probability`、`selection_status`、`raw_payload`、`created_at`。

## 入库解析

`BusinessJsonMapper.mapOdds()` 支持：

- `companies[].markets[].selections[]`
- `companies[].markets[].outcomes[]`
- `companies[].markets[].options[]`
- `markets[].odds` 或 `markets[].prices` 对象
- `all_books[].markets[]`
- 旧单赔率 market 格式

旧单赔率格式会写入一个 selection：`selection_code=RAW`、`selection_name=原始赔率`。

## 后端 API

- `GET /api/odds`
- `GET /api/odds/matches/{matchId}`
- `GET /api/odds/bookmakers`
- `GET /api/odds/markets`

所有接口走认证，只展示正式业务库中的赔率事实。

## 前端

- 新增 `client/src/api/odds.ts`
- 新增 `client/src/views/OddsCenterView.vue`
- 路由 `/odds`
- Dashboard 卡片“赔率中心”

## 规则边界

- 体彩赔率只作为出票/结算票面 SP 事实，不在本阶段做分析主依据。
- 页面不输出下注建议、资金建议或推荐结论。
- 本阶段不修改 `skill/` 和 `CLAUDE.md`。
