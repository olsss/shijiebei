# 核心数据模型设计

## Overview

本阶段建立正式业务库基础层。第二阶段已经生成 `import_items` 暂存记录；本阶段只允许 `status=APPROVED` 且 `valid_json=true` 的条目进入正式表。正式表保存查询和统计需要的结构化字段，同时通过 `import_item_id` 和 `source_evidence` 保留原始 JSON 追溯链。

## Backend

新增包 `com.worldcup.coredata`：

- `domain`：核心业务 JPA entity，例如 `BusinessMatch`、`AnalysisReport`、`BetRecord`、`ImportItemMapping`。
- `repo`：JPA repositories。
- `service`：`CoreDataImportService`、`BusinessJsonMapper`、`MatchKeyNormalizer`、`CoreDataOverviewService`。
- `api`：`CoreDataController` 和 DTO。

`CoreDataImportService` 负责：

1. 查找 import item。
2. 校验 `APPROVED` 和 `valid_json=true`。
3. 判断是否已有映射，已有则幂等返回。
4. 根据 `ImportItemType` 调用 mapper。
5. 写入 `import_item_mappings` 与 `audit_logs`。

## Data Model

新增 Flyway 迁移 `V3__create_core_data_tables.sql`。

核心表：

- `data_dictionaries`：字典与别名。
- `source_evidence`：来源证据。
- `data_conflicts`：来源冲突。
- `teams`、`players`：球队和球员基础档案。
- `matches`：比赛基础信息，使用 `match_key` 去重。
- `match_events`、`match_team_stats`、`match_player_stats`、`match_lineups`：后续比赛/阵容模块使用的基础表。
- `odds_snapshots`：赔率快照。
- `analysis_reports`：分析报告结构化摘要与长文。
- `bets`：正式下注记录。
- `import_item_mappings`：正式入库映射和幂等记录。

## Mapping Rules

### ANALYSIS

从 `analysis/*.json` 提取：

- `match`、`matchday`、`jc_code` upsert 到 `matches`。
- `id`、`status`、`conclusion_type`、`confidence`、`risks`、`recommended`、`dimensions`、`sources`、`narrative_md` 保存到 `analysis_reports`。
- `sources[]` 尽量展开到 `source_evidence`。

### ODDS

从 `odds/*.json` 提取：

- `match`、`date`、`jc_code` upsert 到 `matches`。
- `companies`、`markets`、`all_books` 中可识别的 bookmaker / market / selection / odds 展开到 `odds_snapshots`。
- 无法精确结构化的片段保存到 `raw_payload`。

### SOURCE

从 `sources/*.json` 提取：

- `match`、`date`、`jc_code` upsert 到 `matches`。
- `snapshots` 展开到 `source_evidence`。
- `conflicts` 展开到 `data_conflicts`。
- `aliases` 写入 `data_dictionaries`。

### BETS

从 `bets.json` 提取：

- 每个 `bets[]` 元素写入 `bets`。
- `比赛` 与 `比赛日` upsert 到 `matches`。

## API

所有接口继续要求管理员认证：

- `GET /api/core-data/overview`
- `POST /api/core-data/import-items/{itemId}/import`
- `GET /api/core-data/import-items/{itemId}/mappings`

成功响应使用 `ApiResponse.ok(...)`。

## Frontend

- `client/src/api/coreData.ts` 提供 overview、import item、mapping 查询函数。
- Dashboard 增加正式业务数据概览卡片。
- JSON 审核中心对 `APPROVED` 条目显示正式入库按钮，已导入时显示映射状态。

## Safety Boundary

- 导入服务只读取数据库中的 `import_items.raw_json`。
- 不写入、移动、删除 `skill/archive/` 文件。
- 不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/`。
- Java 系统只做归档、查询和管理，不做比赛判断或下注推荐。
