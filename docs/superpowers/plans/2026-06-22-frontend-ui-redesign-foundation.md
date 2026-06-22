# Frontend UI Redesign Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the security/API foundation for the full front-end redesign: public `/api/public/...` read-only DTOs, admin-only legacy rich APIs, hardened auth/API clients, and public overview/decisions endpoints.

**Architecture:** Keep existing rich controllers and query services as admin-only APIs. Add separate public controllers and DTO mappers under `com.worldcup.publicapi` so anonymous pages never reuse rich DTOs. Frontend gets split public/admin API clients before any large UI rewrite.

**Tech Stack:** Spring Boot 3 + Spring Security + MockMvc + JdbcTemplate tests; Vue 3 + Pinia + Axios + Vitest; TDD for every behavior change.

---

## Baseline Evidence

Run from worktree `.worktrees/frontend-ui-redesign-foundation` before implementing:

- `cd client && npm.cmd ci --prefer-offline --no-audit --no-fund` ->dependency install completed.
- `cd client && npm.cmd run test:run` ->13 test files / 15 tests passed.
- `cd client && npm.cmd run build` ->build passed; existing Rollup chunk-size warning only.
- `mvn -f server/pom.xml test` ->63 tests passed.

Maven note: run Maven from repository root with `-f server/pom.xml`; running inside `server/` looks for `server/.mvn/settings.xml` and fails.

## File Map

### Backend create

- `server/src/main/java/com/worldcup/config/MethodSecurityConfig.java`  - enable method-level security.
- `server/src/main/java/com/worldcup/publicapi/dto/PublicApiDtos.java`  - all public response records; no `payload`, `rawPayload`, ticket, stake, budget, review metadata fields.
- `server/src/main/java/com/worldcup/publicapi/service/PublicApiMapper.java`  - maps existing rich DTOs to public DTOs and sanitizes free text.
- `server/src/main/java/com/worldcup/publicapi/api/PublicMatchesController.java`  - `GET /api/public/matches/**`.
- `server/src/main/java/com/worldcup/publicapi/api/PublicOddsController.java`  - `GET /api/public/odds/**`.
- `server/src/main/java/com/worldcup/publicapi/api/PublicSentimentController.java`  - `GET /api/public/sentiment/**`.
- `server/src/main/java/com/worldcup/publicapi/api/PublicProfilesController.java`  - `GET /api/public/profiles/**`.
- `server/src/main/java/com/worldcup/publicapi/api/PublicPrematchWorkbenchController.java`  - `GET /api/public/prematch-workbench/**`.
- `server/src/main/java/com/worldcup/publicapi/api/PublicDecisionsController.java`  - `GET /api/public/decisions/**`.
- `server/src/main/java/com/worldcup/publicapi/api/PublicOverviewController.java`  - `GET /api/public/overview`.
- `server/src/main/java/com/worldcup/publicapi/service/PublicOverviewService.java`  - safe public overview aggregation.
- `server/src/test/java/com/worldcup/security/SecurityBoundaryTest.java`  - URL/method security matrix.
- `server/src/test/java/com/worldcup/publicapi/PublicApiSanitizationTest.java`  - forbidden field and value-level leakage tests.
- `server/src/test/java/com/worldcup/publicapi/PublicOverviewControllerTest.java`  - overview public contract.
- `server/src/test/java/com/worldcup/publicapi/PublicDecisionsControllerTest.java`  - decisions public contract.

### Backend modify

- `server/src/main/java/com/worldcup/config/SecurityConfig.java`  - method-specific public GET, admin-only writes, protected Swagger outside local/test, safe default admin handling.
- Existing rich controllers add class-level `@PreAuthorize("hasRole('ADMIN')")`:
  - `analysisreviewcenter/api/AnalysisReviewCenterController.java`
  - `coredata/api/CoreDataController.java`
  - `importreview/api/ImportReviewController.java`
  - `matchcenter/api/MatchCenterController.java`
  - `oddscenter/api/OddsCenterController.java`
  - `prematchworkbench/api/PrematchWorkbenchController.java`
  - `profile/api/ProfileController.java`
  - `sentimentcenter/api/SentimentCenterController.java`
  - `system/SystemSettingsController.java`

### Frontend create/modify

- Modify `client/src/api/http.ts`  - add `publicHttp`, `adminHttp`, `createAuthHeaders`, `isForbidden`, `isUnauthorized`.
- Modify `client/src/stores/auth.ts`  - remove default password, add `isAdmin`, `canWrite`, explicit-password-only auth.
- Create `client/src/api/publicOverview.ts`  - public overview client.
- Modify public-capable API modules to expose public functions while keeping admin rich functions:
  - `client/src/api/matches.ts`
  - `client/src/api/odds.ts`
  - `client/src/api/sentiment.ts`
  - `client/src/api/profiles.ts`
  - `client/src/api/prematchWorkbench.ts`
  - `client/src/api/analysisReview.ts`
- Modify/create tests:
  - `client/src/__tests__/auth-store.test.ts`
  - `client/src/__tests__/http-client.test.ts`
  - `client/src/__tests__/public-overview-api.test.ts`
  - existing domain API tests.

---

## Task 1: Backend security boundary and method-level safety

**Files:**
- Create: `server/src/main/java/com/worldcup/config/MethodSecurityConfig.java`
- Modify: `server/src/main/java/com/worldcup/config/SecurityConfig.java`
- Modify: rich controllers listed in File Map
- Create: `server/src/test/java/com/worldcup/security/SecurityBoundaryTest.java`

- [ ] **Step 1: Write the failing security boundary test**

Create `server/src/test/java/com/worldcup/security/SecurityBoundaryTest.java`:

```java
package com.worldcup.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityBoundaryTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void publicGetNamespaceIsAnonymousButOnlyForReadMethods() throws Exception {
        mockMvc.perform(get("/api/public/not-yet-created"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicNamespaceRejectsAnonymousWrites() throws Exception {
        mockMvc.perform(post("/api/public/overview"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/import-jobs/scan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyRichReadEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/matches")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/odds")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/sentiment")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/prematch-workbench/matches")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profiles/teams")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/core-data/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analysis-review/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/import-items")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/system/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanReadLegacyRichEndpoints() throws Exception {
        mockMvc.perform(get("/api/matches").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/odds").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/sentiment").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/prematch-workbench/matches").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/profiles/teams").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/core-data/overview").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/analysis-review/overview").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -f server/pom.xml -Dtest=SecurityBoundaryTest test
```

Expected: FAIL because the current security config does not yet permit anonymous `GET /api/public/**` and does not force public namespace writes through admin auth.

- [ ] **Step 3: Write minimal security implementation**

Create `MethodSecurityConfig.java`:

```java
package com.worldcup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
```

Modify `SecurityConfig.securityFilterChain` matcher order:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN")
                    .requestMatchers("/api/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
}
```

Add to each rich controller:

```java
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ADMIN')")
```

- [ ] **Step 4: Run focused test again**

Run:

```bash
mvn -f server/pom.xml -Dtest=SecurityBoundaryTest test
```

Expected: PASS after security matcher order and method security are implemented.

---

## Task 2: Public DTO contract and sanitization mapper

**Files:**
- Create: `server/src/main/java/com/worldcup/publicapi/dto/PublicApiDtos.java`
- Create: `server/src/main/java/com/worldcup/publicapi/service/PublicApiMapper.java`
- Create: `server/src/test/java/com/worldcup/publicapi/PublicApiSanitizationTest.java`

- [x] **Step 1: Write failing forbidden-field and value-level tests**

Create `PublicApiSanitizationTest.java`:

```java
package com.worldcup.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReport;
import com.worldcup.publicapi.service.PublicApiMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PublicApiSanitizationTest {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    PublicApiMapper mapper = new PublicApiMapper();

    @Test
    void publicDtoJsonDoesNotExposeForbiddenFieldNames() throws Exception {
        var dto = new PublicDecisionReport(
                1L, 2L, "Spain vs Brazil", LocalDate.of(2026, 6, 23), "031",
                "VALUE", "HIGH", "risk summary", "review summary", "lesson summary"
        );

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("rawJson", "rawPayload", "payload", "archivePath", "sourcePath");
        assertThat(json).doesNotContain("ticketNo", "stake", "stakeSuggestion", "budgetAmount");
        assertThat(json).doesNotContain("returnAmount", "profitLoss", "approvedBy", "reviewedBy", "reviewNote");
        assertThat(json).doesNotContain("mappings", "importItemId");
    }

    @Test
    void sanitizerRemovesSensitiveValuesFromFreeText() {
        String unsafe = "ticketNo=ABC123 stake=88 profitLoss=-20 C:/secret/archive.json {\"raw\":true} reviewedBy=admin";

        String sanitized = mapper.sanitizeText(unsafe);

        assertThat(sanitized).doesNotContain("ABC123", "stake=88", "profitLoss=-20", "C:/secret", "raw", "reviewedBy=admin");
        assertThat(sanitized).contains("[REDACTED]");
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -f server/pom.xml -Dtest=PublicApiSanitizationTest test
```

Expected: FAIL because `PublicApiDtos` and `PublicApiMapper` do not exist.

- [x] **Step 3: Implement public DTO records and sanitizer**

Create `PublicApiDtos.java` with records named in File Map. Minimum first-pass records required by tests:

```java
package com.worldcup.publicapi.dto;

import java.time.LocalDate;

public final class PublicApiDtos {
    private PublicApiDtos() {}

    public record PublicDecisionReport(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String conclusionType,
            String confidence,
            String riskSummary,
            String reviewSummary,
            String lessonSummary
    ) {}
}
```

Create `PublicApiMapper.java`:

```java
package com.worldcup.publicapi.service;

import org.springframework.stereotype.Component;

@Component
public class PublicApiMapper {
    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = value;
        sanitized = sanitized.replaceAll("(?i)ticketNo\\s*[:=]\\s*[^\\s,;]+", "ticketNo=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(stake|stakeSuggestion|budgetAmount|returnAmount|profitLoss)\\s*[:=]\\s*[-+]?[0-9]+(\\.[0-9]+)?", "$1=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(reviewedBy|approvedBy|reviewNote)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]");
        sanitized = sanitized.replaceAll("[A-Za-z]:[/\\\\][^\\s,;]+", "[REDACTED]");
        sanitized = sanitized.replaceAll("\\{[^{}]*(raw|payload)[^{}]*}", "[REDACTED]");
        return sanitized;
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -f server/pom.xml -Dtest=PublicApiSanitizationTest test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add server/src/main/java/com/worldcup/publicapi server/src/test/java/com/worldcup/publicapi/PublicApiSanitizationTest.java
git commit -m "feat: add public api sanitization contracts"
```

---

## Task 3: Public evidence read APIs

**Files:**
- Create public controllers for matches, odds, sentiment, profiles.
- Extend `PublicApiDtos.java` and `PublicApiMapper.java`.
- Update `SecurityBoundaryTest.java`.

- [ ] **Step 1: Write failing endpoint assertions**

Extend `SecurityBoundaryTest` with actual public paths introduced in this task:

```java
mockMvc.perform(get("/api/public/odds/bookmakers")).andExpect(status().isOk());
mockMvc.perform(get("/api/public/odds/markets")).andExpect(status().isOk());
mockMvc.perform(get("/api/public/sentiment/categories")).andExpect(status().isOk());
mockMvc.perform(get("/api/public/sentiment/risk-types")).andExpect(status().isOk());
mockMvc.perform(get("/api/public/profiles/players")).andExpect(status().isOk());
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -f server/pom.xml -Dtest=SecurityBoundaryTest test
```

Expected: FAIL until these public controllers exist.

- [ ] **Step 3: Implement controllers by delegating to existing query services**

Example `PublicMatchesController`:

```java
package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.matchcenter.service.MatchCenterQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/matches")
public class PublicMatchesController {
    private final MatchCenterQueryService queryService;

    public PublicMatchesController(MatchCenterQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiResponse<List<?>> matches() {
        return ApiResponse.ok(queryService.matches());
    }
}
```

Then replace `List<?>` with typed public DTOs once `PublicApiMapper` methods are added. Repeat for odds/sentiment/profiles. Do not return rich detail DTO fields named `payload`, `rawPayload`, `approvedBy`, `reviewedBy`, `reviewNote`.

- [ ] **Step 4: Add negative JSONPath tests before mapping rich details**

For every public detail endpoint added, assert:

```java
.andExpect(jsonPath("$..rawPayload").doesNotExist())
.andExpect(jsonPath("$..payload").doesNotExist())
.andExpect(jsonPath("$..approvedBy").doesNotExist())
.andExpect(jsonPath("$..reviewNote").doesNotExist())
```

- [ ] **Step 5: Run focused tests**

```bash
mvn -f server/pom.xml -Dtest=SecurityBoundaryTest,PublicApiSanitizationTest test
```

Expected: PASS for public evidence APIs.

- [ ] **Step 6: Commit**

```bash
git add server/src/main/java/com/worldcup/publicapi server/src/test/java/com/worldcup/security/SecurityBoundaryTest.java server/src/test/java/com/worldcup/publicapi
git commit -m "feat: add public evidence read apis"
```

---

## Task 4: Public prematch, decisions, and overview APIs

**Files:**
- Create `PublicPrematchWorkbenchController.java`, `PublicDecisionsController.java`, `PublicOverviewController.java`, `PublicOverviewService.java`.
- Tests: `PublicOverviewControllerTest.java`, `PublicDecisionsControllerTest.java`.

- [ ] **Step 1: Write failing overview and decisions tests**

Create `PublicOverviewControllerTest.java`:

```java
package com.worldcup.publicapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicOverviewControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void overviewIsPublicAndDoesNotExposeSensitiveFields() throws Exception {
        mockMvc.perform(get("/api/public/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedAt").exists())
                .andExpect(jsonPath("$..rawPayload").doesNotExist())
                .andExpect(jsonPath("$..payload").doesNotExist())
                .andExpect(jsonPath("$..ticketNo").doesNotExist())
                .andExpect(jsonPath("$..stake").doesNotExist())
                .andExpect(jsonPath("$..profitLoss").doesNotExist());
    }
}
```

Create `PublicDecisionsControllerTest.java`:

```java
package com.worldcup.publicapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicDecisionsControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void reportsArePublicSummariesOnly() throws Exception {
        mockMvc.perform(get("/api/public/decisions/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..rawPayload").doesNotExist())
                .andExpect(jsonPath("$..ticketNo").doesNotExist())
                .andExpect(jsonPath("$..stakeSuggestion").doesNotExist())
                .andExpect(jsonPath("$..budgetAmount").doesNotExist())
                .andExpect(jsonPath("$..profitLoss").doesNotExist());
    }
}
```

- [ ] **Step 2: Run tests to verify RED**

```bash
mvn -f server/pom.xml -Dtest=PublicOverviewControllerTest,PublicDecisionsControllerTest test
```

Expected: FAIL until controllers exist.

- [ ] **Step 3: Implement minimal public overview**

Create `PublicOverviewController` and `PublicOverviewService`. The first green implementation may return empty-safe counters:

```java
return new PublicOverviewResponse(
        LocalDateTime.now(),
        List.of(),
        new PublicRiskCounters(0, 0, 0, 0),
        new PublicIntegrityCounters(0, 0, 0),
        new PublicOddsFreshness(0, 0, 0),
        new PublicDecisionSummary(0, 0, null),
        new PublicAdminTodoCounters(0, 0)
);
```

- [ ] **Step 4: Implement minimal public decisions**

Create `PublicDecisionsController`:

```java
@RestController
@RequestMapping("/api/public/decisions")
public class PublicDecisionsController {
    @GetMapping("/reports")
    public ApiResponse<List<PublicDecisionReport>> reports() {
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/reviews")
    public ApiResponse<List<PublicDecisionReport>> reviews() {
        return ApiResponse.ok(List.of());
    }
}
```

Then map existing `AnalysisReviewCenterQueryService` summaries in the refactor step after tests are green.

- [ ] **Step 5: Run focused and full backend tests**

```bash
mvn -f server/pom.xml -Dtest=SecurityBoundaryTest,PublicOverviewControllerTest,PublicDecisionsControllerTest,PublicApiSanitizationTest test
mvn -f server/pom.xml test
```

Expected: PASS; full backend test count greater than baseline 63.

- [ ] **Step 6: Commit**

```bash
git add server/src/main/java/com/worldcup/publicapi server/src/test/java/com/worldcup/publicapi server/src/test/java/com/worldcup/security/SecurityBoundaryTest.java
git commit -m "feat: add public overview and decision apis"
```

---

## Task 5: Frontend API/auth foundation

**Files:**
- Modify `client/src/api/http.ts`
- Modify `client/src/stores/auth.ts`
- Create `client/src/api/publicOverview.ts`
- Modify existing API tests and domain API clients.

- [ ] **Step 1: Write failing HTTP/auth tests**

Create `client/src/__tests__/http-client.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { buildBasicAuthHeader, createAuthHeaders, isForbidden, isUnauthorized } from '@/api/http';

describe('http helpers', () => {
  it('builds basic auth header from explicit credentials only', () => {
    expect(buildBasicAuthHeader('admin', 'secret')).toBe(`Basic ${btoa('admin:secret')}`);
  });

  it('omits Authorization header when auth header is empty', () => {
    expect(createAuthHeaders('')).toBeUndefined();
    expect(createAuthHeaders('Basic abc')).toEqual({ Authorization: 'Basic abc' });
  });

  it('detects 401 and 403 responses', () => {
    expect(isUnauthorized({ response: { status: 401 } })).toBe(true);
    expect(isForbidden({ response: { status: 403 } })).toBe(true);
  });
});
```

Extend `auth-store.test.ts`:

```ts
it('does not keep a default admin password', () => {
  const store = useAuthStore();
  store.setAdmin({ username: 'admin', displayName: 'Admin', authType: 'BASIC' });
  expect(store.basicAuthHeader).toBe('');
  expect(store.canWrite).toBe(false);
});

it('allows writes only when admin identity and explicit password are present', () => {
  const store = useAuthStore();
  store.setAdmin({ username: 'admin', displayName: 'Admin', authType: 'BASIC' }, 'secret');
  expect(store.isAdmin).toBe(true);
  expect(store.canWrite).toBe(true);
  expect(store.basicAuthHeader).toBe(`Basic ${btoa('admin:secret')}`);
});
```

- [ ] **Step 2: Run tests to verify RED**

```bash
cd client && npm.cmd run test:run -- src/__tests__/http-client.test.ts src/__tests__/auth-store.test.ts
```

Expected: FAIL because helpers and store properties are missing and `setAdmin` defaults to `admin123456`.

- [ ] **Step 3: Implement HTTP/auth helpers**

Update `http.ts`:

```ts
import axios from 'axios';

export const http = axios.create({ baseURL: '/api', timeout: 10000 });
export const publicHttp = axios.create({ baseURL: '/api/public', timeout: 10000 });
export const adminHttp = http;

export function buildBasicAuthHeader(username: string, password: string): string {
  const token = btoa(`${username}:${password}`);
  return `Basic ${token}`;
}

export function createAuthHeaders(authHeader: string): { Authorization: string } | undefined {
  return authHeader ? { Authorization: authHeader } : undefined;
}

export function isUnauthorized(error: unknown): boolean {
  return Boolean((error as { response?: { status?: number } }).response?.status === 401);
}

export function isForbidden(error: unknown): boolean {
  return Boolean((error as { response?: { status?: number } }).response?.status === 403);
}
```

Update `auth.ts`:

```ts
const isAdmin = computed(() => admin.value?.authType === 'BASIC' && admin.value.username === 'admin');
const canWrite = computed(() => isAdmin.value && Boolean(password.value));

function setAdmin(identity: AdminIdentity, rawPassword = '') {
  admin.value = identity;
  password.value = rawPassword;
}
```

Return `isAdmin` and `canWrite`.

- [ ] **Step 4: Run tests to verify GREEN**

```bash
cd client && npm.cmd run test:run -- src/__tests__/http-client.test.ts src/__tests__/auth-store.test.ts
```

Expected: PASS.

- [ ] **Step 5: Add public overview API test and implementation**

Create `public-overview-api.test.ts`:

```ts
import { describe, expect, it, vi } from 'vitest';
import { fetchPublicOverview } from '@/api/publicOverview';
import { publicHttp } from '@/api/http';

describe('public overview api', () => {
  it('calls public overview without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValueOnce({
      data: { success: true, data: {}, message: '', timestamp: '' },
    });

    await fetchPublicOverview();

    expect(getSpy).toHaveBeenCalledWith('/overview');
  });
});
```

Create `publicOverview.ts`:

```ts
import { publicHttp } from './http';
import type { ApiResponse } from './system';

export interface PublicOverviewResponse {
  generatedAt: string;
  todayMatches: unknown[];
  riskCounters: Record<string, number>;
  integrityCounters: Record<string, number>;
  oddsFreshness: Record<string, number>;
  decisionSummary: Record<string, unknown>;
  adminTodoCounters: Record<string, number>;
}

export async function fetchPublicOverview(): Promise<ApiResponse<PublicOverviewResponse>> {
  const response = await publicHttp.get<ApiResponse<PublicOverviewResponse>>('/overview');
  return response.data;
}
```

- [ ] **Step 6: Add public functions to domain API clients**

Pattern for `matches.ts`:

```ts
import { createAuthHeaders, http, publicHttp } from './http';

export async function listPublicMatches(): Promise<ApiResponse<MatchSummary[]>> {
  const response = await publicHttp.get<ApiResponse<MatchSummary[]>>('/matches');
  return response.data;
}

export async function listMatches(authHeader: string): Promise<ApiResponse<MatchSummary[]>> {
  const response = await http.get<ApiResponse<MatchSummary[]>>('/matches', {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}
```

Repeat for odds/sentiment/profiles/prematch/analysis public endpoints. Keep admin rich APIs and auth headers intact.

- [ ] **Step 7: Run frontend verification**

```bash
cd client && npm.cmd run test:run
cd client && npm.cmd run build
```

Expected: PASS; existing chunk-size warning is acceptable.

- [ ] **Step 8: Commit**

```bash
git add client/src/api client/src/stores client/src/__tests__
git commit -m "feat: add public api client foundation"
```

---

## Task 6: Final phase verification and review handoff

**Files:** no new files unless test fixes are required.

- [ ] **Step 1: Run full verification**

```bash
cd client && npm.cmd run test:run
cd client && npm.cmd run build
mvn -f server/pom.xml test
```

Expected:

- Frontend tests pass.
- Frontend build passes; known Rollup chunk-size warning is acceptable.
- Backend tests pass; count greater than baseline 63.

- [ ] **Step 2: Inspect git diff**

```bash
git status --short
git diff --stat main...HEAD
```

Expected: only planned files changed.

- [ ] **Step 3: Request code review**

Dispatch code review with:

- What implemented: Security/API foundation for frontend redesign.
- Requirements: `docs/superpowers/specs/2026-06-22-frontend-ui-redesign-design.md` sections 7, 9, 10, 11.
- Base: `72f295a`.
- Head: branch HEAD.

- [ ] **Step 4: Fix review feedback**

Apply Critical/Important fixes with TDD. Re-run focused tests and full verification.

---

## Self-Review

### Spec coverage

- Public `/api/public/...` read-only DTOs: Tasks 2-4.
- Existing rich endpoints admin-only: Task 1.
- JSON review/import/system/analysis sensitive reads protected: Task 1 security matrix.
- Field and value-level public DTO hardening: Task 2 and Task 4 tests.
- Public overview and decisions: Task 4 backend, Task 5 frontend.
- Frontend `http.ts` / auth store split public/admin calls, no default password: Task 5.
- Old UI page migration and full H5 shell are intentionally out of this first plan; they follow after the foundation is reviewed.

### Placeholder scan

This plan uses concrete file paths, tests, commands, expected results, and commit messages throughout.

### Type consistency

Public DTO names are centralized in `PublicApiDtos`. Frontend public overview type is intentionally minimal for phase 1-5 and can be narrowed when the homepage UI task begins.

