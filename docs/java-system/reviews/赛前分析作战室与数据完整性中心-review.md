# 赛前分析作战室与数据完整性中心代码审查报告

审查时间：2026-06-22（北京时间）
审查范围：OpenSpec change `赛前分析作战室与数据完整性中心`、后端 `prematchworkbench` 只读聚合 API、前端 `/prematch-workbench` 页面、测试与入口。

## 1. OpenSpec 与范围审查

- Change 名称使用中文：`赛前分析作战室与数据完整性中心`。
- 范围保持方案 A：赛前作战室 + 数据完整性中心。
- 未新增数据库表；后端只聚合既有正式业务表：比赛、球队、球员、阵容、赔率、舆情、证据、冲突、分析报告、AI 下注方案与实际下注记录。
- 完整性分数只表示数据准备度，不表示胜率、投注价值或下注建议。
- 未修改 `CLAUDE.md`、`skill/`、`skill/rules/`、`skill/archive/`。

## 2. 后端代码审查

新增：

- `PrematchWorkbenchController`
  - `GET /api/prematch-workbench/matches`
  - `GET /api/prematch-workbench/matches/{matchId}`
  - `GET /api/prematch-workbench/matches/{matchId}/integrity`
- `PrematchWorkbenchQueryService`
  - 使用 `JdbcTemplate` 只读查询。
  - 未知比赛返回 404。
  - 聚合球队画像、球员画像、阵容、各玩法赔率盘口快照、舆情风险、多源证据、未解决冲突、分析报告、AI 下注方案、实际出票。
- `PrematchWorkbenchDtos`
  - DTO 字段与前端展示、测试 JSONPath 对齐。

完整性规则审查：

1. `TEAM_PROFILE`：主客队均有球队画像。
2. `PLAYER_PROFILE`：阵容球员均有球员画像。
3. `LINEUP`：存在阵容且存在首发。
4. `ODDS_MARKET`：存在赔率市场快照。
5. `LIVE_ODDS_FRESHNESS`：LIVE 赔率 3 小时内更新。
6. `SENTIMENT_FACTOR`：舆情/外部因素存在且未过期。
7. `ANALYSIS_REPORT`：存在分析报告。
8. `AI_BET_PLAN`：存在 AI 下注方案。
9. `MULTI_SOURCE_EVIDENCE`：至少 2 个来源证据。
10. `UNRESOLVED_CONFLICT`：未解决冲突显示 `CONFLICT`。

已处理问题：

- 详情接口证据链在相同 `evidence_time` 下排序不稳定，目标测试暴露后已改为 `evidence_time DESC, id`，保证先入库证据稳定展示。

## 3. 前端代码审查

新增：

- `client/src/api/prematchWorkbench.ts`
  - path builder 与请求函数：列表、详情、完整性子接口。
- `client/src/views/PrematchWorkbenchView.vue`
  - 比赛准备清单、完整性卡片与检查项。
  - 球队/球员/阵容、赔率与盘口、舆情/证据/冲突、分析/方案/出票标签页。
  - 页面明确提示：完整性分数不是下注推荐。
- `client/src/router/index.ts`
  - 新增 `/prematch-workbench`。
- `client/src/views/DashboardView.vue`
  - 新增赛前分析作战室入口。

前端 TDD 审查：

- 新增 API path helper 测试，并先确认因模块缺失失败。
- 更新 router 测试，并先确认因路由缺失失败。
- 实现后重新运行测试与构建通过。

## 4. 验证结果

- `openspec validate '赛前分析作战室与数据完整性中心' --strict`
  - 结果：`Change '赛前分析作战室与数据完整性中心' is valid`
- `mvn -f server/pom.xml test`
  - 结果：`Tests run: 62, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
- `npm run test:run`（client）
  - 结果：`Test Files 12 passed`，`Tests 14 passed`
- `npm run build`（client）
  - 结果：exit 0，构建成功；仍有既有 Vite/Rollup 大 chunk 与 PURE 注释警告，不阻塞本次交付。
- `git diff --check`
  - 结果：exit 0，无空白错误。
- protected diff 检查
  - 结果：无输出，未修改受保护规则文件。

## 5. 审查结论

本次变更满足已确认设计边界：不参与比赛分析生成，不生成下注建议，不改规则 skill，不新增表，只做赛前数据聚合和完整性可视化。后端、前端、OpenSpec 与保护文件检查均通过，准予合并。
