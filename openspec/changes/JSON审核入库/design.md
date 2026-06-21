# JSON 审核批准入库流程设计

## Overview

本阶段建立 JSON 审核入库暂存层。后端读取 `app.archive-path` 指向的 `skill/archive/`，将 JSON 文件扫描成 `import_job` 和 `import_item` 记录。用户通过前端或 CLI 审核后，条目状态从 `PENDING_REVIEW` 变为 `APPROVED` 或 `REJECTED`。`APPROVED` 表示原始 JSON 已经进入 MySQL 暂存层，并可被后续业务模型 change 消费。

## Backend

新增包 `com.worldcup.importreview`，包含：

- `ImportJob`、`ImportItem` JPA entity。
- `ImportItemStatus` 和 `ImportItemType` enum。
- `ImportJobRepository`、`ImportItemRepository`。
- `JsonArchiveScanner`：扫描 `bets.json`、`analysis/*.json`、`odds/*.json`、`sources/*.json`。
- `JsonImportReviewService`：创建扫描任务、校验条目、状态流转、审计记录。
- `ImportReviewController`：提供 REST API。
- `ImportJsonCommandLineRunner`：提供 CLI dry-run/approve。

## Data Model

`import_jobs` 记录扫描批次：archive path、状态、文件数、有效数、无效数、创建时间。

`import_items` 记录单个 JSON 条目：job id、类型、相对路径、SHA-256、摘要标题、校验状态、校验消息、审核状态、原始 JSON、驳回原因、审核时间。

阶段内不建立最终业务表，避免在核心数据模型尚未确认前过早固化字段。

## API

所有 API 除健康检查外继续要求管理员认证：

- `POST /api/import-jobs/scan`
- `GET /api/import-jobs/{jobId}`
- `GET /api/import-items?status=&type=`
- `GET /api/import-items/{itemId}`
- `POST /api/import-items/{itemId}/approve`
- `POST /api/import-items/{itemId}/reject`
- `POST /api/import-items/batch-approve`

## Frontend

新增路由 `/import-review`，页面名“JSON 审核中心”。页面能力：

- 显示扫描按钮。
- 展示最新扫描任务摘要。
- 按状态和类型筛选条目。
- 展示条目摘要、相对路径、校验状态、审核状态。
- 展示原始 JSON 预览。
- 支持批准、批量批准、驳回。

## CLI

命令示例：

```powershell
java -jar server/target/worldcup-management-server-0.1.0-SNAPSHOT.jar import-json --path ../skill/archive --dry-run
java -jar server/target/worldcup-management-server-0.1.0-SNAPSHOT.jar import-json --path ../skill/archive --approve
```

`--dry-run` 只扫描和输出摘要，不写入数据库；`--approve` 扫描后写入数据库并把有效条目标记为 `APPROVED`。

## Safety Boundary

本阶段只读取 JSON 文件，不修改 `skill/archive/`。审批流不会生成比赛结论，也不会自动推荐下注。

