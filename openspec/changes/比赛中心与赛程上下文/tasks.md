# Tasks: 比赛中心与赛程上下文

## 1. OpenSpec

- [x] 创建中文 change 目录与文档。
- [x] 编写 proposal、design、tasks、spec delta。
- [x] 执行 `openspec validate 比赛中心与赛程上下文 --strict`。

## 2. 后端

- [x] 新增后端控制器测试并先确认失败。
- [x] 实现 DTO、查询服务和控制器。
- [x] 目标后端测试通过。

## 3. 前端

- [x] 新增 matches API helper 测试并更新 router 测试，先确认失败。
- [x] 实现 matches API helper、比赛中心页面、路由和 Dashboard 卡片。
- [x] 前端测试通过。

## 4. 验证与审查

- [x] `openspec validate 比赛中心与赛程上下文 --strict` 通过。
- [x] `mvn -f server/pom.xml test` 通过。
- [x] `npm run test:run` 在 `client/` 通过。
- [x] `npm run build` 在 `client/` 通过。
- [x] `git diff --check` 通过。
- [x] protected diff 无 `CLAUDE.md` 与 `skill/` 修改。
- [x] 生成代码审查报告。
