# 赔率中心与盘口快照

## Why

现有 `odds_snapshots` 只能保存一个市场的单个赔率值，无法满足“各个玩法的赔率都保存”的需求。竞彩和海外盘口包含胜平负、让球胜平负、比分、总进球、半全场、亚洲让球、大小球等多个玩法；每个玩法又包含多个选项。若只保存一个 `odds_value`，会丢失同一玩法下的大部分赔率，后续无法审计赔率来源、比较公司差异、记录 open/live/closing 快照或做赛后 CLV 复盘。

## What Changes

- 新增赔率市场快照表 `odds_market_snapshots`。
- 新增赔率选项快照表 `odds_selection_snapshots`。
- 改造 ODDS JSON 批准入库流程，把每个玩法和每个选项赔率结构化保存。
- 保留旧 `odds_snapshots` 写入，兼容现有概览统计和历史逻辑。
- 新增后端 `com.worldcup.oddscenter`，提供赔率总览、比赛赔率详情、公司列表、玩法列表 API。
- 新增前端 `/odds` 赔率中心页面，展示比赛、公司、玩法、盘口线、快照类型、抓取时间、全部选项赔率和原始 JSON。
- 增加后端和前端测试，覆盖结构化入库、API 查询、路由和 API helper。

## Out of Scope

- 不调用外部赔率 API，不新增密钥配置。
- 不生成投注建议、下注金额、推荐组合或比赛结论。
- 不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 或 `skill/archive/`。
- 不做高级图表和自动 CLV 计算；本阶段只保存和展示赔率事实数据。

## Review Gate

1. `openspec validate 赔率中心与盘口快照 --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm run test:run` 在 `client/` 通过。
4. `npm run build` 在 `client/` 通过。
5. `git diff --check` 通过。
6. protected diff 确认无 `CLAUDE.md` 与 `skill/` 修改。
7. 生成 `docs/java-system/reviews/赔率中心与盘口快照-review.md`。
