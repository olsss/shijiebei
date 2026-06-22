# 球队球员采集画像设计

## Overview

本阶段把核心数据模型中的 `teams`、`players`、`matches`、`match_lineups`、`match_team_stats`、`source_evidence`、`data_conflicts` 聚合成可查看的球队/球员画像，并新增采集暂存与画像事实表。外部/AI 数据先进入 `collection_items`，管理员批准后才写入 `team_profile_facts` 或 `player_profile_facts`。

## Backend

新增包 `com.worldcup.profile`：

- `api.ProfileController`：画像与采集审核 API。
- `api.dto.*`：请求/响应 DTO。
- `service.ProfileCollectionService`：创建采集任务、列出采集项、批准/驳回采集项。
- `service.ProfileQueryService`：查询球队/球员列表和详情。

本阶段继续使用 `JdbcTemplate` 管理画像事实表，避免引入大量弱结构 JSON JPA entity。正式基础表仍由 `核心数据模型` 阶段负责。

## Data Model

新增 Flyway 迁移 `V4__create_profile_tables.sql`：

- `collection_jobs`：采集任务。
- `collection_items`：采集暂存项与审核状态。
- `team_profile_facts`：球队画像事实。
- `player_profile_facts`：球员画像事实。

事实表字段包括：实体 ID、事实类型、period_key、title、summary、sentiment_label、confidence_score、reliability_score、source_name、source_url、source_ref、captured_at、approved_by、collection_item_id、raw_payload、created_at、updated_at。

## Approval Rules

- `collection_items.status=PENDING_REVIEW` 才能批准/驳回。
- 批准 `TEAM` 项时必须能找到 `teams.team_key=entity_key`。
- 批准 `PLAYER` 项时必须能找到 `players.player_key=entity_key`。
- 同一 collection item 已生成事实后，再次批准直接返回既有事实映射。
- 驳回只更新状态、review_note、reviewed_by，不写正式事实。

## Query Rules

- 球队列表返回基础信息、球员数、事实数、最近更新时间。
- 球队详情返回基础信息、事实列表、球员列表、冲突数和证据数。
- 球员列表返回基础信息、所属球队、事实数、最近更新时间。
- 球员详情返回基础信息、所属球队、事实列表。
- 事实按 `captured_at DESC, id DESC` 排序。

## API

所有接口要求管理员认证：

- `POST /api/profiles/collections/jobs`
- `GET /api/profiles/collections/items`
- `POST /api/profiles/collections/items/{itemId}/approve`
- `POST /api/profiles/collections/items/{itemId}/reject`
- `GET /api/profiles/teams`
- `GET /api/profiles/teams/{teamId}`
- `GET /api/profiles/teams/{teamId}/players`
- `GET /api/profiles/players`
- `GET /api/profiles/players/{playerId}`

## Frontend

- `client/src/api/profiles.ts`：画像 API helper。
- `TeamProfilesView.vue`：球队画像中心，包含列表、详情、事实、球员名单、待审核采集项。
- `PlayerProfilesView.vue`：球员画像中心，包含列表、详情、事实、状态/伤病/红黄牌/更衣室摘要。
- Router 增加 `/profiles/teams` 与 `/profiles/players`。
- Dashboard 模块卡片跳转到真实画像页面。

## Safety Boundary

- 不读取或写入 `skill/archive/` 文件。
- 不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/`。
- 不自动采信未经批准的舆情或外部因素。
- Java 系统只展示结构化事实和证据，不输出下注建议。
