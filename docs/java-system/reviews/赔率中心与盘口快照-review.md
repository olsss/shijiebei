# 赔率中心与盘口快照代码审查报告

日期：2026-06-22
分支：`feature/odds-center`
范围：OpenSpec change、结构化赔率入库、赔率中心后端 API、前端 `/odds` 页面、测试与验证。

## 结论

本次审查未发现未处理的 Critical / Important 问题。模块满足“各个玩法的赔率都保存”的当前阶段要求：批准后的 ODDS JSON 会按“市场快照 + 选项赔率”结构保存，每个玩法与每个选项赔率可通过后端 API 和前端赔率中心查看。

## 审查重点

- 数据模型：新增 `odds_market_snapshots` 与 `odds_selection_snapshots`，保留旧 `odds_snapshots` 兼容行为。
- 入库流程：`BusinessJsonMapper.mapOdds()` 支持 `companies[]`、`all_books[]`、`markets[]`、`selections/outcomes/options`、对象型 `odds/prices`，并保存原始 payload。
- 赔率完整性：显式选项、标量对象赔率、嵌套对象赔率均有测试覆盖。
- 后端接口：`GET /api/odds`、`GET /api/odds/matches/{matchId}`、`GET /api/odds/bookmakers`、`GET /api/odds/markets` 需要认证并返回结构化数据。
- 前端页面：`/odds` 提供总览、筛选、市场列表、选项赔率和原始 JSON 核对。
- 边界：不调用外部赔率 API，不做下注建议、不做资金分配，不修改 `CLAUDE.md` 或 `skill/`。

## 审查中已修复的问题

1. 详情接口缺少 `selectionCount`：补充 DTO 字段、SQL 计数与接口测试。
2. 零盘口展示为 `-`：抽出 `formatMarketLine()`，新增前端测试，确保 `0` 盘口保留显示。
3. 嵌套对象赔率丢失赔率值：支持 `{ "HOME": { "name": "主胜", "odds": 1.80 } }` 形式，保留 name、odds、probability、status，并同步更新 OpenSpec。

## 验证结果

- `openspec validate 赔率中心与盘口快照 --strict`：通过，输出 `Change '赔率中心与盘口快照' is valid`。
- `mvn -f server/pom.xml test`：通过，`Tests run: 45, Failures: 0, Errors: 0, Skipped: 0`。
- `npm run test:run`（client）：通过，`Test Files 9 passed`，`Tests 11 passed`。
- `npm run build`（client）：通过，Vite 构建 exit 0；仅保留现有 Rollup PURE 注释与 chunk size 警告。
- `git diff --check`：通过，无输出。
- protected diff：通过，无 `CLAUDE.md` 与 `skill/` 修改。

## 后续建议

- 数据量增长后可补分页和按比赛/公司/玩法的服务端过滤。
- 后续若加入赔率曲线，可基于 `captured_at` 和市场/选项表做时间序列视图。
- 若来源 JSON 进一步固定，可增加玩法/选项代码字典标准化。
