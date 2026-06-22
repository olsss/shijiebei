# Tasks: 舆情与外部因素中心

## 1. OpenSpec 与文档审查

- [x] 创建中文 change 目录与文档。
- [x] 编写 proposal、design、tasks、spec delta。
- [x] 编写 `docs/superpowers/specs/2026-06-22-舆情与外部因素中心-design.md`。
- [x] 编写 `docs/superpowers/plans/2026-06-22-舆情与外部因素中心.md`。
- [x] 执行 `openspec validate 舆情与外部因素中心 --strict`。
- [x] 执行文档未完成标记与一致性审查。
- [x] 生成 `docs/java-system/reviews/舆情与外部因素中心-openspec-review.md`。

## 2. 数据模型与 SOURCE 入库

- [x] 新增 SOURCE 舆情入库测试并先确认失败。
- [x] 新增 V6 迁移：`match_context_factors` 与 `sentiment_risk_assessments`。
- [x] 增强 `BusinessJsonMapper.mapSource()` 保存 A1 factors 和 A2 risks。
- [x] 保持既有 evidence/conflicts/aliases 行为不变。
- [x] 目标后端入库测试通过。

## 3. 后端 API

- [x] 新增舆情中心控制器测试并先确认失败。
- [x] 实现 DTO、查询服务和控制器。
- [x] 覆盖认证、总览、比赛详情、分类字典、风险类型字典。
- [x] 目标后端 API 测试通过。

## 4. 前端

- [x] 新增 sentiment API helper 测试并更新 router 测试，先确认失败。
- [x] 实现 sentiment API helper、舆情中心页面、路由和 Dashboard 卡片。
- [x] 前端测试与构建通过。

## 5. 验证与代码审查

- [x] `openspec validate 舆情与外部因素中心 --strict` 通过。
- [x] `mvn -f server/pom.xml test` 通过。
- [x] `npm run test:run` 在 `client/` 通过。
- [x] `npm run build` 在 `client/` 通过。
- [x] `git diff --check` 通过。
- [x] protected diff 无 `CLAUDE.md` 与 `skill/` 修改。
- [x] 生成 `docs/java-system/reviews/舆情与外部因素中心-review.md`。
