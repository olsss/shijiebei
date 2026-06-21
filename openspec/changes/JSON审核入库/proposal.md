# JSON 审核批准入库流程

## Why

当前项目的权威分析与下注档案仍由 AI/Claude/Codex 生成 JSON，并保存在 `skill/archive/`。Java 系统需要提供一个可审计的入口，把这些 JSON 扫描为待审核条目，用户确认后再写入 MySQL 暂存层，作为后续核心数据模型、球队画像、球员画像、分析报告和下注记录模块的数据入口。

## What Changes

- 新增 `import_jobs` 与 `import_items` 数据表，用于记录扫描任务、JSON 条目、校验结果、审核状态和原始 JSON。
- 新增后端扫描服务，识别 `bets.json`、`analysis/*.json`、`odds/*.json`、`sources/*.json` 四类档案。
- 新增 JSON 校验：可解析性、文件类型、基础必填字段、内容摘要、SHA-256 去重指纹。
- 新增后端 API：创建扫描任务、查看任务、查看条目列表、查看条目详情、批准、批量批准、驳回。
- 新增 CLI 入口：`import-json --path <archive> --dry-run` 和 `import-json --path <archive> --approve`。
- 新增前端“JSON 审核中心”页面，展示扫描任务、待审核条目、校验状态、原始 JSON 预览，并支持批准/驳回。
- 新增审计日志记录审核动作。

## Out of Scope

- 不把 JSON 字段拆入最终业务表（球队、球员、比赛、下注、赔率等业务表由后续 change 实现）。
- 不修改 `CLAUDE.md`、`skill/` 比赛分析规则或现有 JSON 档案。
- 不自动做比赛分析、下注推荐或赔率判断。

## Review Gate

本阶段交付前必须完成：

1. `openspec validate JSON审核入库 --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm --prefix client run test:run` 通过。
4. `npm --prefix client run build` 通过。
5. 审查者确认 `CLAUDE.md` 与 `skill/` 未被误改。
6. 审查者确认审批流是“JSON 暂存入库”，没有越界替代比赛分析。

