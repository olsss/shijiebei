# Design: 分析报告下注记录与赛后复盘中心

## 核心决策

本阶段采用“AI JSON 草稿 -> 用户审核批准 -> MySQL 正式库 -> 前端展示统计”的路径。Java 系统只做审核后数据的保存、查询、统计和追溯，不直接生成比赛分析或下注方式。

数据分四类：

1. 分析报告：九维分析、本届逐场表现、风险点、冷门路径、结论类型、置信度、长文内容。
2. AI 下注方案：玩法、选项、金额建议、赔率、逻辑类型、风险说明、资金分配理由。
3. 实际下注记录：票号、真实投入、出票赔率、收盘赔率、CLV、返还、盈亏、结算状态。
4. 赛后复盘：数学层、足球层、盘口层、大赛气质层、赔率价值层、规则沉淀。

## 数据模型

### analysis_reports

继续作为分析报告正式表。`BusinessJsonMapper.mapAnalysis()` 保留现有行为，并作为 AI 下注方案和赛后复盘的关联入口。

### bet_plans

保存 AI 生成的下注方案。关键字段：`import_item_id`、`analysis_report_id`、`match_id`、`plan_key`、`plan_title`、`conclusion_type`、`confidence`、`budget_amount`、`risk_summary`、`betting_method`、`strategy_type`、`status`、`generated_by`、`generated_at`、`raw_payload`。

`status` 使用 `IMPORTED` 表示该方案已由用户批准入库。它不是出票状态，不能和实际下注混用。

### bet_plan_items

保存下注方案明细。关键字段：`bet_plan_id`、`match_id`、`market_type`、`selection_text`、`stake_suggestion`、`odds`、`line_value`、`logic_type`、`risk_level`、`play_type`、`pass_type`、`item_order`、`raw_payload`。

### bets 扩展字段

现有 `bets` 表继续保存实际下注记录。本阶段增加正式出票与结算字段：`ticket_no`、`bet_date`、`matchday`、`closing_odds`、`clv`、`return_amount`、`settled_at`、`plan_item_id`、`review_status`。

CLV 优先从 JSON 读取；若 JSON 同时提供入场赔率和收盘赔率且未提供 CLV，入库时按 `odds / closing_odds - 1` 计算。

### post_match_reviews

保存赛后五层复盘。关键字段：`import_item_id`、`match_id`、`analysis_report_id`、`review_key`、`review_title`、`math_review`、`football_review`、`handicap_review`、`tournament_temperament_review`、`odds_value_review`、`overall_summary`、`raw_payload`。

### review_lessons

保存复盘沉淀规则。关键字段：`review_id`、`lesson_type`、`lesson_text`、`severity`、`raw_payload`。

## 入库解析

### ANALYSIS JSON

`BusinessJsonMapper.mapAnalysis()` 增强解析：

- `bet_plan`、`bet_plans[]`、`recommended_plan` 进入 `bet_plans`。
- 方案内 `items[]`、`selections[]`、`tickets[]` 进入 `bet_plan_items`。
- 方案层 `betting_method`/`下注方式` 与 `strategy_type` 保存 AI 方案的下注方式和策略类型。
- 明细层 `play_type`/`pass_type` 保存单关、串关、总进球、半全场等玩法表达方式。
- `post_match_review`、`review` 进入 `post_match_reviews`。
- 复盘内 `lessons[]`、`rules[]` 进入 `review_lessons`。
- 原有 `analysis_reports` 与 `source_evidence` 行为保持兼容。

### BETS JSON

`BusinessJsonMapper.mapBets()` 增强解析：

- `ticket_no`、`ticket`、`票号` 保存为票号。
- `bet_date`、`下注日期` 保存为下注日。
- `matchday`、`比赛日` 保存为比赛日。
- `closing_odds`、`收盘赔率` 保存为收盘赔率。
- `clv` 保存为 CLV；缺失时按赔率计算。
- `return_amount`、`返还` 保存为返还。
- `profit_loss`、`盈亏` 保存为盈亏。
- `review` 或 `post_match_review` 可生成赛后复盘。

## 查询 API

新增 `com.worldcup.analysisreviewcenter`：

- `GET /api/analysis-review/overview`：统计分析报告数、AI 下注方案数、实际下注数、已复盘数、总投入、总返还、净盈亏、平均 CLV。
- `GET /api/analysis-review/reports`：分析报告列表。
- `GET /api/analysis-review/reports/{reportId}`：分析报告详情，含关联方案和复盘。
- `GET /api/analysis-review/bet-plans`：AI 下注方案列表。
- `GET /api/analysis-review/bet-plans/{planId}`：下注方案详情，含 items。
- `GET /api/analysis-review/bets`：实际下注记录列表。
- `GET /api/analysis-review/reviews`：赛后复盘列表。

所有 API 复用现有 Basic Auth 安全策略。

## 前端

新增 `/analysis-review` 页面：

- 顶部统计：分析报告、下注方案、实际下注、已复盘、总投入、净盈亏、平均 CLV。
- 标签页：分析报告、AI 下注方案、实际下注、赛后复盘。
- 分析报告详情：结论类型、置信度、风险摘要、长文内容、原始 JSON。
- AI 下注方案详情：玩法、选项、金额建议、赔率、逻辑类型、风险等级、原始 JSON。
- 实际下注详情：票号、玩法、投入、赔率、收盘赔率、CLV、返还、盈亏、命中状态。
- 赛后复盘详情：五层复盘与规则沉淀。

Dashboard 新增入口卡片，指向 `/analysis-review`。

## 统计口径

- 总投入：实际下注记录 `stake` 求和。
- 总返还：`return_amount` 求和，缺失时不计入。
- 净盈亏：优先使用 `profit_loss` 求和。
- ROI：`净盈亏 / 总投入`，总投入为 0 时返回 0。
- 平均 CLV：非空 `clv` 求平均。

## 边界

AI 下注方案可展示“金额建议”和“玩法组合”，因为它是已批准 JSON 的归档内容；Java 系统不得根据实时数据生成新方案，也不得在页面额外推导“推荐下注”“加注”“倍投”“稳赚”“必胜”等结论。
