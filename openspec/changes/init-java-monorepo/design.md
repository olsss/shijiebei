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
