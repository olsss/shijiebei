# 比赛中心与赛程上下文

## Why

核心数据模型已经有比赛、赛程、阵容、事件、球队统计、球员统计、证据链和冲突表；球队/球员画像也已经能展示与球队或球员有关的上下文。但本人还需要一个独立的比赛中心，从比赛视角集中查看赛程、主客队、开球时间、竞彩编号、阶段、场地、首发阵容、上阵人员、事件、进失球时间、外部因素、证据来源和数据冲突。

## What Changes

- 新增后端 `com.worldcup.matchcenter` 查询聚合。
- 新增 API：比赛列表、比赛详情、阵容、事件、球队统计、球员统计。
- 比赛详情聚合外部因素、证据链和数据冲突，便于后续人工审核与分析前准备。
- 前端新增比赛中心页面 `/matches`，Dashboard 接入比赛中心模块卡片。
- 增加后端和前端测试，覆盖认证、列表、详情、子资源、路由和 API helper。

## Out of Scope

- 不实现自动爬虫、不调用付费外部 API、不保存外部密钥。
- 不生成比赛分析结论、下注建议或资金分配。
- 不新增数据库迁移；复用现有核心数据模型表。
- 不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 或 `skill/archive/`。
- 不做赔率中心、分析报告中心、赛后复盘中心。

## Review Gate

本阶段交付前必须完成：

1. `openspec validate 比赛中心与赛程上下文 --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm run test:run` 在 `client/` 通过。
4. `npm run build` 在 `client/` 通过。
5. `git diff --check` 通过。
6. protected diff 确认 `CLAUDE.md` 与 `skill/` 未被修改。
7. 子代理或 Codex 审查通过，并生成 `docs/java-system/reviews/比赛中心与赛程上下文-review.md`。
