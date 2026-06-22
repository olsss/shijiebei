# Tasks: 赔率中心与盘口快照

## 1. OpenSpec

- [x] 创建中文 change 目录与文档。
- [x] 编写 proposal、design、tasks、spec delta。
- [x] 执行 `openspec validate 赔率中心与盘口快照 --strict`。

## 2. 数据模型与入库

- [x] 新增结构化赔率入库测试并先确认失败。
- [x] 新增 V5 迁移：市场快照表与选项赔率表。
- [x] 增强 `BusinessJsonMapper.mapOdds()` 保存每个玩法和每个选项赔率。
- [x] 目标后端入库测试通过。

## 3. 后端 API

- [ ] 新增赔率中心控制器测试并先确认失败。
- [ ] 实现 DTO、查询服务和控制器。
- [ ] 目标后端 API 测试通过。

## 4. 前端

- [ ] 新增 odds API helper 测试并更新 router 测试，先确认失败。
- [ ] 实现 odds API helper、赔率中心页面、路由和 Dashboard 卡片。
- [ ] 前端测试与构建通过。

## 5. 验证与审查

- [ ] `openspec validate 赔率中心与盘口快照 --strict` 通过。
- [ ] `mvn -f server/pom.xml test` 通过。
- [ ] `npm run test:run` 在 `client/` 通过。
- [ ] `npm run build` 在 `client/` 通过。
- [ ] `git diff --check` 通过。
- [ ] protected diff 无 `CLAUDE.md` 与 `skill/` 修改。
- [ ] 生成代码审查报告。

