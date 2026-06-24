# Agent 指南

> 本项目的权威规则见根目录 `CLAUDE.md`。
> 本文件只保留入口说明，避免双份维护漂移。规则冲突时以 `CLAUDE.md` 为准。

## 当前数据流

- 数据库是世界杯管理数据的唯一长期权威源。
- JSON 只作为 AI/人工临时投递与审核材料使用。
- 新 JSON 默认放入 `data-inbox/pending/`，经后台审核与正式入库后，才成为权威数据。
- 入库成功的本地 JSON 归档到 `data-inbox/imported/`；驳回归档到 `data-inbox/rejected/`。
- 旧 `skill/` 足球分析流程已移除，不再作为当前项目规则或数据入口。
