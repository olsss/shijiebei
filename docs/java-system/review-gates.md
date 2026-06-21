# Java 系统阶段交付审查门禁

## 目的

每个 Java 系统阶段交付前，必须先完成自动验证和 Codex 自审或子代理审查。审查通过后才能向用户报告该阶段可交付。

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
7. `git status --short` 在交付时为空，或仅包含用户明确要求保留的未提交文件。

## 交付回复格式

每阶段交付回复必须包含：

```text
阶段：<change-name>
验证：<命令与结果>
审查：<审查者：Codex 或 子代理；结论>
风险：<剩余风险或“无阻塞风险”>
提交：<commit hash>
```
