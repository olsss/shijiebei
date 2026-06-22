# 分析报告下注记录与赛后复盘中心代码审查报告

日期：2026-06-22（北京时间）
分支：`feature/analysis-bet-review-center`
审查范围：`main...HEAD`

## 审查结论

通过。当前变更满足 OpenSpec 范围：批准后的 ANALYSIS/BETS JSON 入库，后端认证查询 API，前端 `/analysis-review` 页面展示。未发现阻塞问题。

## 需求边界审查

- AI/Claude/Codex 生成分析报告、下注方式与下注方案 JSON；Java 系统仅在用户批准后保存与展示。
- `BusinessJsonMapper` 只解析已批准 JSON 字段，不接入外部实时数据，不生成新推荐、不计算新选项、不加注、不倍投。
- 前端页面文案已明确“AI 方案归档”和“实际出票记录”分离。
- protected diff 检查未发现 `CLAUDE.md` 或 `skill/` 修改。

## 数据模型审查

- 新增 `V7__create_analysis_bet_review_tables.sql`：
  - `bet_plans` 保存 AI 下注方案，包含 `betting_method`、`strategy_type`。
  - `bet_plan_items` 保存方案明细，包含 `play_type`、`pass_type`。
  - `post_match_reviews` 保存五层赛后复盘。
  - `review_lessons` 保存复盘规则沉淀。
  - `bets` 扩展票号、下注日、比赛日、收盘赔率、CLV、返还、结算时间、复盘状态。
- 外键顺序与测试清理顺序已覆盖新增依赖。

## JSON 入库审查

- `mapAnalysis()` 保留原有 `analysis_reports`、`source_evidence` 行为，并新增：
  - `bet_plan`、`bet_plans[]`、`recommended_plan` -> `bet_plans`。
  - `items[]`、`selections[]`、`tickets[]` -> `bet_plan_items`。
  - `post_match_review`、`review`、`post_match_reviews[]` -> `post_match_reviews`。
  - `lessons[]`、`rules[]` -> `review_lessons`。
- `mapBets()` 保留旧 BETS 导入，并新增 ticket、日期、收盘赔率、CLV、返还、结算字段。
- CLV 计算口径：缺失时使用 `entry odds / closing odds - 1`，保留 6 位小数。

## 后端 API 审查

- 新增 `AnalysisReviewCenterController`，所有接口复用现有 Basic Auth。
- 新增查询服务覆盖 overview、reports、report detail、bet-plans、plan detail、bets、reviews。
- overview 统计总投入、总返还、净盈亏、ROI、平均 CLV；总投入为 0 时 ROI 返回 0。
- Controller 测试覆盖未认证 401、统计、列表、详情、下注记录、复盘规则。

## 前端审查

- 新增 `client/src/api/analysisReview.ts`，路径 helper 与类型定义覆盖后端接口。
- 新增 `/analysis-review` 路由和 Dashboard 入口。
- 页面用标签页区分分析报告、AI 下注方案归档、实际出票记录、赛后复盘。
- 页面展示 raw payload，保留追溯路径。
- 构建通过；Vite 输出仅有既有 chunk/PURE 注释警告，无编译错误。

## TDD 证据

- ANALYSIS 入库测试先 RED：`approvedAnalysisImportsBetPlanAndPostMatchReview` 初次只返回 1 个旧映射，期望 6 个映射。
- BETS 入库测试先 RED：`approvedBetsImportsSettlementAndClvFields` 初次失败于 `ticket_no` 字段不存在。
- API 测试先 RED：已认证 `/api/analysis-review/*` 返回 404。
- 前端测试先 RED：`@/api/analysisReview` 缺失，router 未包含 `/analysis-review`。

## 验证结果

- `openspec validate 分析报告下注记录与赛后复盘中心 --strict`：通过。
- `mvn -f server/pom.xml test`：58 tests，0 failures，BUILD SUCCESS。
- `npm run test:run`：11 files / 13 tests passed。
- `npm run build`：exit 0。
- `git diff --check`：通过。
- protected diff：无 `CLAUDE.md`、`skill/` 修改。

## 审查备注

- Vite chunk size 警告不属于本阶段阻塞问题；如后续页面继续增长，可单独做路由级动态导入和 manualChunks 优化。
