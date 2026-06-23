## Context

当前系统已有 MySQL 正式业务表、`import_jobs` / `import_items` 审核暂存表、JSON 审核页面和 `BusinessJsonMapper` 映射器。历史实现默认从 `../skill/archive` 扫描 `bets.json`、`analysis/*.json`、`odds/*.json`、`sources/*.json`，适配的是旧 skill 长期保存 JSON 档案的工作方式。

现在足球分析 skill 已删除，项目规则已调整为：数据库是唯一权威数据源；JSON 只作为 AI/人工向系统投递数据的临时文件。后续 skill 可以读取数据库进行分析，但写入必须输出 JSON，经系统审核后进入数据库。

## Goals / Non-Goals

**Goals:**

- 建立 `data-inbox` 临时投递目录，替代 `skill/archive` 作为默认扫描入口。
- 让扫描器按 JSON 内部 `type` 识别数据类型，支持扁平 `pending/*.json` 投递。
- 支持基础主数据入库：球队、球员、比赛、阵容、事件、技术统计。
- 继续支持现有业务数据入库：来源、赔率、分析、下注记录，并补充下注方案、赛后复盘、复盘规则等类型。
- 导入成功或驳回后移动本地 JSON，避免 pending 目录反复扫描同一文件。
- 保留数据库中的 `import_items.raw_json` 与业务表 `raw_payload` 作为长期审计原文。
- 保持后台审核闸门：正式业务表写入必须经过批准，AI 不直接写数据库。

**Non-Goals:**

- 不恢复旧足球分析 skill。
- 不让本地 JSON 继续作为长期权威数据源。
- 不自动采集外部网站数据；外部数据获取仍由用户或后续独立能力处理。
- 不绕过管理员审核直接写正式业务表。
- 不在本变更中重做前端整体 UI。

## Decisions

### Decision 1: 数据库作为唯一权威源

正式页面、分析查询和复盘均读取 MySQL。JSON 入库后即不再作为事实来源，数据库中的原始 JSON 字段用于审计与追溯。

替代方案是 JSON 作为权威源、数据库作为缓存；该方案更适合文档型项目，不适合当前已有后台管理、审核和多中心查询的系统。

### Decision 2: 使用 `data-inbox/pending` 作为默认投递目录

目录结构固定为：`pending/`、`imported/`、`rejected/`、`templates/`。后端默认扫描 `../data-inbox/pending`，前端默认输入同一路径。

替代方案是继续使用 `skill/archive`；该方案会把已删除的 skill 与系统数据流继续绑定，造成职责混乱。

### Decision 3: 通过 JSON `type` 字段分类

新 JSON 使用统一外壳：`type`、`idempotency_key`、`source`、`payload`。扫描器优先读取 `type`，并保留对旧结构的兼容识别以便必要时迁移。

替代方案是继续依赖目录名；该方案不适合临时投递，因为用户和 AI 需要记住多个目录结构。

### Decision 4: 文件归档在正式导入/驳回后执行

扫描只把文件录入审核暂存，不移动文件；正式导入成功后移动到 `imported/yyyy-MM-dd/`，驳回后移动到 `rejected/yyyy-MM-dd/`。这样用户在审核前仍能修正 pending 文件，入库后 pending 目录保持干净。

替代方案是在批准时移动文件；但批准不等于正式业务表已写入，提前移动会让失败重试变复杂。

### Decision 5: 基础主数据通过同一审核链路入库

新增 `TEAM`、`PLAYER`、`MATCH`、`MATCH_LINEUP`、`MATCH_EVENT`、`MATCH_STATS` 类型，复用 `import_items` 审核与 `import_item_mappings` 映射。基础表使用自然键幂等 upsert：`team_key`、`player_key`、`match_key`。

替代方案是直接 SQL 灌库；该方案快但缺少审核、审计和幂等映射，不适合作为长期工作流。

### Decision 6: skill 只读数据库，写入走 JSON 审核

后续足球分析 skill 可以通过数据库/API 读取权威事实，生成 `ANALYSIS`、`BET_PLAN` 等 JSON 投递文件，但不得直接写正式业务表。

替代方案是给 skill 数据库写权限；该方案风险高，容易污染权威数据。

## Risks / Trade-offs

- `type` 字段缺失或写错导致扫描失败 → 扫描器返回明确校验错误，并在 `templates/` 提供可复制模板。
- 基础主数据 upsert 可能覆盖已有字段 → 仅在 payload 显式提供字段时更新，并保留完整 raw payload；实现前用测试固定行为。
- 导入后移动文件失败但数据库已写入 → 业务导入仍以数据库为准，文件移动失败记录审计/错误信息，允许用户手动清理 pending。
- 旧 JSON 档案路径被删除导致默认扫描为空 → 默认路径切到 `data-inbox/pending`，前端文案说明旧 skill 路径不再使用。
- 模板过多导致维护成本 → 第一版只提供常用核心模板，后续按实际数据类型补充。

## Migration Plan

1. 新建 `data-inbox/pending`、`data-inbox/imported`、`data-inbox/rejected`、`data-inbox/templates`，使用 `.gitkeep` 保留空目录。
2. 修改后端和前端默认扫描路径到 `../data-inbox/pending`。
3. 扩展扫描器按 `type` 识别新格式，同时保留旧格式兼容。
4. 扩展 `BusinessJsonMapper` 支持基础主数据与补充业务类型。
5. 导入或驳回后执行文件归档；失败时不回滚业务导入，但返回/记录可见错误。
6. 添加模板和测试，验证新数据流。
7. 如果需要恢复旧数据，可从 Git 历史或数据库 `import_items.raw_json` 查找，不再依赖本地 JSON 副本。

## Open Questions

- 已导入 JSON 是否长期保留在 `imported/`，还是定期清理：默认先保留，后续可加清理任务。
- 是否需要在 UI 上提供“导入成功后立即删除文件”开关：本变更不做，保留归档即可。
