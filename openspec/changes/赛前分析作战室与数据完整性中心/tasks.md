# Tasks: 赛前分析作战室与数据完整性中心

## 1. OpenSpec 与文档审查

- [x] 创建中文 change 目录与 proposal、design、tasks、spec delta。
- [x] 编写 `docs/superpowers/specs/2026-06-22-赛前分析作战室与数据完整性中心-design.md`。
- [x] 编写 `docs/superpowers/plans/2026-06-22-赛前分析作战室与数据完整性中心.md`。
- [x] 执行 `openspec validate 赛前分析作战室与数据完整性中心 --strict`。
- [x] 执行文档未完成标记与一致性审查。
- [x] 生成 `docs/java-system/reviews/赛前分析作战室与数据完整性中心-openspec-review.md`。

## 2. 后端 API 与完整性规则

- [x] 新增 `PrematchWorkbenchControllerTest` 并先确认失败。
- [x] 实现 `PrematchWorkbenchDtos`。
- [x] 实现 `PrematchWorkbenchQueryService`，聚合比赛、球队、球员、阵容、赔率、舆情、分析报告、AI 下注方案和实际下注。
- [x] 实现完整性检查规则与分数计算。
- [x] 实现 `PrematchWorkbenchController`。
- [x] 覆盖认证、比赛列表、作战室详情、完整性子接口和未知比赛 404。
- [x] 后端目标测试和全量测试通过。

## 3. 前端赛前作战室

- [x] 新增 `prematchWorkbench` API helper 测试并先确认失败。
- [x] 更新 router 测试要求 `/prematch-workbench` 并先确认失败。
- [x] 实现 `client/src/api/prematchWorkbench.ts`。
- [x] 实现 `client/src/views/PrematchWorkbenchView.vue`。
- [x] 更新 router 和 Dashboard 入口。
- [x] 前端测试与构建通过。

## 4. 验证与代码审查

- [x] `openspec validate 赛前分析作战室与数据完整性中心 --strict` 通过。
- [x] `mvn -f server/pom.xml test` 通过。
- [x] `npm run test:run` 在 `client/` 通过。
- [x] `npm run build` 在 `client/` 通过。
- [x] `git diff --check` 通过。
- [x] protected diff 无 `CLAUDE.md` 与 `skill/` 修改。
- [x] 生成 `docs/java-system/reviews/赛前分析作战室与数据完整性中心-review.md`。



