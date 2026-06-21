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
