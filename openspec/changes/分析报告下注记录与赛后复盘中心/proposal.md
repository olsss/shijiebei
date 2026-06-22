# 分析报告下注记录与赛后复盘中心

## Why

当前系统已经具备 JSON 审核入库、核心数据模型、球队/球员画像、比赛中心、赔率中心、舆情与外部因素中心，但分析报告、AI 下注方案、实际出票记录和赛后复盘还缺少统一的业务中心。现有 `analysis_reports` 与 `bets` 表只能保存基础内容，无法区分“AI 给出的下注方式”和“用户实际出票”，也无法展示 ROI、CLV、五层复盘和规则沉淀。

本阶段要把 AI/Claude/Codex 生成的分析报告、下注方案、下注记录和复盘 JSON 纳入统一审批入库链路：AI 生成 JSON，用户审核批准，Java 系统保存到 MySQL 并展示统计。Java 系统不自动生成下注方案，只管理已经批准的结构化数据。

## What Changes

- 增强 ANALYSIS JSON 入库：保存分析报告的同时保存 AI 下注方案 `bet_plans` 与 `bet_plan_items`。
- 增强 BETS JSON 入库：扩展实际下注记录，保存票号、下注日、比赛日、收盘赔率、CLV、返还、盈亏、结算状态和原始 payload。
- 新增赛后复盘表 `post_match_reviews` 与 `review_lessons`，保存五层复盘和规则沉淀。
- 新增后端 `analysisreviewcenter` 查询 API，提供总览、分析报告、下注方案、下注记录和复盘查询。
- 新增前端 `/analysis-review` 页面，用标签页展示分析报告、AI 下注方案、实际下注、赛后复盘和 ROI/CLV 概览。
- Dashboard 增加“分析/下注/复盘中心”入口。
- 增加 OpenSpec 文档审查报告和代码审查报告。

## Out of Scope

- 不调用 AI、新闻、赔率或彩票平台接口生成新分析。
- 不自动生成下注方式、投注方向、倍投、加注或保证命中文案。
- 不替代 `CLAUDE.md` 与 `skill/` 的比赛分析流程。
- 不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 或 `skill/archive/`。
- 不实现复杂图表；本阶段以表格、详情、统计卡片和原始 JSON 追溯为主。

## Review Gate

文档阶段：

1. `openspec validate 分析报告下注记录与赛后复盘中心 --strict` 通过。
2. 新增文档无未完成标记、无矛盾，范围聚焦分析报告、AI 下注方案、实际下注记录和赛后复盘。
3. 生成 `docs/java-system/reviews/分析报告下注记录与赛后复盘中心-openspec-review.md`。

代码阶段：

1. `openspec validate 分析报告下注记录与赛后复盘中心 --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm run test:run` 在 `client/` 通过。
4. `npm run build` 在 `client/` 通过。
5. `git diff --check` 通过。
6. protected diff 确认无 `CLAUDE.md` 与 `skill/` 修改。
7. 生成 `docs/java-system/reviews/分析报告下注记录与赛后复盘中心-review.md`。
