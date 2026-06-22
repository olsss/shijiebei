# 球队球员采集画像

## Why

核心数据模型已经具备球队、球员、比赛、阵容、统计、证据链和冲突表，但前端还没有球队/球员画像中心，后端也没有面向画像的查询与采集审核能力。本人需要在 Java 系统中集中查看球队特性、上阵人员、首发阵容、历史进球时间点、外部因素、舆情，以及球员状态、伤病、红黄牌、更衣室情况。

## What Changes

- 新增球队/球员画像事实表与采集暂存表。
- 新增 `com.worldcup.profile` 后端聚合，提供采集项审核、球队画像查询、球员画像查询。
- 新增 API：采集任务、采集项列表、采集项批准/驳回、球队列表/详情、球队球员列表、球员列表/详情。
- 前端新增球队画像中心和球员画像中心，并把 Dashboard 模块卡片接入真实路由。
- 增加后端和前端测试，覆盖批准流、幂等、实体不存在拒绝、画像查询和 API helper。

## Out of Scope

- 不实现自动爬虫、不调用付费外部 API、不保存外部密钥。
- 不生成比赛分析结论、下注建议或资金分配。
- 不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 或 `skill/archive/`。
- 不做完整 CRUD 编辑后台；本阶段以批准采集项与画像查询为主。
- 不做后续 `skill规则同步`，该部分单独 change。

## Review Gate

本阶段交付前必须完成：

1. `openspec validate 球队球员采集画像 --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm --prefix client run test:run` 通过。
4. `npm --prefix client run build` 通过。
5. protected diff 确认 `CLAUDE.md` 与 `skill/` 未被修改。
6. 审查者确认采集项只有批准后才进入正式画像事实表。
7. 子代理或 Codex 审查通过，并生成 `docs/java-system/reviews/球队球员采集画像-review.md`。
