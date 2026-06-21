# JSON 审核批准入库阶段审查记录

- 阶段：第二阶段 / `JSON审核入库`
- 分支：`feature/json-review-import`（实现分支；OpenSpec change 已重命名为 `JSON审核入库`）
- 基线：`aaa1d3a`
- 审查时间：2026-06-22 03:06（Asia/Shanghai）
- 审查者：Codex

## 审查结论

通过。该阶段已建立 JSON 审核入库暂存层：后端可扫描 `skill/archive/` 风格 JSON、生成 import job/items、校验 JSON 与基础字段、批准/批量批准/驳回并写审计日志；CLI 支持 dry-run/approve；前端新增“JSON 审核中心”。本阶段没有修改 `CLAUDE.md` 或 `skill/`，审批仅进入 MySQL 暂存表，不替代比赛分析流程。

## 验证命令证据

| 命令 | 结果 |
| --- | --- |
| `openspec validate JSON审核入库 --strict` | 通过：`Change 'JSON审核入库' is valid` |
| `mvn -f server/pom.xml test` | 通过：`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS` |
| `npm --prefix client run test:run` | 通过：`Test Files 4 passed`，`Tests 5 passed` |
| `npm --prefix client run build` | 通过：`✓ built`；存在 Vite/Rollup chunk 与 PURE 注释警告，非阻断 |
| `git diff --name-only aaa1d3a..HEAD \| Select-String -Pattern '^(CLAUDE.md\|skill/)'` | 无输出，确认未改保护文件/目录 |
| 变更文件 secret pattern 扫描 | 无输出 |
| `git status --short` | 在生成本审查记录前为空；之后仅包含 tasks/review 文档更新 |

## OpenSpec 对照

1. JSON archive scan job：已实现 `POST /api/import-jobs/scan`，创建 `import_jobs` 并记录 archive path、total/valid/invalid、message、时间字段。
2. JSON import item classification：`JsonArchiveScanner` 覆盖 `bets.json`、`analysis/*.json`、`odds/*.json`、`sources/*.json`，排除 `_模板.json`。
3. JSON validation and summary：支持 JSON 解析校验、基础字段校验、summary 提取、SHA-256；无效 JSON/API 测试覆盖。
4. Review status transitions：支持 approve/reject/batch approve，写 `audit_logs`；无效 JSON 批准返回 400。
5. CLI import command：`import-json --path <archive> --dry-run` 调用只读扫描，`--approve` 扫描并持久化且自动批准有效条目。
6. JSON review frontend：新增 `/import-review` 路由、API helper、审核页面、路由/API 测试。

## 安全边界审查

- `JsonArchiveScanner` 仅使用 `Files.list`、`Files.isRegularFile`、`Files.readString` 读取 JSON；没有写入、移动、删除 `skill/archive/` 的代码。
- 后端批准动作只更新 `import_items` 状态并写 `audit_logs`，不创建球队/球员/比赛/下注最终业务表。
- protected diff 检查确认未修改 `CLAUDE.md`、`skill/`。
- 前端只提供 JSON 审核操作，不生成比赛结论、赔率判断或下注建议。

## 风险与后续建议

- 当前“基础字段校验”仍是阶段内轻量规则：BETS 要求 `bets` 数组；ANALYSIS/SOURCE 要求 `id` 或 `match`；ODDS 要求 `event_id` 或 `match`。后续最终业务模型阶段应升级为按 schema 的字段级校验。
- 前端生产构建存在大 chunk 警告，阶段内不阻断；后续可做路由懒加载/手动分包。
- CLI approve 使用运行时数据库配置，真实导入前应确认 MySQL 连接参数与备份策略。

## 结论

阶段通过，可合并到 main 后进入下一阶段：核心业务数据模型（球队、球员、比赛、赔率、下注、舆情/外部因素）设计与入库映射。


