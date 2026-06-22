# 比赛中心与赛程上下文代码审查报告

审查日期：2026-06-22（北京时间）
审查方式：Codex 本地审查（diff、OpenSpec、后端、前端、规则边界与验证证据）
分支：`feature/match-center`

## 审查范围

- OpenSpec change：`openspec/changes/比赛中心与赛程上下文/`
- 设计与计划：
  - `docs/superpowers/specs/2026-06-22-比赛中心与赛程上下文-design.md`
  - `docs/superpowers/plans/2026-06-22-比赛中心与赛程上下文.md`
- 后端：
  - `server/src/main/java/com/worldcup/matchcenter/api/MatchCenterController.java`
  - `server/src/main/java/com/worldcup/matchcenter/api/dto/MatchCenterDtos.java`
  - `server/src/main/java/com/worldcup/matchcenter/service/MatchCenterQueryService.java`
  - `server/src/test/java/com/worldcup/matchcenter/api/MatchCenterControllerTest.java`
- 前端：
  - `client/src/api/matches.ts`
  - `client/src/views/MatchCenterView.vue`
  - `client/src/router/index.ts`
  - `client/src/views/DashboardView.vue`
  - `client/src/__tests__/matches-api.test.ts`
  - `client/src/__tests__/router.test.ts`

## 需求符合性

- 已使用中文 OpenSpec change 名称：`比赛中心与赛程上下文`。
- API 覆盖比赛列表、比赛详情、阵容、事件、球队统计、球员统计。
- 详情聚合外部因素、证据链和数据冲突。
- 前端已新增 `/matches` 比赛中心页面，并从 Dashboard 接入。
- 未新增数据库迁移，复用现有 V3/V4 表。
- 未修改 `CLAUDE.md`、`skill/`。
- 未实现下注建议、资金分配或比赛结论生成。

## TDD 证据

- 后端测试先红：`/api/matches` 缺实现时返回 404，`MatchCenterControllerTest` 失败。
- 后端实现后目标测试通过：`MatchCenterControllerTest` 5 tests / 0 failures。
- 前端测试先红：缺 `@/api/matches` 且路由缺 `/matches`。
- 前端实现后测试通过：7 files / 9 tests。

## 审查发现与处理

### 已处理

1. 前端统计文案“待核冲突”与后端 `conflictCount`（全部冲突数）语义不完全一致。
   - 处理：改为“数据冲突”。

### 未发现阻塞问题

- 后端查询全部使用参数绑定，无字符串拼接输入。
- 子资源接口对不存在比赛返回 404，详情接口也返回 404。
- 所有 `/api/matches/**` 走现有 Spring Security 认证。
- 前端 API 路径与后端 `/api/matches` 前缀匹配。
- 页面只展示正式业务库数据，不绕过 JSON 审核入库链路。

## 验证记录

最终验证命令将在审查报告提交前后重新运行并以终端输出为准：

- `openspec validate 比赛中心与赛程上下文 --strict`
- `mvn -f server/pom.xml test`
- `npm run test:run`（`client/`）
- `npm run build`（`client/`）
- `git diff --check`
- protected diff 检查：无 `CLAUDE.md` 与 `skill/` 修改

## 结论

本阶段实现与 OpenSpec 设计一致，审查未发现阻塞项。建议合并后下一阶段进入“赔率中心/分析报告中心”之一，由新的中文 OpenSpec change 管理。
