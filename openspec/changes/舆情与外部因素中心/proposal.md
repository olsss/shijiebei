# 舆情与外部因素中心

## Why

当前系统已能管理比赛、球队/球员画像和赔率，但舆情与外部因素仍缺少结构化中心。天气、场地、裁判、旅行距离、休息天数、战意、轮换、默契球风险、主帅压力、队内矛盾、媒体观点、球迷情绪和市场热门程度都会影响赛前判断；如果只存在长文本或 `matches.external_factors`，无法审核来源、追踪时效、展示风险评分或服务后续分析报告。

## What Changes

- 新增 `match_context_factors` 保存 A1 事实型舆情与外部因素记录。
- 新增 `sentiment_risk_assessments` 保存 A2 风险类型、风险等级和风险分。
- 增强 SOURCE JSON 批准入库流程，解析 `external_factors/factors/sentiment_records/sentiments/risk_assessments/risks`。
- 保留现有 `source_evidence`、`data_conflicts`、`aliases` 入库行为。
- 新增后端 `com.worldcup.sentimentcenter`，提供总览、比赛详情、分类字典、风险类型字典 API。
- 新增前端 `/sentiment` 页面，展示因素记录、风险评分、过期提醒、来源与原始 payload。
- 增加 OpenSpec 文档审查报告和代码审查报告。

## Out of Scope

- 不调用外部天气、新闻、社媒或赔率 API。
- 不自动生成比赛结论、下注建议、资金分配或推荐组合。
- 不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 或 `skill/archive/`。
- 不替代 AI/Claude/Codex 的比赛分析流程。
- 不做高级趋势图；本阶段以结构化表格和风险摘要为主。

## Review Gate

文档阶段：

1. `openspec validate 舆情与外部因素中心 --strict` 通过。
2. 新增文档无未完成标记、无矛盾、范围聚焦 A1 + A2。
3. 生成 `docs/java-system/reviews/舆情与外部因素中心-openspec-review.md`。

代码阶段：

1. `openspec validate 舆情与外部因素中心 --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm run test:run` 在 `client/` 通过。
4. `npm run build` 在 `client/` 通过。
5. `git diff --check` 通过。
6. protected diff 确认无 `CLAUDE.md` 与 `skill/` 修改。
7. 生成 `docs/java-system/reviews/舆情与外部因素中心-review.md`。
