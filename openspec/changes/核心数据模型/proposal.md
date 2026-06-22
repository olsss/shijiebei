# 核心数据模型

## Why

JSON 审核批准流程已经把 `skill/archive/` 中的 AI/Claude/Codex 档案保存为可审核的 `import_items`。Java 系统下一步需要把已批准 JSON 映射进正式 MySQL 业务表，作为球队画像、球员画像、比赛中心、赔率中心、分析报告、下注记录和复盘模块的统一数据地基。

## What Changes

- 新增核心业务基础表：数据字典、证据链、冲突记录、球队、球员、比赛、事件、统计、阵容、赔率快照、分析报告、下注记录和导入映射表。
- 新增 `com.worldcup.coredata` 后端聚合，负责 approved import item 的正式入库、幂等映射和概览统计。
- 新增 API：核心数据概览、单条 import item 正式入库、查询 import item 映射结果。
- 新增正式入库映射规则：支持 `ANALYSIS`、`ODDS`、`SOURCE`、`BETS` 四类 JSON。
- 前端轻量更新：Dashboard 展示核心数据概览，JSON 审核中心展示/触发正式入库状态。
- 增加后端和前端测试，覆盖批准校验、重复导入幂等、四类 JSON 映射和 API helper。

## Out of Scope

- 不做完整球队画像、球员画像、比赛中心、赔率图表和舆情页面。
- 不自动生成比赛分析、下注建议或赔率判断。
- 不修改 `CLAUDE.md`、`skill/` 规则或现有 JSON 档案。
- 不删除、不覆盖原始 JSON；正式表只保存结构化摘要和追溯引用。

## Review Gate

本阶段交付前必须完成：

1. `openspec validate 核心数据模型 --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm --prefix client run test:run` 通过。
4. `npm --prefix client run build` 通过。
5. protected diff 确认 `CLAUDE.md` 与 `skill/` 未被修改。
6. 审查者确认正式入库只读取 `import_items.raw_json`，不写 `skill/archive/`。
7. 子代理或 Codex 审查通过，并生成 `docs/java-system/reviews/核心数据模型-review.md`。
