# 舆情与外部因素中心 OpenSpec 文档审查报告

日期：2026-06-22
分支：`feature/sentiment-center`
范围：`openspec/changes/舆情与外部因素中心/`、设计文档、实施计划。

## 结论

OpenSpec 文档审查通过，可以进入代码任务阶段。设计范围聚焦用户确认的 A1 + A2：

- A1：保存事实型外部因素和舆情记录。
- A2：保存风险类型、风险等级和风险分。

文档明确 Java 系统只负责保存、展示、时效提醒和风险提示，不自动给出比赛结论、下注建议或资金分配。

## 审查项目

1. OpenSpec proposal、design、tasks、spec delta 均已创建。
2. `docs/superpowers/specs/2026-06-22-舆情与外部因素中心-design.md` 已创建。
3. `docs/superpowers/plans/2026-06-22-舆情与外部因素中心.md` 已创建。
4. 数据模型覆盖 `match_context_factors` 与 `sentiment_risk_assessments`。
5. SOURCE JSON 入库策略覆盖 `external_factors/factors/sentiment_records/sentiments/risk_assessments/risks`。
6. 后端 API 范围覆盖总览、比赛详情、分类字典、风险类型字典。
7. 前端范围覆盖 `/sentiment` 页面、API helper、路由和 Dashboard 卡片。
8. 审查门明确包含 OpenSpec 校验、后端测试、前端测试、构建、diff 检查、protected diff 和代码审查报告。

## 验证证据

- `openspec validate 舆情与外部因素中心 --strict`：通过，输出 `Change '舆情与外部因素中心' is valid`。
- 文档未完成标记扫描：通过，无输出。
- `git diff --check`：通过，无输出。
- protected diff 检查：通过，无 `CLAUDE.md` 与 `skill/` 修改。

## 风险与约束

- 本阶段不调用外部 API，只导入已批准 JSON。
- 风险评分只作为提示，不作为投注建议。
- 后续代码必须按 TDD 执行：先写失败测试，再实现。
- 代码完成后必须生成独立代码审查报告。
