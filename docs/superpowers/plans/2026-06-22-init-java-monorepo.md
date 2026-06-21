# 初始化 Java 前后端分离项目结构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立当前仓库的 OpenSpec 基线、Spring Boot 3 + Java 17 后端骨架、Vue3 + Element Plus 前端骨架、MySQL/Flyway 基础配置、单管理员登录、JSON 档案路径配置，并把阶段审查门禁固化为每阶段交付前必须执行的规则。

**Architecture:** 本阶段只搭建 Java 系统地基，不替代 `skill/` 的比赛分析流程。`server/` 提供后端 API、认证、配置和数据库迁移；`client/` 提供登录、仪表盘和系统设置入口；`openspec/changes/init-java-monorepo/` 记录本阶段变更。每个阶段交付前必须通过自动验证和 Codex 或子代理审查。

**Tech Stack:** Java 17, Spring Boot 3.x, Maven, Spring Web, Spring Security, Spring Validation, Flyway, MySQL 8, H2 test runtime, Springdoc OpenAPI, Vue 3, Vite, TypeScript, Element Plus, Pinia, Vue Router, Axios, Vitest, OpenSpec.

---

## Scope Boundary

本计划只覆盖首批交付边界：

```text
OpenSpec 初始化
+ init-java-monorepo change
+ server/client 基础骨架
+ MySQL/Flyway 基础
+ 单管理员登录
+ JSON 路径配置
+ 接口文档
+ 阶段交付审查门禁
```

球队画像、球员画像、JSON 审核入库、赔率、舆情、分析报告、下注记录、复盘和高级可视化会按后续 OpenSpec change 推进。本阶段只登记这些方向，不实现业务功能。

## File Structure

- Modify: `.gitignore` — 增加 Java、Node、构建产物和本地数据库忽略规则。
- Generate: `openspec/` — 由 `openspec init --tools codex,claude` 生成。
- Create: `openspec/changes/init-java-monorepo/proposal.md` — 中文提案。
- Create: `openspec/changes/init-java-monorepo/design.md` — 中文设计。
- Create: `openspec/changes/init-java-monorepo/tasks.md` — 中文任务清单。
- Create: `openspec/changes/init-java-monorepo/specs/java-system/spec.md` — 能力规格增量。
- Create: `server/pom.xml` — Spring Boot 3 + Java 17 Maven 配置。
- Create: `server/src/main/java/com/worldcup/WorldCupApplication.java` — 后端启动类。
- Create: `server/src/main/java/com/worldcup/common/api/ApiResponse.java` — 统一响应。
- Create: `server/src/main/java/com/worldcup/config/AppProperties.java` — JSON 档案路径和管理员配置。
- Create: `server/src/main/java/com/worldcup/config/SecurityConfig.java` — 单管理员 Basic Auth。
- Create: `server/src/main/java/com/worldcup/health/HealthController.java` — 健康检查。
- Create: `server/src/main/java/com/worldcup/auth/AuthController.java` — 登录校验。
- Create: `server/src/main/java/com/worldcup/auth/LoginRequest.java` — 登录请求。
- Create: `server/src/main/java/com/worldcup/auth/LoginResponse.java` — 登录响应。
- Create: `server/src/main/java/com/worldcup/system/SystemSettingsController.java` — 系统设置。
- Create: `server/src/main/resources/application.yml` — MySQL 与应用配置。
- Create: `server/src/main/resources/db/migration/V1__init_system_tables.sql` — 系统表迁移。
- Create: `server/src/test/**` — Spring context、健康、登录、设置接口测试。
- Create: `client/package.json`, `client/vite.config.ts`, `client/tsconfig.json`, `client/index.html` — 前端工程配置。
- Create: `client/src/main.ts`, `client/src/App.vue`, `client/src/router/index.ts` — Vue 入口与路由。
- Create: `client/src/api/http.ts`, `client/src/api/system.ts` — Axios 与系统 API。
- Create: `client/src/stores/auth.ts` — 单管理员状态。
- Create: `client/src/views/LoginView.vue`, `DashboardView.vue`, `SystemSettingsView.vue` — 首批页面。
- Create: `client/src/__tests__/*.test.ts` — 路由、store、API helper 测试。
- Create: `docs/java-system/review-gates.md` — 阶段交付审查门禁。
- Read-only: `CLAUDE.md`, `skill/` — 本阶段不得修改比赛分析规则与档案。

---

### Task 1: Initialize OpenSpec Baseline

**Files:**
- Generate: `openspec/`
- Create: `openspec/changes/init-java-monorepo/proposal.md`
- Create: `openspec/changes/init-java-monorepo/design.md`
- Create: `openspec/changes/init-java-monorepo/tasks.md`
- Create: `openspec/changes/init-java-monorepo/specs/java-system/spec.md`

- [ ] **Step 1: Verify OpenSpec toolchain**

Run:

```powershell
node -v
npm -v
openspec --version
```

Expected: Node prints `v22.12.0` or newer, npm prints a version, and openspec prints a version.

- [ ] **Step 2: Upgrade OpenSpec CLI**

Run:

```powershell
npm install -g @fission-ai/openspec@latest
openspec --version
```

Expected: npm exits with code 0 and `openspec --version` prints the installed version.

- [ ] **Step 3: Initialize OpenSpec**

Run:

```powershell
openspec init --tools codex,claude
```

Expected: `openspec/` exists and OpenSpec instruction files exist for Codex and Claude.

- [ ] **Step 4: Create the change directory**

Run:

```powershell
New-Item -ItemType Directory -Force -Path 'openspec/changes/init-java-monorepo/specs/java-system'
```

Expected: `openspec/changes/init-java-monorepo/specs/java-system` exists.

- [ ] **Step 5: Write `proposal.md`**

Create `openspec/changes/init-java-monorepo/proposal.md`:

```markdown
# 初始化 Java 前后端分离项目结构

## Why

当前项目已有 `CLAUDE.md`、`skill/`、JSON 档案和本地看板，但缺少可长期维护的 Java 管理系统。需要先建立 OpenSpec、后端、前端、数据库迁移、认证和配置基础。

## What Changes

- 初始化 OpenSpec，并登记 `init-java-monorepo` 变更。
- 新增 `server/` Spring Boot 3 + Java 17 后端项目。
- 新增 `client/` Vue3 + TypeScript + Element Plus 前端项目。
- 新增 MySQL/Flyway 基础配置。
- 新增单管理员登录能力。
- 新增 JSON 档案路径配置和系统设置接口。
- 新增阶段交付审查门禁文档。
- 保留 `skill/` 比赛分析体系。

## Out of Scope

- 不实现球队画像、球员画像、赔率、舆情、下注记录和复盘业务页面。
- 不改写 `CLAUDE.md` 的比赛分析规则。
- 不导入真实 JSON 数据到 MySQL。

## Review Gate

本阶段交付前必须完成：

1. `openspec validate init-java-monorepo --strict` 通过。
2. `mvn -f server/pom.xml test` 通过。
3. `npm --prefix client run test:run` 通过。
4. `npm --prefix client run build` 通过。
5. 审查者确认 `CLAUDE.md` 与 `skill/` 未被误改。
```

- [ ] **Step 6: Write `design.md`**

Create `openspec/changes/init-java-monorepo/design.md`:

```markdown
# 初始化 Java 前后端分离项目结构设计

## Overview

本阶段创建 monorepo 的 Java 系统地基。后端位于 `server/`，前端位于 `client/`，OpenSpec change 位于 `openspec/changes/init-java-monorepo/`。现有 `skill/` 目录继续作为比赛分析、下注规则和 JSON 档案来源。

## Backend

后端使用 Java 17、Spring Boot 3、Maven、Spring Web、Spring Security、Spring Validation、Flyway、MySQL 和 Springdoc OpenAPI。测试使用 H2 数据库运行 Flyway 迁移，避免本地缺少 MySQL 时阻塞测试。

第一阶段接口：

- `GET /api/health`
- `POST /api/auth/login`
- `GET /api/system/settings`

## Frontend

前端使用 Vue3、Vite、TypeScript、Element Plus、Pinia、Vue Router、Axios 和 Vitest。第一阶段页面包括登录页、仪表盘和系统设置页。

## Safety Boundary

本阶段不得修改 `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/`、`skill/archive/` 的比赛分析内容。Java 系统只读取 JSON 档案路径配置，不生成比赛结论。
```

- [ ] **Step 7: Write spec delta**

Create `openspec/changes/init-java-monorepo/specs/java-system/spec.md`:

```markdown
# java-system Specification

## ADDED Requirements

### Requirement: Java system monorepo baseline

The project SHALL contain a Java system baseline with separate backend and frontend directories while preserving the existing `skill/` analysis system.

#### Scenario: Backend and frontend directories exist

- **WHEN** a developer lists the repository root
- **THEN** `server/` exists for Spring Boot backend code
- **AND** `client/` exists for Vue frontend code
- **AND** `skill/` remains present and unmodified by this change

### Requirement: Backend health and settings APIs

The backend SHALL expose health and system settings endpoints for the personal management system.

#### Scenario: Health endpoint responds

- **WHEN** `GET /api/health` is requested
- **THEN** the response status is 200
- **AND** the response body contains `status` equal to `UP`

#### Scenario: Settings endpoint reports archive path

- **WHEN** an authenticated administrator requests `GET /api/system/settings`
- **THEN** the response status is 200
- **AND** the response body contains the configured JSON archive path

### Requirement: Single administrator authentication

The backend SHALL support a single administrator login for personal use.

#### Scenario: Valid administrator credentials

- **WHEN** `POST /api/auth/login` is called with configured credentials
- **THEN** the response status is 200
- **AND** the response body identifies the administrator user

#### Scenario: Invalid administrator credentials

- **WHEN** `POST /api/auth/login` is called with an incorrect password
- **THEN** the response status is 401

### Requirement: Frontend shell

The frontend SHALL provide a Vue3 shell with login, dashboard, and system settings pages.

#### Scenario: Routes are registered

- **WHEN** the frontend router is created
- **THEN** routes exist for `/login`, `/`, and `/settings`

### Requirement: Review gate before delivery

Each stage delivery SHALL pass automated verification and human or subagent review before being reported as deliverable.

#### Scenario: Stage verification is complete

- **WHEN** a stage is ready for delivery
- **THEN** OpenSpec validation, backend tests, frontend tests, frontend build, and review gate checks have recorded passing evidence
```

- [ ] **Step 8: Write `tasks.md`**

Create `openspec/changes/init-java-monorepo/tasks.md`:

```markdown
# 初始化 Java 前后端分离项目结构任务

- [ ] 初始化 OpenSpec 并创建 `init-java-monorepo` change。
- [ ] 创建 Spring Boot 3 + Java 17 后端骨架。
- [ ] 创建后端健康检查、登录、系统设置接口和测试。
- [ ] 创建 Flyway 基础迁移和测试配置。
- [ ] 创建 Vue3 + TypeScript + Element Plus 前端骨架。
- [ ] 创建登录、仪表盘、系统设置页面和 Vitest 测试。
- [ ] 增加阶段交付审查门禁文档。
- [ ] 运行 OpenSpec、后端、前端验证命令。
- [ ] 完成阶段审查并记录结果。
```

- [ ] **Step 9: Validate and commit**

Run:

```powershell
openspec validate init-java-monorepo --strict
git add openspec
git commit -m "chore: 初始化OpenSpec变更"
```

Expected: validation exits with code 0 and commit succeeds.

---

### Task 2: Create Backend Skeleton and Tests

**Files:**
- Modify: `.gitignore`
- Create: `server/pom.xml`
- Create: `server/src/main/java/com/worldcup/WorldCupApplication.java`
- Create: `server/src/main/java/com/worldcup/config/AppProperties.java`
- Create: `server/src/main/java/com/worldcup/common/api/ApiResponse.java`
- Create: `server/src/main/resources/application.yml`
- Create: `server/src/main/resources/db/migration/V1__init_system_tables.sql`
- Create: `server/src/test/java/com/worldcup/WorldCupApplicationTests.java`
- Create: `server/src/test/resources/application-test.yml`

- [ ] **Step 1: Add ignore rules**

Append to `.gitignore`:

```gitignore
# Java / Maven
server/target/
*.class
*.jar
*.war

# Node / Vite
client/node_modules/
client/dist/
client/.vite/
client/coverage/

# Local runtime data
*.h2.db
*.mv.db
*.trace.db
```

- [ ] **Step 2: Create Maven project**

Create `server/pom.xml` using Spring Boot parent `3.3.6`, `java.version` 17, and dependencies:

```xml
<dependencies>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
  <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-mysql</artifactId></dependency>
  <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
  <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.6.0</version></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-configuration-processor</artifactId><optional>true</optional></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
  <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
  <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope></dependency>
</dependencies>
```

Also include `spring-boot-maven-plugin`.

- [ ] **Step 3: Write application and configuration classes**

Create `WorldCupApplication.java`:

```java
package com.worldcup;

import com.worldcup.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class WorldCupApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorldCupApplication.class, args);
    }
}
```

Create `AppProperties.java`:

```java
package com.worldcup.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @NotBlank
    private String archivePath = "../skill/archive";
    @Valid
    private Admin admin = new Admin();
    public String getArchivePath() { return archivePath; }
    public void setArchivePath(String archivePath) { this.archivePath = archivePath; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
    public static class Admin {
        @NotBlank private String username = "admin";
        @NotBlank private String password = "admin123456";
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
```

Create `ApiResponse.java`:

```java
package com.worldcup.common.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(boolean success, T data, String message, OffsetDateTime timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "ok", OffsetDateTime.now());
    }
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, OffsetDateTime.now());
    }
}
```

- [ ] **Step 4: Write backend configuration files**

Create `application.yml` with MySQL environment variables and JSON path:

```yaml
server:
  port: ${SERVER_PORT:8080}
spring:
  application:
    name: worldcup-management-server
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/worldcup_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
app:
  archive-path: ${WORLDCUP_ARCHIVE_PATH:../skill/archive}
  admin:
    username: ${WORLDCUP_ADMIN_USERNAME:admin}
    password: ${WORLDCUP_ADMIN_PASSWORD:admin123456}
springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

Create `application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:worldcup_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
app:
  archive-path: ../skill/archive
  admin:
    username: admin
    password: admin123456
```

Create `V1__init_system_tables.sql`:

```sql
CREATE TABLE system_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(120) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admin_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor VARCHAR(120) NOT NULL,
    action VARCHAR(160) NOT NULL,
    target_type VARCHAR(120) NOT NULL,
    target_id VARCHAR(160) NOT NULL,
    detail TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 5: Write and run context test**

Create `WorldCupApplicationTests.java`:

```java
package com.worldcup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WorldCupApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

Run:

```powershell
mvn -f server/pom.xml test -Dtest=WorldCupApplicationTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit backend skeleton**

Run:

```powershell
git add .gitignore server
git commit -m "feat: 添加后端基础骨架"
```

Expected: commit succeeds.

---

### Task 3: Add Backend Health, Auth, and Settings APIs

**Files:**
- Create: `server/src/main/java/com/worldcup/health/HealthController.java`
- Create: `server/src/main/java/com/worldcup/config/SecurityConfig.java`
- Create: `server/src/main/java/com/worldcup/auth/AuthController.java`
- Create: `server/src/main/java/com/worldcup/auth/LoginRequest.java`
- Create: `server/src/main/java/com/worldcup/auth/LoginResponse.java`
- Create: `server/src/main/java/com/worldcup/system/SystemSettingsController.java`
- Create: `server/src/test/java/com/worldcup/health/HealthControllerTest.java`
- Create: `server/src/test/java/com/worldcup/auth/AuthControllerTest.java`
- Create: `server/src/test/java/com/worldcup/system/SystemSettingsControllerTest.java`

- [ ] **Step 1: Write tests first**

Create tests that assert:

```java
mockMvc.perform(get("/api/health"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.status").value("UP"));

mockMvc.perform(post("/api/auth/login")
    .contentType(MediaType.APPLICATION_JSON)
    .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.username").value("admin"));

mockMvc.perform(post("/api/auth/login")
    .contentType(MediaType.APPLICATION_JSON)
    .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
    .andExpect(status().isUnauthorized());

mockMvc.perform(get("/api/system/settings"))
    .andExpect(status().isUnauthorized());

mockMvc.perform(get("/api/system/settings").with(httpBasic("admin", "admin123456")))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.archivePath").value("../skill/archive"))
    .andExpect(jsonPath("$.data.analysisSystemProtected").value(true));
```

Run:

```powershell
mvn -f server/pom.xml test -Dtest=HealthControllerTest,AuthControllerTest,SystemSettingsControllerTest
```

Expected: compilation fails before controllers are created.

- [ ] **Step 2: Implement controllers and security**

Create `HealthController.java`:

```java
package com.worldcup.health;

import com.worldcup.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }
}
```

Create `SecurityConfig.java` with `/api/health`, `/api/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` public and all other requests authenticated via HTTP Basic. Use `InMemoryUserDetailsManager` from `AppProperties`.

Create `LoginRequest.java` and `LoginResponse.java`:

```java
package com.worldcup.auth;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
```

```java
package com.worldcup.auth;
public record LoginResponse(String username, String displayName, String authType) {}
```

Create `AuthController.java` so valid configured credentials return `ApiResponse.ok(new LoginResponse("admin", "系统管理员", "basic"))` and invalid credentials return HTTP 401 with `ApiResponse.fail("用户名或密码错误")`.

Create `SystemSettingsController.java` so authenticated users get:

```java
public record SystemSettingsResponse(
    String archivePath,
    boolean analysisSystemProtected,
    String boundaryDescription
) {}
```

The response must include `archivePath` from `AppProperties`, `analysisSystemProtected=true`, and boundary text explaining that Java system does not replace `skill/`.

- [ ] **Step 3: Run backend tests and commit**

Run:

```powershell
mvn -f server/pom.xml test
git add server
git commit -m "feat: 添加后端登录健康检查与系统设置"
```

Expected: `BUILD SUCCESS` and commit succeeds.

---

### Task 4: Create Frontend Shell and Tests

**Files:**
- Create: `client/package.json`
- Create: `client/index.html`
- Create: `client/vite.config.ts`
- Create: `client/tsconfig.json`
- Create: `client/src/main.ts`
- Create: `client/src/App.vue`
- Create: `client/src/router/index.ts`
- Create: `client/src/api/http.ts`
- Create: `client/src/api/system.ts`
- Create: `client/src/stores/auth.ts`
- Create: `client/src/views/LoginView.vue`
- Create: `client/src/views/DashboardView.vue`
- Create: `client/src/views/SystemSettingsView.vue`
- Create: `client/src/styles/main.css`
- Create: `client/src/__tests__/router.test.ts`
- Create: `client/src/__tests__/auth-store.test.ts`
- Create: `client/src/__tests__/system-api.test.ts`

- [ ] **Step 1: Create package and install dependencies**

Create `client/package.json` with scripts:

```json
{
  "scripts": {
    "dev": "vite --host 127.0.0.1 --port 5173",
    "build": "vue-tsc -b && vite build",
    "test:run": "vitest run",
    "test": "vitest"
  }
}
```

Add dependencies: `vue`, `vue-router`, `pinia`, `axios`, `element-plus`, `@element-plus/icons-vue`, `echarts`. Add dev dependencies: `@vitejs/plugin-vue`, `@vue/test-utils`, `jsdom`, `typescript`, `vite`, `vitest`, `vue-tsc`.

Run:

```powershell
npm --prefix client install
```

Expected: install exits with code 0 and `client/package-lock.json` exists.

- [ ] **Step 2: Write frontend tests first**

Create `router.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { router } from '@/router';
describe('router', () => {
  it('registers login, dashboard, and settings routes', () => {
    const paths = router.getRoutes().map((route) => route.path);
    expect(paths).toContain('/login');
    expect(paths).toContain('/');
    expect(paths).toContain('/settings');
  });
});
```

Create `auth-store.test.ts`:

```ts
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '@/stores/auth';
describe('auth store', () => {
  beforeEach(() => setActivePinia(createPinia()));
  it('stores administrator identity', () => {
    const store = useAuthStore();
    store.setAdmin({ username: 'admin', displayName: '系统管理员', authType: 'basic' }, 'admin123456');
    expect(store.isAuthenticated).toBe(true);
    expect(store.basicAuthHeader).toBe('Basic YWRtaW46YWRtaW4xMjM0NTY=');
  });
});
```

Create `system-api.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { buildBasicAuthHeader } from '@/api/http';
describe('http helpers', () => {
  it('builds a basic auth header', () => {
    expect(buildBasicAuthHeader('admin', 'admin123456')).toBe('Basic YWRtaW46YWRtaW4xMjM0NTY=');
  });
});
```

Run:

```powershell
npm --prefix client run test:run
```

Expected: tests fail because router, store, and API helpers do not exist.

- [ ] **Step 3: Implement Vite/Vue shell**

Create Vite config with alias `@` to `src`, jsdom Vitest environment, and `/api` proxy to `http://127.0.0.1:8080`.

Create `main.ts` mounting Vue with Pinia, router, and Element Plus.

Create routes:

```ts
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/settings', name: 'settings', component: SystemSettingsView },
  ],
});
```

Create `buildBasicAuthHeader(username, password)` using `btoa`, and `useAuthStore()` with `setAdmin`, `logout`, `isAuthenticated`, and `basicAuthHeader`.

Create pages:

- `LoginView.vue` with Element Plus username/password form and `login()` API call.
- `DashboardView.vue` with cards for JSON 审核中心、球队画像中心、球员画像中心、赛后复盘中心.
- `SystemSettingsView.vue` that calls `/api/system/settings` with Basic Auth and displays archive path and boundary description.

- [ ] **Step 4: Run frontend verification and commit**

Run:

```powershell
npm --prefix client run test:run
npm --prefix client run build
git add client
git commit -m "feat: 添加Vue前端基础工作台"
```

Expected: Vitest passes, Vite build exits with code 0, and commit succeeds.

---

### Task 5: Add Review Gate Documentation

**Files:**
- Create: `docs/java-system/review-gates.md`
- Modify: `openspec/changes/init-java-monorepo/tasks.md`

- [ ] **Step 1: Write review-gates document**

Create `docs/java-system/review-gates.md`:

```markdown
# Java 系统阶段交付审查门禁

## 固定验证命令

在项目根目录运行：

```powershell
openspec validate <change-name> --strict
mvn -f server/pom.xml test
npm --prefix client run test:run
npm --prefix client run build
git status --short
```

## 审查清单

1. 本阶段只实现对应 OpenSpec change 的范围。
2. `CLAUDE.md`、`skill/SKILL.md`、`skill/rules/` 和 `skill/archive/` 未被误改，除非用户明确要求。
3. 后端测试覆盖新增 API、配置、数据库迁移和安全规则。
4. 前端测试覆盖新增路由、状态管理和 API 封装。
5. OpenSpec change 的 proposal、design、spec 和 tasks 与实现一致。
6. 没有硬编码真实密钥、账号之外的隐私数据或不可提交的本地路径。

## 交付回复格式

```text
阶段：<change-name>
验证：<命令与结果>
审查：<审查者：Codex 或 子代理；结论>
风险：<剩余风险或“无阻塞风险”>
提交：<commit hash>
```
```

- [ ] **Step 2: Mark OpenSpec task complete and commit**

Update `openspec/changes/init-java-monorepo/tasks.md` so the review-gate document item is checked.

Run:

```powershell
Select-String -LiteralPath 'docs/java-system/review-gates.md' -Pattern 'openspec validate','mvn -f server/pom.xml test','npm --prefix client run test:run','npm --prefix client run build','git status --short'
git add docs/java-system/review-gates.md openspec/changes/init-java-monorepo/tasks.md
git commit -m "docs: 添加阶段交付审查门禁"
```

Expected: each mandatory command appears and commit succeeds.

---

### Task 6: Final Verification and Stage Review

**Files:**
- Read: `docs/superpowers/specs/2026-06-22-java-worldcup-system-design.md`
- Read: `docs/superpowers/plans/2026-06-22-init-java-monorepo.md`
- Read: `docs/java-system/review-gates.md`
- Inspect: `openspec/changes/init-java-monorepo/`, `server/`, `client/`, `.gitignore`

- [ ] **Step 1: Run all verification commands**

Run:

```powershell
openspec validate init-java-monorepo --strict
mvn -f server/pom.xml test
npm --prefix client run test:run
npm --prefix client run build
git status --short
```

Expected: OpenSpec validation exits with code 0, Maven prints `BUILD SUCCESS`, Vitest passes, Vite build exits with code 0, and Git status is empty.

- [ ] **Step 2: Verify protected analysis files were not modified**

Run:

```powershell
git diff --name-only HEAD~6..HEAD | Select-String -Pattern '^(CLAUDE.md|skill/)'
```

Expected: no output. If the number of stage commits differs from six, replace `HEAD~6..HEAD` with the commit before implementation began.

- [ ] **Step 3: Perform required review**

Use Codex self-review or dispatch a subagent. The review must check:

```text
1. Implementation matches init-java-monorepo scope.
2. OpenSpec proposal/design/spec/tasks match implementation.
3. Backend and frontend verification commands passed.
4. Protected match-analysis files were not modified.
5. No real secrets were committed.
```

If a confirmed issue is found, fix it and rerun Step 1.

- [ ] **Step 4: Deliver stage report**

Reply using:

```text
阶段：init-java-monorepo / 初始化 Java 前后端分离项目结构
验证：OpenSpec、后端测试、前端测试、前端构建均通过
审查：Codex 或 子代理审查通过
风险：列出非阻塞风险
提交：列出本阶段 commit hash
下一步：json-review-import / JSON 审核批准入库流程
```

---

## Self-Review Checklist for This Plan

- [ ] The plan covers only the first approved delivery boundary and does not absorb unrelated subsystems.
- [ ] Every task lists exact files.
- [ ] Code-changing steps include concrete file contents or exact method contracts.
- [ ] Test steps include exact commands and expected results.
- [ ] Review-gate requirements are explicit and repeatable.
- [ ] Protected match-analysis files remain outside the implementation scope.
- [ ] OpenSpec change uses English directory name and Chinese title as approved.
