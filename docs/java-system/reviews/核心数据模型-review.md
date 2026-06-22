# 核心数据模型阶段审查记录

## 审查结论

- 结论：PASS
- 审查时间：2026-06-22
- 审查范围：OpenSpec change `核心数据模型`，分支 `feature/core-data-model`，diff `b1d9c14..58a0e6b`
- 子代理复审：PASS，无阻塞问题

## 实现摘要

- 新增 `V3__create_core_data_tables.sql`，建立 dictionaries/evidence/conflicts/teams/players/matches/match details/odds/analysis/bets/import mappings 等正式业务表。
- 新增 `com.worldcup.coredata` 后端聚合：正式入库服务、JSON mapper、match key normalizer、overview service、REST API 和 DTO。
- 正式入库只允许 `APPROVED && validJson` 的 `import_items`，读取 `import_items.raw_json`，通过 `import_item_mappings` 实现幂等。
- 支持 ANALYSIS / ODDS / SOURCE / BETS 基础映射；ODDS 支持 `companies.markets` / `markets` / `all_books` 展开。
- 前端新增 core data API helper，Dashboard 正式业务数据概览，ImportReview 已批准项导入正式库按钮与映射状态/映射数。
- OpenSpec 已明确本阶段使用 `JdbcTemplate + Jackson` 查询/写入模型，不新增核心业务 JPA entity/repository。

## 验证命令证据

在 feature worktree `C:\Users\admin\.config\superpowers\worktrees\世界杯\core-data-model` 执行：

```powershell
openspec validate 核心数据模型 --strict
# Change '核心数据模型' is valid

mvn -f server/pom.xml test
# Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS

npm --prefix client run test:run
# Test Files 5 passed; Tests 6 passed

npm --prefix client run build
# built successfully；保留既有 Vite 大 chunk / PURE annotation 警告

git diff --check b1d9c14..HEAD
# 无输出

git diff --name-only b1d9c14..HEAD | Select-String -Pattern '^(CLAUDE.md|skill/)'
# 无输出

$files = git diff --name-only b1d9c14..HEAD | Where-Object { Test-Path $_ -PathType Leaf }
if ($files) { Select-String -Path $files -Pattern 'BEGIN [A-Z ]*PRIVATE KEY|AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9_-]{20,}|ghp_[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{20,}|xox[baprs]-|aws_secret_access_key' -CaseSensitive }
# 无输出

git status --short
# 无输出
```

## 审查清单

- [x] OpenSpec requirements 已实现。
- [x] 只有 `APPROVED` 且 `validJson=true` 的 import item 可进入正式业务表。
- [x] 重复导入返回已有 mappings，不重复创建目标业务行。
- [x] 导入读取数据库 `import_items.raw_json`，不写入/移动/删除 `skill/archive/`。
- [x] `CLAUDE.md` 与 `skill/` 未改动。
- [x] 后端/前端测试与构建通过。
- [x] 前端只提供轻量概览、导入入口、映射状态，不生成比赛分析或下注建议。
- [x] 子代理复审 PASS。

## 已处理的审查反馈

- 修正 OpenSpec 与实现不一致：从 JPA entity/repository 改为 JdbcTemplate 查询/写入模型。
- 补充 `APPROVED + validJson=false`、未认证 API、invalid import 400 测试。
- 增强 ImportReview 列表正式库状态/映射数展示。
- 增强 ODDS `companies.markets` / `markets` / `all_books` 映射。
- 清理 `git diff --check` whitespace。

## 风险与后续建议

- 本阶段只做基础正式库与弱结构映射；球队/球员画像的复杂编辑、冲突处理 UI、舆情分析聚合应放到后续独立 OpenSpec change。
- 前端仍存在既有 Vite 大 chunk 警告；后续可通过路由级懒加载或 manualChunks 优化。
