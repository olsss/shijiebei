# 舆情与外部因素中心代码审查报告

审查时间：2026-06-22 15:07（北京时间）
审查者：Codex（本地审查；未启用子代理委派）
审查范围：`openspec/changes/舆情与外部因素中心`、后端舆情入库与查询 API、前端 `/sentiment` 页面。

## 结论

通过。当前实现符合 A1 事实记录 + A2 风险评分范围：SOURCE JSON 批准后保存外部因素、舆情记录与风险评分；后端提供认证保护的查询 API；前端只展示事实、来源、时效与风险评分，不生成方向或金额结论。

## 审查要点

- OpenSpec：中文 change `舆情与外部因素中心` 通过严格校验，proposal/design/tasks/spec 与实现一致。
- 数据模型：新增 `match_context_factors` 与 `sentiment_risk_assessments`，包含 match/import item/factor 关联与查询索引。
- SOURCE 入库：保留既有 evidence/conflicts/aliases 行为，并新增 factors/risks 映射；审查中发现同义数组并存时漏采，已用 TDD 增加失败用例并在 `ceff414` 修复。
- 后端 API：覆盖 `/api/sentiment`、`/api/sentiment/matches/{matchId}`、`/api/sentiment/categories`、`/api/sentiment/risk-types`，未登录返回 401，未知比赛返回 404。
- 前端页面：新增 sentiment API helper、路由和 Dashboard 入口；页面展示筛选、统计、详情、时效与 raw payload。
- 边界：未修改 `CLAUDE.md`、`skill/`；未新增外部抓取或自动下注/投注建议逻辑；未提交密钥。

## 验证证据

- `openspec validate 舆情与外部因素中心 --strict`：通过，输出 `Change '舆情与外部因素中心' is valid`。
- `mvn -f server/pom.xml test`：通过，52 tests，0 failures，0 errors。
- `npm run test:run`（client）：通过，10 test files，12 tests。
- `npm run build`（client）：通过，exit 0；保留既有 Rollup PURE 注释与 chunk size warnings。
- `git diff --check`：通过，无输出。
- protected diff 检查：通过，无 `CLAUDE.md`、`skill/*` 变更。
- 风险文案扫描：未发现“下注建议/投注建议/推荐下注/加注/倍投/稳赚/必胜/投注方向”等命中文案。

## 已处理问题

1. 重要：`external_factors` 与 `factors`、`sentiment_records` 与 `sentiments`、`risk_assessments` 与 `risks` 同时出现时只采集第一个字段。
   - 红灯：`CoreDataImportServiceTest#approvedSourceImportsAllSentimentAliasArraysWhenPresentTogether` 失败。
   - 修复：改为逐字段累加入库。
   - 绿灯：目标测试 1/1 通过，`CoreDataImportServiceTest` 10/10 通过，最终后端全量 52/52 通过。

## 剩余风险

无阻塞风险。前端构建仍有既有依赖 PURE 注释与大 chunk 警告，不影响本阶段交付。
