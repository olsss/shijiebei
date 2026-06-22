# 球队球员采集画像阶段审查

## 审查结论

- 结论：PASS
- 审查方式：子代理只读代码质量审查 + Codex 根据反馈修复后复验
- 审查时间：2026-06-22
- 审查范围：`球队球员采集画像` OpenSpec、后端画像采集/查询、前端球队/球员画像页面、测试与安全边界

## 子代理审查摘要

子代理原始结论为 `PASS_WITH_NOTES`，无 Critical 问题。提出的主要问题：

1. 球队详情对首发阵容、历史进球时间点、外部因素覆盖偏弱。
2. 采集项批准顺序幂等，但并发/唯一键冲突时可能返回 500。
3. 证据数统计语义可能不准。
4. 前端批准/驳回失败缺少错误提示。
5. Review Gate 与 tasks 需要同步。

## 已处理反馈

- 增加球队详情结构化聚合：
  - `lineups`：来自 `match_lineups`，含首发标记、球员名、位置。
  - `scoringPatterns`：来自 `match_team_stats`，含首球分钟与进球分钟。
  - `externalFactors`：来自 `matches.external_factors`。
  - `matchHistory`：来自 `matches + match_team_stats`。
- 更新 OpenSpec，明确球队详情必须返回结构化比赛上下文，不只依赖自由文本事实。
- 修复采集项批准唯一键冲突：若事实已由同一 `collection_item_id` 生成，返回既有事实并回填 collection item 映射。
- 调整 `evidenceCount`，把已有画像事实来源纳入证据计数，减少 source_ref 语义误导。
- 前端球队/球员画像页面批准/驳回失败时显示 `ElMessage.error`。
- 补充后端测试：采集任务创建 API、驳回 API、结构化球队详情、重复事实冲突幂等。
- `git diff --check` 已无 whitespace 警告。

## 验证证据

- `openspec validate 球队球员采集画像 --strict`：通过。
- `mvn -f server/pom.xml test`：通过，33 tests，0 failures，0 errors。
- `npm run test:run`（client）：通过，6 files / 8 tests。
- `npm run build`（client）：通过；仅保留既有 Vite/Rollup `#__PURE__` 与 chunk size 警告。
- `git diff --check`：通过，无输出。
- protected diff：未修改 `CLAUDE.md`、`skill/`。

## 安全边界

- Java 系统只管理采集暂存、人工审核和画像事实展示。
- 未修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 或 `skill/archive/`。
- 采集项必须经批准才写入 `team_profile_facts` / `player_profile_facts`。
- 未加入自动下注建议、资金分配或比赛判断逻辑。

## 后续建议

- 后续 `skill规则同步` change 再处理 Java 结构化事实与 `skill/` 分析流程的冲突。
- 可继续增加页面组件级交互测试，以及更完整的并发批准集成测试。
