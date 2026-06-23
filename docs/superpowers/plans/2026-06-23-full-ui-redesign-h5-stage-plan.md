# Full UI Redesign H5 Stage Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the remaining frontend redesign from the approved design spec: responsive tournament command shell, migrated public pages, protected admin area, and H5 verification.

**Architecture:** Build the UI in stages. First add global design tokens, AppShell, mobile tabbar, route skeleton, and admin gating. Then migrate each domain page onto public APIs and responsive components while keeping legacy functionality available through admin routes or redirects.

**Tech Stack:** Vue 3, Vue Router, Pinia, Element Plus, Vitest, Vue Test Utils, Vite, Spring Boot public/admin APIs already added in the foundation phase.

---

## Stage 1: UI foundation and route skeleton

**Files:**
- Create: `client/src/layout/AppShell.vue`
- Create: `client/src/layout/MobileTabbar.vue`
- Create: `client/src/components/common/ReadonlyNotice.vue`
- Modify: `client/src/App.vue`
- Modify: `client/src/router/index.ts`
- Modify: `client/src/styles/main.css`
- Test: `client/src/__tests__/app-shell.test.ts`
- Test: `client/src/__tests__/router.test.ts`

- [x] Step 1: Write failing tests for new routes, redirects, admin metadata, AppShell visibility, mobile tabbar, and readonly/admin notices.
- [x] Step 2: Run focused tests and confirm they fail for missing shell/routes.
- [x] Step 3: Add semantic color/type/spacing tokens, remove desktop-only `min-width`, and add responsive base styles.
- [x] Step 4: Implement AppShell with desktop sidebar, top status bar, skip link, main landmark, and admin link visibility based on auth store.
- [x] Step 5: Implement MobileTabbar with the 5 documented H5 sections: overview, workbench, evidence, decisions, more/admin.
- [x] Step 6: Implement ReadonlyNotice for anonymous read-only mode and admin writable mode.
- [x] Step 7: Update router to new paths and old-route redirects: `/workbench`, `/evidence/*`, `/decisions`, `/admin/*`, while preserving `/login`.
- [x] Step 8: Run focused tests, full frontend tests, and frontend build.
- [x] Step 9: Commit stage and request subagent code review.

## Stage 2: Public overview homepage

**Files:**
- Replace: `client/src/views/DashboardView.vue`
- Possibly create: `client/src/components/overview/*`
- Test: `client/src/__tests__/dashboard-view.test.ts`

- [x] Build a public immersive overview homepage backed by `fetchPublicOverview()`.
- [x] Show public KPIs, upcoming matches, risk counters, integrity counters, odds freshness, and stage entry cards.
- [x] Keep admin todos off the public homepage.
- [x] Verify 375px no-horizontal-scroll behavior with an automated DOM check where feasible.
- [x] Commit and request subagent review.

## Stage 3: Prematch workbench migration

**Files:**
- Modify: `client/src/views/PrematchWorkbenchView.vue`
- Possibly create: `client/src/components/workbench/*`
- Test: existing and new prematch view tests.

- [x] Convert workbench page to responsive decision-flow layout using public prematch APIs by default.
- [x] Keep admin-only actions gated behind Basic admin auth.
- [x] Render tables as cards on H5 where appropriate.
- [x] Commit and request subagent review.

## Stage 4: Evidence center pages

**Files:**
- Modify: matches, odds, sentiment, team profiles, player profiles views.
- Possibly create: `client/src/components/evidence/*`
- Test: per-domain view tests and API tests.

- [x] Migrate evidence pages to new public route groups and responsive page layout.
- [x] Keep all previous read functionality through public sanitized DTOs.
- [x] Ensure raw payload / ticket / stake / approval metadata never renders in public UI.
- [x] Commit and request subagent review.

## Stage 5: Decisions and review page

**Files:**
- Modify: `client/src/views/AnalysisReviewCenterView.vue`
- Possibly create: `client/src/components/decisions/*`
- Test: decisions view/API tests.

- [x] Public decisions page shows report summaries and review lessons from public decisions APIs.
- [x] Admin-sensitive bet plans, tickets, stake, profit/loss details remain admin-only.
- [x] Commit and request subagent review.

## Stage 6: Admin backend pages

**Files:**
- Modify: `client/src/views/ImportReviewView.vue`
- Modify: `client/src/views/SystemSettingsView.vue`
- Add collection review surface if backend endpoints are already available.
- Test: admin route guard and import review tests.

- [x] Move JSON review center under `/admin/import-review` and keep it admin-only.
- [x] Preserve scan, approve, reject, batch approve, mappings, and import-to-core functions.
- [x] Show readonly/no-permission states for non-admins.
- [x] Commit and request subagent review.

## Stage 7: Final H5, accessibility, and full verification

**Files:**
- Cross-cutting CSS/tests/docs.

- [x] Verify 375, 768, 1024, 1440 layouts.
- [x] Verify keyboard focus, landmarks, aria-current, skip link, form labels, and visible focus states.
- [x] Run full frontend tests, frontend build, backend tests, and route smoke checks.
- [x] Request final subagent review before merge.

## Post-review closure evidence

- [x] Public sanitizer redacts ROI / CLV / closing-odds free text in backend DTO mapping and in the prematch workbench UI fallback path.
- [x] Edge/Playwright smoke covered `/`, `/workbench`, `/decisions`, `/admin/import-review` at 375, 768, 1024, and 1440 px with zero horizontal overflow. 375/768 showed mobile tabbar and hidden sidebar; 1024/1440 showed desktop sidebar and hidden mobile tabbar. Anonymous `/admin/import-review` redirected to `/login?redirect=/admin/import-review`. Raw evidence: `docs/superpowers/reports/2026-06-23-h5-viewport-smoke.json`.
