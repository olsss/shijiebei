# Tasks: 分析报告下注记录与赛后复盘中心

## 1. OpenSpec 与文档审查

- [x] 创建中文 change 目录与文档。
- [x] 编写 proposal、design、tasks、spec delta。
- [x] 编写 `docs/superpowers/specs/2026-06-22-分析报告下注记录与赛后复盘中心-design.md`。
- [x] 编写 `docs/superpowers/plans/2026-06-22-分析报告下注记录与赛后复盘中心.md`。
- [x] 执行 `openspec validate 分析报告下注记录与赛后复盘中心 --strict`。
- [x] 执行文档未完成标记与一致性审查。
- [x] 生成 `docs/java-system/reviews/分析报告下注记录与赛后复盘中心-openspec-review.md`。

## 2. 数据模型与 JSON 入库

- [x] 新增 ANALYSIS 下注方案与复盘入库测试并先确认失败。
- [x] 新增 BETS 扩展字段与 CLV 入库测试并先确认失败。
- [x] 新增 V7 迁移：`bet_plans`、`bet_plan_items`、`post_match_reviews`、`review_lessons` 和 `bets` 扩展字段。
- [x] 增强 `BusinessJsonMapper.mapAnalysis()` 保存 AI 下注方案、方案明细、赛后复盘和规则沉淀。
- [x] 增强 `BusinessJsonMapper.mapBets()` 保存票号、下注日、比赛日、收盘赔率、CLV、返还和复盘。
- [x] 保持既有 analysis、bets、source evidence 行为不变。
- [x] 目标后端入库测试通过。

## 3. 后端 API

- [x] 新增分析/下注/复盘中心控制器测试并先确认失败。
- [x] 实现 DTO、查询服务和控制器。
- [x] 覆盖认证、总览、分析报告列表/详情、下注方案列表/详情、下注记录列表、复盘列表。
- [x] 目标后端 API 测试通过。

## 4. 前端

- [x] 新增 analysis-review API helper 测试并更新 router 测试，先确认失败。
- [x] 实现 analysis-review API helper、页面、路由和 Dashboard 卡片。
- [x] 前端页面展示统计、分析报告、AI 下注方案、实际下注、赛后复盘与原始 JSON。
- [x] 前端测试与构建通过。

## 5. 验证与代码审查

- [x] `openspec validate 分析报告下注记录与赛后复盘中心 --strict` 通过。
- [x] `mvn -f server/pom.xml test` 通过。
- [x] `npm run test:run` 在 `client/` 通过。
- [x] `npm run build` 在 `client/` 通过。
- [x] `git diff --check` 通过。
- [x] protected diff 无 `CLAUDE.md` 与 `skill/` 修改。
- [x] 生成 `docs/java-system/reviews/分析报告下注记录与赛后复盘中心-review.md`。
