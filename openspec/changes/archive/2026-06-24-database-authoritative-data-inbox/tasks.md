## 1. 数据入口与配置

- [x] 1.1 新建 `data-inbox/pending`、`data-inbox/imported`、`data-inbox/rejected`、`data-inbox/templates` 目录，并用 `.gitkeep` 保留空目录。
- [x] 1.2 将后端默认 `app.archive-path` 从 `../skill/archive` 改为 `../data-inbox/pending`，同步更新 `AppProperties` 默认值。
- [x] 1.3 将前端 JSON 审核中心默认路径从 `../skill/archive` 改为 `../data-inbox/pending`，并更新页面文案说明 JSON 只是临时投递文件。
- [x] 1.4 更新 CLI `import-json` 默认路径为 `../data-inbox/pending`。

## 2. 扫描器与导入类型

- [x] 2.1 为新 `type` 分类行为补充后端测试：扁平 `pending/*.json` 可按 `type` 识别，缺失/未知 `type` 时标记无效。
- [x] 2.2 扩展 `ImportItemType`，支持 `TEAM`、`PLAYER`、`MATCH`、`MATCH_LINEUP`、`MATCH_EVENT`、`MATCH_STATS`、`BET_PLAN`、`BET`、`POST_REVIEW`、`REVIEW_LESSON`。
- [x] 2.3 改造 `JsonArchiveScanner`：优先扫描指定目录下所有 `.json` 文件并按内部 `type` 分类，同时保留旧 archive 布局兼容逻辑。
- [x] 2.4 为扫描摘要提取和基础字段校验补充测试，覆盖 master data、business data、legacy archive 三类输入。

## 3. 核心业务映射

- [x] 3.1 为 `TEAM`、`PLAYER`、`MATCH` 导入编写失败测试，验证自然键 upsert、raw payload 保存、`import_item_mappings` 写入。
- [x] 3.2 实现 `BusinessJsonMapper` 对 `TEAM`、`PLAYER`、`MATCH` 的映射，按 `team_key`、`player_key`、`match_key` 幂等导入。
- [x] 3.3 为 `MATCH_LINEUP`、`MATCH_EVENT`、`MATCH_STATS` 导入编写失败测试，验证能解析 match/team/player key 并写入明细表。
- [x] 3.4 实现阵容、事件、技术统计映射；无法解析关联实体时保留 raw payload 并返回清晰映射信息或校验错误。
- [x] 3.5 为 `BET_PLAN`、`BET`、`POST_REVIEW`、`REVIEW_LESSON` 独立类型补充测试，验证不依赖旧 `ANALYSIS` 嵌套结构也能入库。
- [x] 3.6 实现补充业务类型映射，复用现有 `bet_plans`、`bet_plan_items`、`bets`、`post_match_reviews`、`review_lessons` 表。

## 4. 文件归档生命周期

- [x] 4.1 为导入成功后文件移动编写测试：源文件从 `pending` 移动到 `imported/yyyy-MM-dd`，再次扫描 pending 不重复出现。
- [x] 4.2 为驳回后文件移动编写测试：源文件从 `pending` 移动到 `rejected/yyyy-MM-dd`，驳回原因仍保留在数据库。
- [x] 4.3 实现文件归档服务，基于 `ImportItem.relativePath` 与扫描根路径定位源文件，处理重名文件时追加安全后缀。
- [x] 4.4 将归档服务接入 core import 成功路径和 reject 路径；移动失败时返回可见错误并保留数据库审计信息。

## 5. 模板与文档

- [x] 5.1 在 `data-inbox/templates` 新增 `team.json`、`player.json`、`match.json`、`odds.json`、`analysis.json`、`bet-plan.json`、`post-review.json` 模板。
- [x] 5.2 在模板中统一使用 `type`、`idempotency_key`、`source`、`payload` 外壳，并提供可替换示例值。
- [x] 5.3 更新相关测试或文档断言，确保模板 JSON 可解析且包含必需外壳字段。

## 6. 前端与可见性

- [x] 6.1 更新前端导入审核 API 类型，支持新增 `ImportItemType` 枚举值。
- [x] 6.2 更新 `ImportReviewView` 类型标签、默认路径、空状态、成功导入/驳回文案，明确数据库为权威源。
- [x] 6.3 补充前端测试，验证新增类型标签、默认路径和导入状态文案。

## 7. 验证

- [x] 7.1 运行 `openspec status --change "database-authoritative-data-inbox"`，确认工件完整。
- [x] 7.2 运行 `openspec validate database-authoritative-data-inbox --strict`，修复所有 OpenSpec 校验问题。
- [x] 7.3 运行 `mvn -f server/pom.xml test`，修复后端测试失败。
- [x] 7.4 运行 `npm --prefix client run test:run`，修复前端测试失败。
- [x] 7.5 运行 `npm --prefix client run build`，确认前端构建成功。
- [x] 7.6 手动检查 `CLAUDE.md` 保留 OpenSpec 流程规则，且未恢复旧足球分析 skill 数据目录。




