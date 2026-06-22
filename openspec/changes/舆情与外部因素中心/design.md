# Design: 舆情与外部因素中心

## 核心决策

本阶段采用 A1 + A2：

1. A1 使用 `match_context_factors` 保存事实型外部因素和舆情记录。
2. A2 使用 `sentiment_risk_assessments` 保存风险评分。
3. SOURCE JSON 是输入入口，用户批准后才进入正式表。
4. Java 系统只展示事实、观点、来源、时效和风险评分，不输出投注结论。

## 数据模型

### match_context_factors

保存比赛相关的天气、场地、裁判、旅行、休息、战意、轮换、更衣室、媒体、球迷情绪和市场热度等记录。

关键字段：`import_item_id`、`match_id`、`factor_category`、`factor_type`、`title`、`summary`、`impact_direction`、`entity_type`、`entity_key`、`evidence_level`、`source_name`、`source_url`、`source_ref`、`observed_at`、`expires_at`、`confidence_score`、`reliability_score`、`raw_payload`。

### sentiment_risk_assessments

保存风险类型、等级和分数。`factor_id` 可为空；为空表示比赛级总体风险。

关键字段：`import_item_id`、`match_id`、`factor_id`、`risk_type`、`risk_level`、`risk_score`、`title`、`rationale`、`suggested_action`、`source_name`、`source_ref`、`raw_payload`。

## 入库解析

`BusinessJsonMapper.mapSource()` 增强解析：

- `external_factors[]`、`factors[]`、`sentiment_records[]`、`sentiments[]` 进入 `match_context_factors`。
- `weather`、`venue`、`referee`、`motivation`、`public_sentiment` 等单对象也转为 factor。
- factor 内 `risks[]` 与顶层 `risk_assessments[]` / `risks[]` 进入 `sentiment_risk_assessments`。
- 每个入库目标写 `import_item_mappings`，便于审核追踪。

## 查询 API

- `GET /api/sentiment`：总览列表，含过期状态、风险数量、最高风险等级。
- `GET /api/sentiment/matches/{matchId}`：比赛详情，含 factors 与 risks。
- `GET /api/sentiment/categories`：已入库分类。
- `GET /api/sentiment/risk-types`：已入库风险类型。

## 过期判断

查询服务按 `expires_at` 与当前数据库/服务时间判断：

- `expires_at` 为空：不标过期。
- `expires_at < now`：`stale=true`。

## 风险等级排序

`CRITICAL > HIGH > MEDIUM > LOW > UNKNOWN`。总览的最高风险等级由该比赛/因素相关风险计算。

## 前端

新增 `/sentiment`：

- 顶部统计：比赛、因素、风险、过期、高风险。
- 筛选：分类、风险类型、过期状态。
- 因素表：比赛、分类、标题、影响方向、来源、可信度、过期状态、最高风险。
- 详情：同场 factors、risk assessments、原始 JSON。

## 边界

风险评分只用于提醒，不得转换成下注建议。`suggested_action` 只能是核查、监控、降置信度等动作。
