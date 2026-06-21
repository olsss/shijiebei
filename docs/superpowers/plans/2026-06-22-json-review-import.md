# JSON 审核批准入库流程 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 JSON 审核入库暂存层：扫描 `skill/archive/`，生成可审核 import job/items，支持校验、批准、批量批准、驳回、CLI dry-run/approve，并提供前端 JSON 审核中心。

**Architecture:** 后端新增 `importreview` 聚合，使用 JPA + Flyway 存储 `import_jobs/import_items`；扫描器只读取 JSON，不修改 `skill/`。前端新增 `/import-review` 页面和 API helper。审批结果先进入 MySQL 暂存表，不拆入最终业务表。

**Tech Stack:** Java 17, Spring Boot 3, Spring Data JPA, Flyway, Jackson, MockMvc, Vue3, TypeScript, Element Plus, Pinia, Vitest, OpenSpec.

---

## Scope Boundary

本阶段实现 OpenSpec change `json-review-import`。不修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/`、`skill/archive/`，不生成比赛结论，不把 JSON 拆入球队/球员/比赛/下注最终业务表。

## File Structure

### Backend

- Create: `server/src/main/resources/db/migration/V2__create_import_review_tables.sql`
- Create: `server/src/main/java/com/worldcup/importreview/domain/ImportItemType.java`
- Create: `server/src/main/java/com/worldcup/importreview/domain/ImportItemStatus.java`
- Create: `server/src/main/java/com/worldcup/importreview/domain/ImportJob.java`
- Create: `server/src/main/java/com/worldcup/importreview/domain/ImportItem.java`
- Create: `server/src/main/java/com/worldcup/importreview/repo/ImportJobRepository.java`
- Create: `server/src/main/java/com/worldcup/importreview/repo/ImportItemRepository.java`
- Create: `server/src/main/java/com/worldcup/importreview/service/ArchiveScanCandidate.java`
- Create: `server/src/main/java/com/worldcup/importreview/service/ArchiveScanResult.java`
- Create: `server/src/main/java/com/worldcup/importreview/service/JsonArchiveScanner.java`
- Create: `server/src/main/java/com/worldcup/importreview/service/JsonImportReviewService.java`
- Create: `server/src/main/java/com/worldcup/importreview/api/ImportReviewController.java`
- Create: `server/src/main/java/com/worldcup/importreview/api/dto/*.java`
- Create: `server/src/main/java/com/worldcup/importreview/cli/ImportJsonCommandLineRunner.java`
- Create: `server/src/test/java/com/worldcup/importreview/service/JsonArchiveScannerTest.java`
- Create: `server/src/test/java/com/worldcup/importreview/api/ImportReviewControllerTest.java`
- Create: `server/src/test/java/com/worldcup/importreview/cli/ImportJsonCommandLineRunnerTest.java`

### Frontend

- Modify: `client/src/router/index.ts`
- Modify: `client/src/views/DashboardView.vue`
- Create: `client/src/api/importReview.ts`
- Create: `client/src/views/ImportReviewView.vue`
- Create: `client/src/__tests__/import-review-api.test.ts`
- Modify: `client/src/__tests__/router.test.ts`

### Docs / OpenSpec

- Modify: `openspec/changes/json-review-import/tasks.md`
- Create: `docs/java-system/reviews/json-review-import-review.md`

---

### Task 1: Backend Database and Domain Model

**Files:**
- Create migration and entity/repository files listed above.

- [ ] **Step 1: Write repository/entity tests through API first**

Create `ImportReviewControllerTest` with tests that will initially fail:

```java
mockMvc.perform(post("/api/import-jobs/scan").with(httpBasic("admin", "admin123456")))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.totalItems").isNumber());

mockMvc.perform(get("/api/import-items").with(httpBasic("admin", "admin123456")))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data").isArray());
```

Run:

```powershell
mvn -f server/pom.xml test "-Dtest=ImportReviewControllerTest"
```

Expected: fails because import review API does not exist.

- [ ] **Step 2: Add Flyway migration**

Create `V2__create_import_review_tables.sql` with:

```sql
CREATE TABLE import_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    archive_path VARCHAR(1000) NOT NULL,
    status VARCHAR(40) NOT NULL,
    total_items INT NOT NULL DEFAULT 0,
    valid_items INT NOT NULL DEFAULT 0,
    invalid_items INT NOT NULL DEFAULT 0,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    item_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    relative_path VARCHAR(1000) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    summary_title VARCHAR(500) NOT NULL,
    valid_json BOOLEAN NOT NULL,
    validation_message VARCHAR(1000) NOT NULL,
    raw_json LONGTEXT NOT NULL,
    rejection_reason VARCHAR(1000),
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_items_job FOREIGN KEY (job_id) REFERENCES import_jobs(id),
    CONSTRAINT uk_import_items_sha_path UNIQUE (sha256, relative_path)
);
```

- [ ] **Step 3: Implement enums, entities, repositories**

Enums:

```java
public enum ImportItemType { BETS, ANALYSIS, ODDS, SOURCE }
public enum ImportItemStatus { PENDING_REVIEW, APPROVED, REJECTED }
```

Entities must map all columns above, use `GenerationType.IDENTITY`, `@ManyToOne(fetch = FetchType.LAZY)` from item to job, and initialize timestamps with `OffsetDateTime.now()`.

Repositories:

```java
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {}
public interface ImportItemRepository extends JpaRepository<ImportItem, Long> {
    List<ImportItem> findByStatus(ImportItemStatus status);
    List<ImportItem> findByItemType(ImportItemType itemType);
    List<ImportItem> findByStatusAndItemType(ImportItemStatus status, ImportItemType itemType);
}
```

- [ ] **Step 4: Run migration/context tests**

Run:

```powershell
mvn -f server/pom.xml test "-Dtest=WorldCupApplicationTests"
```

Expected: `BUILD SUCCESS`.

---

### Task 2: Backend JSON Scanner and Review Service

**Files:**
- Create scanner/service DTO files.
- Create scanner/service tests.

- [ ] **Step 1: Write scanner tests first**

Create a temp archive with:

```text
bets.json
analysis/match.json
odds/match.json
sources/source.json
analysis/bad.json
```

Test expectations:

- scanner returns 5 candidates.
- types are `BETS`, `ANALYSIS`, `ODDS`, `SOURCE`, `ANALYSIS`.
- invalid JSON candidate has `validJson=false`.
- valid candidates have SHA-256 and summary.

Run:

```powershell
mvn -f server/pom.xml test "-Dtest=JsonArchiveScannerTest"
```

Expected: fails because scanner does not exist.

- [ ] **Step 2: Implement scanner**

`JsonArchiveScanner.scan(Path archivePath)` shall:

1. Include `bets.json` if present.
2. Include `analysis/*.json`, excluding `_模板.json`.
3. Include `odds/*.json`, excluding `_模板.json`.
4. Include `sources/*.json`, excluding `_模板.json`.
5. Read raw content as UTF-8.
6. Parse with Jackson `ObjectMapper`.
7. Extract summary:
   - BETS: `bets.json`
   - ANALYSIS: `id` or `match`
   - ODDS: `event_id`, `match`, or filename
   - SOURCE: `id`, `match`, or filename
8. Store validation message.

- [ ] **Step 3: Implement review service**

`JsonImportReviewService` shall expose:

```java
ImportJobResponse scanAndPersist(Path archivePath);
List<ImportItemResponse> listItems(ImportItemStatus status, ImportItemType type);
ImportItemDetailResponse getItem(long itemId);
ImportItemResponse approve(long itemId, String actor);
List<ImportItemResponse> batchApprove(List<Long> itemIds, String actor);
ImportItemResponse reject(long itemId, String reason, String actor);
ArchiveScanResult dryRun(Path archivePath);
ImportJobResponse approveRun(Path archivePath, String actor);
```

Approval must reject invalid JSON with `ResponseStatusException(HttpStatus.BAD_REQUEST, ...)`.

- [ ] **Step 4: Run scanner/service tests**

Run:

```powershell
mvn -f server/pom.xml test "-Dtest=JsonArchiveScannerTest"
```

Expected: `BUILD SUCCESS`.

---

### Task 3: Backend REST API and CLI

**Files:**
- Create `ImportReviewController`.
- Create DTOs.
- Create CLI runner.
- Create controller/CLI tests.

- [ ] **Step 1: Implement DTOs**

DTOs:

```java
record ScanArchiveRequest(String archivePath) {}
record RejectImportItemRequest(String reason) {}
record BatchApproveRequest(List<Long> itemIds) {}
record ImportJobResponse(Long id, String archivePath, String status, int totalItems, int validItems, int invalidItems, String message) {}
record ImportItemResponse(Long id, Long jobId, String itemType, String status, String relativePath, String sha256, String summaryTitle, boolean validJson, String validationMessage) {}
record ImportItemDetailResponse(ImportItemResponse item, String rawJson, String rejectionReason) {}
```

- [ ] **Step 2: Implement REST controller**

Routes:

```text
POST /api/import-jobs/scan
GET /api/import-jobs/{jobId}
GET /api/import-items?status=&type=
GET /api/import-items/{itemId}
POST /api/import-items/{itemId}/approve
POST /api/import-items/{itemId}/reject
POST /api/import-items/batch-approve
```

Use `ApiResponse.ok(...)` for successful responses and HTTP Basic admin security inherited from existing config.

- [ ] **Step 3: Implement CLI runner**

When args contain `import-json`:

- parse `--path`, `--dry-run`, `--approve`.
- dry-run prints total/valid/invalid and exits without persistence.
- approve persists a job and marks valid items `APPROVED`.
- unsupported mode prints clear error and exits non-zero.

- [ ] **Step 4: Run API and CLI tests**

Run:

```powershell
mvn -f server/pom.xml test "-Dtest=ImportReviewControllerTest,ImportJsonCommandLineRunnerTest"
```

Expected: `BUILD SUCCESS`.

---

### Task 4: Frontend JSON Review Center

**Files:**
- Modify router/dashboard.
- Create API helper and page.
- Update tests.

- [ ] **Step 1: Update tests first**

Extend router test to expect `/import-review`.

Create `import-review-api.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { buildImportItemPath } from '@/api/importReview';

describe('import review api helpers', () => {
  it('builds item detail path', () => {
    expect(buildImportItemPath(12)).toBe('/import-items/12');
  });
});
```

Run:

```powershell
npm --prefix client run test:run
```

Expected: fails before API/helper route exists.

- [ ] **Step 2: Implement API helper**

Create `client/src/api/importReview.ts` with:

```ts
export function buildImportItemPath(id: number): string {
  return `/import-items/${id}`;
}
```

Also export `scanArchive`, `listImportItems`, `getImportItem`, `approveImportItem`, `batchApproveImportItems`, `rejectImportItem` using existing `http` and Basic Auth header.

- [ ] **Step 3: Implement page and route**

Add route:

```ts
{ path: '/import-review', name: 'import-review', component: ImportReviewView }
```

`ImportReviewView.vue` must include:

- scan button.
- table for import items.
- status/type filters.
- raw JSON drawer.
- approve/reject buttons.
- empty state.

Update dashboard module list to include JSON 审核中心 linking to `/import-review`.

- [ ] **Step 4: Run frontend tests and build**

Run:

```powershell
npm --prefix client run test:run
npm --prefix client run build
```

Expected: tests and build pass.

---

### Task 5: Final Verification and Review

**Files:**
- Modify `openspec/changes/json-review-import/tasks.md`.
- Create `docs/java-system/reviews/json-review-import-review.md`.

- [ ] **Step 1: Run full verification**

Run:

```powershell
openspec validate json-review-import --strict
mvn -f server/pom.xml test
npm --prefix client run test:run
npm --prefix client run build
git status --short
```

Expected: all commands pass and git status is clean after commits.

- [ ] **Step 2: Protected file check**

Run:

```powershell
git diff --name-only aaa1d3a..HEAD | Select-String -Pattern '^(CLAUDE.md|skill/)'
```

Expected: no output.

- [ ] **Step 3: Review**

Codex or a subagent must verify:

1. OpenSpec requirements are implemented.
2. Scanner reads but never modifies `skill/archive/`.
3. Approval writes only to import staging tables.
4. Invalid JSON cannot be approved.
5. CLI dry-run does not persist data.
6. Tests and builds passed.

- [ ] **Step 4: Commit review record and deliver stage report**

Create review record with command evidence, risks, and conclusion. Then commit:

```powershell
git add docs/java-system/reviews/json-review-import-review.md openspec/changes/json-review-import/tasks.md
git commit -m "docs: 记录JSON审核入库阶段审查结果"
```

Delivery report must include phase, verification, review, risks, commits, and next phase.

---

## Self-Review Checklist

- [ ] Plan scope matches `json-review-import` OpenSpec change.
- [ ] No task modifies `CLAUDE.md` or `skill/`.
- [ ] Scanner is read-only.
- [ ] Approval means MySQL staging import, not final business mapping.
- [ ] Review gate is mandatory before delivery.
