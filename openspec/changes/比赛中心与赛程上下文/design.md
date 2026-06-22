# Design: 比赛中心与赛程上下文

## 数据模型

本阶段不新增表，直接读取现有正式业务表：

- `matches`：比赛基础信息、赛程、状态和外部因素。
- `teams`：主队/客队展示名。
- `players`：阵容和球员统计展示名。
- `match_lineups`：首发/替补/上阵信息。
- `match_events`：进球、红黄牌、换人等事件。
- `match_team_stats`：球队进失球和进球时间点。
- `match_player_stats`：球员分钟、进球、助攻、红黄牌。
- `source_evidence`：比赛级证据。
- `data_conflicts`：比赛级冲突。

## 后端结构

- `com.worldcup.matchcenter.api.MatchCenterController`
- `com.worldcup.matchcenter.api.dto.MatchCenterDtos`
- `com.worldcup.matchcenter.service.MatchCenterQueryService`

使用 `JdbcTemplate` 与只读事务，风格保持与 `ProfileQueryService` 一致。

## API

- `GET /api/matches`
- `GET /api/matches/{matchId}`
- `GET /api/matches/{matchId}/lineups`
- `GET /api/matches/{matchId}/events`
- `GET /api/matches/{matchId}/team-stats`
- `GET /api/matches/{matchId}/player-stats`

## 响应模型

- `MatchSummaryResponse`：比赛基础字段、主客队名称、事件/阵容/证据/冲突计数。
- `MatchDetailResponse`：summary、lineups、events、teamStats、playerStats、evidence、conflicts、externalFactors。
- `MatchLineupResponse`：球队、球员、角色、位置、首发标记。
- `MatchEventResponse`：分钟、事件类型、球队、球员、payload。
- `MatchTeamStatsResponse`：球队统计、进失球、首球分钟、进球分钟、payload。
- `MatchPlayerStatsResponse`：球员统计、分钟、进球、助攻、红黄牌、payload。
- `MatchEvidenceResponse`：来源、时间、摘要、可靠性。
- `MatchConflictResponse`：冲突类型、字段、当前值/新值、处理状态。

## 前端结构

- `client/src/api/matches.ts`
- `client/src/views/MatchCenterView.vue`
- `client/src/router/index.ts`
- `client/src/views/DashboardView.vue`

## 安全与规则边界

- 所有 `/api/matches/**` 均走现有认证。
- 页面只展示正式库数据，不直接调用 OpenAI/Claude，不绕过 JSON 审核批准流程。
- 不输出投注推荐、资金建议或比赛结论。
