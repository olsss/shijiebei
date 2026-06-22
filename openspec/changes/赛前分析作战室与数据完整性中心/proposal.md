# 赛前分析作战室与数据完整性中心

## Why

当前 Java 系统已经具备 JSON 审核入库、核心数据模型、球队/球员画像、比赛中心、赔率中心、舆情与外部因素中心、分析下注复盘中心。问题是这些数据仍分散在多个页面里，赛前检查一场比赛时需要来回切换，难以判断“这场比赛的数据是否已经够完整、是否存在过期或冲突”。

本阶段选择方案 A：构建“赛前分析作战室 + 数据完整性中心”。它以单场比赛为入口，聚合已有正式业务库数据，计算完整性检查项，并在前端集中展示。Java 系统只做聚合、校验和追溯，不生成新比赛分析、不生成下注建议、不自动加注或倍投。

## What Changes

- 新增后端 `prematchworkbench` 查询 API：
  - `GET /api/prematch-workbench/matches`：返回可进入作战室的比赛列表、关键数据计数和完整性概览。
  - `GET /api/prematch-workbench/matches/{matchId}`：返回单场比赛的作战室详情，聚合比赛、球队、球员、阵容、赔率、舆情、分析报告、AI 下注方案和实际下注记录。
  - `GET /api/prematch-workbench/matches/{matchId}/integrity`：返回单场比赛数据完整性检查项。
- 新增数据完整性规则：检查球队画像、球员画像、阵容、赔率、live 赔率时效、舆情外部因素、分析报告、AI 下注方案、多源证据和未解决冲突。
- 新增前端 `/prematch-workbench` 页面：比赛列表、完整性评分、缺失/过期/冲突提示、单场详情标签页。
- Dashboard 增加“赛前分析作战室”入口。
- 新增 OpenSpec 文档审查报告和代码审查报告。

## Out of Scope

- 不新增采集器，不调用外部 API，不从网页抓取最新数据。
- 不生成新的比赛分析、投注方向、下注方式、投注金额、加注、倍投或保证命中文案。
- 不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 或 `skill/archive/`。
- 不新增数据库表；本阶段只聚合和校验现有正式业务表。
- 不替代原有各中心页面；作战室提供赛前汇总入口，详细维护仍在各中心完成。

## Review Gate

文档阶段：

1. `openspec validate 赛前分析作战室与数据完整性中心 --strict` 通过。
2. 文档无未完成标记、无范围矛盾，且明确 Java 只聚合已入库数据。
3. 生成 `docs/java-system/reviews/赛前分析作战室与数据完整性中心-openspec-review.md`。

代码阶段：

1. `openspec validate 赛前分析作战室与数据完整性中心 --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm run test:run` 在 `client/` 通过。
4. `npm run build` 在 `client/` 通过。
5. `git diff --check` 通过。
6. protected diff 确认无 `CLAUDE.md` 与 `skill/` 修改。
7. 生成 `docs/java-system/reviews/赛前分析作战室与数据完整性中心-review.md`。
