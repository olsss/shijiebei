## Why

当前系统仍假设本地 `skill/archive` 是长期数据入口，但足球分析 skill 已被删除，且用户已确认数据库应作为唯一权威数据源。需要把 JSON 从“长期档案”降级为“临时投递文件”，避免本地 JSON 与数据库双重维护造成数据漂移。

## What Changes

- 将系统数据原则调整为：数据库是唯一权威源；JSON 只作为入库前临时投递与审核材料。
- 新增 `data-inbox/` 临时投递目录约定，包含 `pending/`、`imported/`、`rejected/`、`templates/`。
- 修改默认扫描路径，从 `../skill/archive` 切换到 `../data-inbox/pending`。
- 修改 JSON 扫描逻辑：优先读取 JSON 内部 `type` 字段识别导入类型，而不是依赖 `analysis/odds/sources/bets.json` 旧目录结构。
- 扩展导入类型，支持基础主数据与业务数据：球队、球员、比赛、阵容、事件、技术统计、来源、赔率、分析、下注方案、下注记录、赛后复盘、复盘规则。
- 导入正式库成功后，将对应本地 JSON 从 `pending/` 移动到按日期分组的 `imported/`；驳回时移动到 `rejected/`。
- 保留 `import_items.raw_json` 与业务表 `raw_payload` 作为长期审计原文，不再要求维护本地 JSON 副本。
- 新增 JSON 模板，供 AI/人工生成可审核入库的数据文件。
- 明确 skill 后续边界：skill 可读取数据库分析，但写入只能输出 JSON 到临时投递目录，经系统审核后入库。

## Capabilities

### New Capabilities

- 无。该变更是在现有 Java 管理系统、JSON 审核入库、核心数据模型能力上调整数据权威源与导入链路。

### Modified Capabilities

- `java-system`: 修改 JSON 审核入库与核心数据导入需求，使数据库成为唯一权威数据源，并支持 `data-inbox` 临时投递、基础主数据入库、导入后文件归档。

## Impact

- 后端配置：`server/src/main/resources/application.yml` 与 `AppProperties` 默认 archive path。
- 后端导入审核：`JsonArchiveScanner`、`ImportItemType`、导入审核 API/服务、CLI 默认路径。
- 后端核心映射：`BusinessJsonMapper` 增加基础主数据与新业务类型映射。
- 数据库：可能需要扩展 import item 类型枚举使用方式、文件归档审计字段或状态信息；现有正式表优先复用。
- 前端管理页：`ImportReviewView` 默认路径与文案更新，必要时展示 data-inbox 工作流状态。
- 项目文件：新增 `data-inbox/` 目录骨架与 `templates/` 模板。
- 规则文档：`CLAUDE.md` 已约定重大程序变更走 OpenSpec；足球分析/获取数据/下注不走 OpenSpec。
