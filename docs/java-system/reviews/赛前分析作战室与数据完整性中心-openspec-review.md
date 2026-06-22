# 赛前分析作战室与数据完整性中心 OpenSpec 审查报告

日期：2026-06-22（北京时间）
分支：`feature/prematch-workbench-integrity`
审查者：Codex

## 审查对象

- `openspec/changes/赛前分析作战室与数据完整性中心/proposal.md`
- `openspec/changes/赛前分析作战室与数据完整性中心/design.md`
- `openspec/changes/赛前分析作战室与数据完整性中心/tasks.md`
- `openspec/changes/赛前分析作战室与数据完整性中心/specs/java-system/spec.md`
- `docs/superpowers/specs/2026-06-22-赛前分析作战室与数据完整性中心-design.md`
- `docs/superpowers/plans/2026-06-22-赛前分析作战室与数据完整性中心.md`

## 结论

通过。文档范围聚焦方案 A：赛前分析作战室 + 数据完整性中心。设计明确只聚合现有正式业务库数据，不新增采集器、不新增数据库表、不生成新下注推荐。

## 范围审查

- 后端范围：新增 `prematchworkbench` 只读查询 API 与完整性规则。
- 前端范围：新增 `/prematch-workbench` 页面、API helper、路由和 Dashboard 入口。
- 完整性检查：覆盖球队画像、球员画像、阵容、赔率、live 赔率时效、舆情、分析报告、AI 下注方案、多源证据和未解决冲突。
- 边界：完整性分数只表示数据准备程度，不表示胜率、投注价值或下注置信度。

## 保护文件审查

本阶段文档设计不要求修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 或 `skill/archive/`。

## 后续执行要求

- 代码阶段必须先写失败测试，再实现后端和前端。
- 后端测试必须覆盖认证、列表、详情、完整性检查和未知比赛 404。
- 前端测试必须覆盖 API path helper 与 `/prematch-workbench` 路由。
- 代码完成后必须生成代码审查报告并通过完整验证门禁。
