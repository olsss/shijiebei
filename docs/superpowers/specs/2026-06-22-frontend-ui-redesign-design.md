# 前端整站重设计与 H5 适配设计

日期：2026-06-22  
状态：已根据首轮子代理审查修订，待二次审查  
范围：前端整体重构；允许为体验、权限边界、公开聚合数据调整后端接口/安全配置。

## 1. 背景与目标

当前前端基于 Vue 3 + Element Plus，页面已覆盖 JSON 审核、比赛中心、赔率中心、舆情中心、分析下注复盘、赛前作战室、球队画像、球员画像、系统设置等模块，但整体仍偏“功能堆叠型后台”。本次重设计目标是把系统重塑为“世界杯竞彩情报与决策指挥舱”：既有赛事沉浸感，又保留下注分析所需的数据密度、证据链、赔率细节、CLV 和复盘可读性。

核心目标：

- 采用赛事沉浸 / 品牌化视觉方向，而不是普通后台模板。
- 重新定义模块结构，但不丢失现有功能。
- 同时服务 PC 与手机 H5，不只做桌面端。
- 未登录用户只能浏览经脱敏的只读数据；管理员登录后才能执行审核、批准、入库、保存、修改、删除等写操作。
- 前端权限只是体验层；后端必须用公开白名单 + 管理端点优先匹配 + `hasRole("ADMIN")` 保护敏感读取和所有写操作。
- 保持项目下注规则：官方核对、多源证据、90 分钟口径、玩法区分、动态预算、票面 SP、赔率价值、CLV、战意、行为偏差、复盘沉淀等内容在界面层被明确呈现。
- Java 系统不自动生成下注建议、不自动加注、不自动倍投；它只展示已批准数据、已归档 AI/人工方案与风险摘要。新的分析/方案来源必须是人工录入或外部 AI JSON，经审核后入库。

## 2. 已确认决策

### 2.1 视觉方向

选择 **C：赛事沉浸 / 品牌化**。

设计约束：

- 首页和导航可体现世界杯赛事氛围。
- 业务数据页必须克制，避免过度 HUD、细线、发光、背景图干扰阅读。
- 图标必须使用一致的 SVG 图标体系，不使用 emoji 作为结构性图标。
- 所有可交互控件触控区域不小于 44px。

### 2.2 改造范围

选择 **前端整体重构**，并允许改后端：

- 可抽公共布局、设计系统组件、领域组件和 composable。
- 可重写多数页面结构与样式。
- 可调整后端安全规则、公开聚合接口、脱敏 DTO、测试。
- 不删除现有业务能力；旧路由保留 redirect 或 alias 到全量验收通过。

### 2.3 信息架构

选择 **A：五阶段决策流**。

一级区：

1. 赛事总览
2. 赛前作战
3. 证据中心
4. 决策与复盘
5. 管理后台

### 2.4 默认首页

未登录打开系统时进入 **赛事沉浸总览首页**，展示今日比赛、关键风险、完整性摘要、只读入口与管理员入口。首页公开数据来自 `/api/public/overview`，只返回脱敏摘要和计数。

### 2.5 权限边界

- 未登录：只能访问公开白名单接口返回的脱敏只读数据。
- 管理员：可执行 JSON 审核、扫描、批准、驳回、入库、保存、修改、删除、采集项批准等写动作，也可查看原始归档、rawPayload、票据、金额、入库映射等敏感数据。
- JSON 审核中心整体作为管理后台功能，不向未登录用户开放，因为它可能暴露原始归档内容和入库操作上下文。
- 球队/球员公开画像页只展示已批准事实；采集任务、采集待审项、批准/驳回入口属于管理后台或管理员态区块。

## 3. UI/UX 设计系统

### 3.1 产品类型与风格

基于 ui-ux-pro-max 设计系统查询，本项目匹配：

- Product pattern：Real-Time / Operations Landing
- Style：Data-Dense Dashboard + Real-Time Monitoring + Sports Event Immersive
- 设计关键词：实时态势、赛事氛围、数据密集、证据完整性、风险提醒、决策路径。

### 3.2 色彩 Tokens 与可访问配对

建议在 `client/src/styles/main.css` 或拆分后的 token 文件中定义语义变量：

```css
:root {
  --wc-bg: #07111f;
  --wc-bg-elevated: #0b1730;
  --wc-surface: #101b2c;
  --wc-surface-2: #16253a;
  --wc-glass: rgba(15, 23, 42, 0.88);
  --wc-border: rgba(148, 163, 184, 0.24);

  --wc-text: #eef6ff;
  --wc-text-muted: #b8c7dd;
  --wc-text-subtle: #9ca3af;
  --wc-text-disabled: #64748b;

  --wc-primary: #93c5fd;
  --wc-primary-strong: #3b82f6;
  --wc-primary-deep: #1e40af;
  --wc-accent: #d97706;
  --wc-on-accent: #111827;

  --wc-success: #86efac;
  --wc-warning: #fde68a;
  --wc-danger: #fca5a5;
  --wc-cyan: #22d3ee;

  --wc-radius-sm: 10px;
  --wc-radius-md: 14px;
  --wc-radius-lg: 22px;
  --wc-radius-xl: 30px;
  --wc-focus-ring: 0 0 0 3px rgba(217, 119, 6, 0.36);
}
```

允许的前景 / 背景组合：

| 用途 | 前景 | 背景 | 对比要求 |
|---|---|---|---|
| 正文主文本 | `--wc-text` | `--wc-bg` / `--wc-surface` / `--wc-surface-2` | AA 以上 |
| 次级文本 | `--wc-text-muted` | `--wc-bg` / `--wc-surface` / `--wc-surface-2` | AA 以上 |
| 辅助标签 | `--wc-text-subtle` | `--wc-bg` / `--wc-surface` / `--wc-surface-2` | AA 以上 |
| 禁用文本 | `--wc-text-disabled` | 深色面板 | 仅用于 disabled，不承载关键含义 |
| CTA 文字 | `--wc-on-accent` | `--wc-accent` | AA 以上 |
| 危险状态文字 | `--wc-danger` | 深色面板 | AA 以上，必须配文字说明 |
| 成功状态文字 | `--wc-success` | 深色面板 | AA 以上，必须配文字说明 |
| 警告状态文字 | `--wc-warning` | 深色面板 | AA 以上，必须配文字说明 |

禁止组合：

- `--wc-text` / `--wc-text-muted` 直接放在 `--wc-accent` 上。
- `--wc-danger`、`--wc-success`、`--wc-warning` 作为纯色背景后再放白字，除非重新测得 AA。
- 仅用红/绿/黄表达命中、过期、失败、风险等级；必须同时提供文本、图标或标签。

### 3.3 组件样式 Tokens

| 类别 | 约束 |
|---|---|
| 间距 | 使用 4/8px 节奏；页面 padding：H5 16px、平板 20px、桌面 24-32px |
| 字号 | body 16px 起；小标签不低于 12px；正文 line-height 1.5-1.75 |
| 标题 | H1 32-56px 响应式；H2 24-32px；H3 18-22px |
| 数字 | 赔率、金额、CLV 使用等宽数字或 `font-variant-numeric: tabular-nums` |
| 图标 | 16/20/24px 三档，结构图标统一 stroke 宽度；图标按钮点击区 ≥44px |
| 阴影 | 数据页使用低强度阴影；首页可以更强，但不能遮挡文字 |
| z-index | base 0、sticky 20、tabbar 40、drawer 80、modal 100、toast 120 |
| 状态 | hover/active/focus/disabled/loading 都必须有独立样式 |
| viewport | 保留 `width=device-width, initial-scale=1`，不禁用用户缩放 |

### 3.4 赛事沉浸与数据可读性边界

- 首页可使用径向渐变、赛事球形标识、轻量动态状态点。
- 数据页正文区域禁止背景图；只允许低对比渐变背景出现在页面外层。
- 数据页 `DataPanel` 背景不透明度不低于 0.88；表格区域优先纯色深色面板。
- 发光效果仅限品牌标识、当前导航、状态点，不用于正文、表格边框和长文本。
- 表格、赔率、CLV、票面 SP、风险摘要必须在实际背景上重新测对比度。

### 3.5 字体策略

- 大标题：运动感 Condensed 字体风格，例如 `Barlow Condensed`，用于首页英雄区和模块标题。
- 正文与表格：`Barlow` / `Inter` / `Microsoft YaHei` / `PingFang SC`，保证中文阅读稳定。
- 数字与赔率：`Fira Code` 或等宽数字设置，用于赔率、金额、CLV、编号、时间。
- 字体加载必须使用 `font-display: swap`；如果不引入在线字体，则使用系统字体降级。
- H5 输入框字号不低于 16px，避免 iOS 自动放大。
- 200% 文本缩放下不能截断核心状态和写操作按钮。

### 3.6 布局和响应式

采用移动优先 + 桌面增强：

- 375px：手机 H5 基准。
- 768px：平板。
- 1024px：小桌面。
- 1440px：标准桌面。

桌面：

- 左侧固定五阶段导航。
- 顶部状态栏显示北京时间、只读/管理员状态、登录入口。
- 内容区最大宽度按页面类型控制，数据页允许更宽。

手机 H5：

- 顶部显示赛事标题、当前状态、登录入口。
- 底部使用 5 项主导航：总览、赛前、证据、决策、更多。
- 管理后台放入“更多/管理员入口”，避免底部导航拥挤。
- 定义 `--mobile-tabbar-height: 64px`；主内容底部 padding 使用 `calc(var(--mobile-tabbar-height) + env(safe-area-inset-bottom))`。
- 底部导航 z-index 使用 40；Drawer 使用 80；Modal 使用 100。
- 横屏时底部导航仍不得遮挡 CTA，列表末尾必须可滚到安全区以上。
- 首页 KPI 从 4 列降为 1-2 列。
- 五阶段入口从横向流改为纵向卡片。
- 筛选器使用抽屉或折叠面板，不占满首屏。
- 禁止整页横向滚动；大表格优先转卡片列表，确需表格时只让表格容器横向滚动。

### 3.7 交互与可访问性

- 所有按钮、导航、图标按钮触控区域 ≥44px。
- hover、active、focus、disabled 状态必须明确。
- 键盘 Tab 顺序与视觉顺序一致。
- 提供跳到主内容的 skip link。
- 当前导航项使用 `aria-current="page"`。
- 图标按钮必须有可访问名称，例如 `aria-label`。
- 路由切换后焦点移动到 `main` 或页面 H1。
- Dialog/Drawer 必须支持 focus trap、Esc 关闭、关闭后焦点回到触发器。
- H5 抽屉关闭时恢复滚动状态，不丢筛选上下文。
- 加载超过 300ms 显示 skeleton 或 loading。
- loading、error、保存成功、权限失败用 `aria-live="polite"` 或等效机制提示。
- 动画控制在 150-300ms，并支持 `prefers-reduced-motion`。
- 文字对比度：正文至少 4.5:1，大号文字至少 3:1。

### 3.8 空状态、错误态、过期态

- `EmptyState` 必须区分：无数据、未登录不可操作、筛选无结果、数据尚未入库。
- 请求失败显示原因、重试按钮和可恢复路径。
- 赔率、阵容、伤停、数据文件过期必须以“过期态”展示，不能与加载失败混淆。
- Toast 不抢焦点；重要错误使用页面内 alert 区块。

## 4. 信息架构与路由

### 4.1 新路由

| 新路由 | 名称 | 访问模式 | 说明 |
|---|---|---|---|
| `/` | 赛事总览 | 公开只读 | 未登录默认首页，数据来自脱敏 overview |
| `/workbench` | 赛前作战 | 公开只读，写动作需登录 | 聚合赛前分析上下文 |
| `/evidence/matches` | 比赛与赛程 | 公开只读 | 原比赛中心，使用公开 DTO |
| `/evidence/odds` | 赔率与盘口 | 公开只读 | 原赔率中心，隐藏 rawPayload |
| `/evidence/sentiment` | 舆情与外部因素 | 公开只读 | 原舆情中心，隐藏待审详情 |
| `/evidence/teams` | 球队画像 | 公开只读 | 只展示已批准事实 |
| `/evidence/players` | 球员画像 | 公开只读 | 只展示已批准事实 |
| `/decisions` | 决策与复盘 | 公开脱敏摘要，敏感明细管理员 | 原分析下注复盘中心 |
| `/admin/import-review` | JSON 审核中心 | 管理员 | 扫描、审核、批准、驳回、入库 |
| `/admin/collection-review` | 采集审核 | 管理员 | 球队/球员采集任务、待审项、批准、驳回 |
| `/admin/settings` | 系统设置 | 管理员 | 系统配置、归档路径 |
| `/login` | 登录 | 公开 | 登录后站内回跳 |

### 4.2 旧路由兼容

| 旧路由 | 新路由 |
|---|---|
| `/matches` | `/evidence/matches` |
| `/odds` | `/evidence/odds` |
| `/sentiment` | `/evidence/sentiment` |
| `/analysis-review` | `/decisions` |
| `/prematch-workbench` | `/workbench` |
| `/profiles/teams` | `/evidence/teams` |
| `/profiles/players` | `/evidence/players` |
| `/import-review` | `/admin/import-review` |
| `/settings` | `/admin/settings` |

兼容策略：

- 第一阶段用 redirect 保持链接可用。
- 大模块迁移期间允许旧 view 保留隐藏入口，便于回滚和对比。
- 全量验收通过后再清理旧 view。

## 5. 页面设计

### 5.1 赛事总览

目的：公开只读入口，一屏进入状态。

内容：

- 今日/近期比赛英雄区。
- 官方核对、赔率时效、证据完整性、风险提醒 KPI。
- 高风险比赛队列。
- 五阶段入口卡片。
- 管理员待办摘要，但未登录仅显示数量或提示，不暴露原始 JSON 内容。
- 快捷入口：进入赛前作战、查看证据完整性、打开复盘中心。

H5：

- 英雄区压缩为单列。
- KPI 单列或双列。
- 比赛队列转为卡片。
- 首屏优先展示“今日比赛 + 高风险提醒 + 进入赛前作战”。

### 5.2 赛前作战

目的：按比赛聚合所有下注前信息，保留当前作战室的单场完整上下文。

内容：

- 比赛选择器和开赛倒计时。
- 官方核对状态：比赛编号、开赛时间、玩法、让球数、是否开售。
- HAD、HHAD、比分、TTG、HAFU、串关分别展示，不混用口径。
- 90 分钟结算口径提示，淘汰赛强化“晋级不等于 90 分钟胜”。
- 本届逐场表现摘要。
- 阵容、伤停、红黄牌、战意、48 队最佳第三名横向风险。
- 多源证据、来源冲突、数据时效与完整性检查。
- 赔率快照、Pinnacle/主流公司、票面 SP 提示、赔率过期提醒。
- 已入库分析报告、已归档 AI/人工方案、实际出票记录、CLV/票面信息入口。
- 行为偏差自检：近因、球星/牌面、大众盘从众、锚定、结果偏差。
- 展示已批准分析/AI 方案归档与风险摘要；Java 不自动生成新下注建议。

写操作：

- 保存分析草稿、导入外部 AI JSON、关联票据、更新状态等必须管理员登录。
- 新方案来源必须是人工或外部 AI JSON，经审核后入库。

H5：

- 首屏顺序：比赛选择器 → 官方核对状态 → 风险总览 → 证据完整性。
- 逐场表现、赔率、舆情、方案、票据使用折叠区。
- 关键 CTA 使用底部操作栏，避开安全区；未登录时显示“登录后保存/导入”。
- 筛选和比赛切换使用底部抽屉或顶部选择器。

### 5.3 证据中心

目的：统一承载原比赛、赔率、舆情、球队、球员模块。

桌面：使用二级导航或标签页。  
手机：使用分段控件或顶部横向滚动分类。

页面原则：

- 数据表格统一 `DataPanel` 外壳。
- 状态字段统一 `StatusBadge`。
- 原始 JSON 或 payload 不在公开页面展示；管理员明细页也默认折叠。
- 公开球队/球员画像只展示已批准事实。
- 采集任务、待审项、批准、驳回、状态筛选进入 `/admin/collection-review`。

H5：

- 分类导航横向滚动，当前项可见并使用 `aria-current`。
- 比赛、球队、球员列表使用卡片模式。
- 赔率矩阵可使用局部横向滚动，但卡片摘要必须先展示主盘口、更新时间、过期状态。
- 筛选抽屉必须包含应用、重置、已选筛选 chips。

### 5.4 决策与复盘

目的：呈现分析、投注方案、出票、赛果、复盘和规则沉淀闭环。

内容：

- 公开脱敏分析摘要：比赛、结论类型、置信度、风险摘要、复盘数量。
- 管理员分析明细：rawPayload、完整 narrative、投注方案明细。
- 管理员票据明细：ticketNo、stake、odds、closingOdds、returnAmount、profitLoss。
- 公开复盘摘要：五层复盘摘要与规则沉淀，不含个人资金明细和 rawPayload。
- ROI / CLV / 盈亏指标仅管理员完整可见；公开首页可显示聚合区间或脱敏趋势，不显示票号和个人资金。
- 赛后五层复盘：数学层、足球层、盘口层、大赛气质层、赔率价值层。
- 下注规则检查清单：90 分钟口径、玩法区分、动态预算、票面 SP、CLV、数据时效、多源冲突、战意/排名、行为偏差。

未登录可查看脱敏历史分析摘要与复盘摘要；新增、编辑、结算、保存规则、查看资金与票据明细必须管理员登录。

H5：

- 列表使用卡片，每张卡展示比赛、结论类型、风险、状态。
- 复盘五层使用折叠区，默认展开 overall summary 和赔率价值层。
- 管理员写操作集中在底部操作栏或更多菜单中，危险动作二次确认。

### 5.5 管理后台

目的：管理员专用。

包含：

- JSON 审核中心。
- 扫描归档。
- 审核详情。
- 批准入库。
- 批量批准/驳回。
- 映射结果。
- 采集审核：采集任务、待审项、批准、驳回、状态筛选。
- 系统设置。

访问规则：

- 未登录访问 `/admin/*` 自动跳转 `/login?redirect=...`。
- redirect 只允许站内相对路径，禁止 `http://`、`https://`、`//`。
- 登录成功后回到原页面。
- 后端所有管理读取与写入接口必须 `hasRole("ADMIN")`。

H5：

- 管理入口位于“更多”菜单。
- 审核列表使用卡片 + 状态筛选抽屉。
- 批准/驳回按钮固定在详情页底部操作区，避开安全区。
- 驳回原因使用可见 label 和错误提示。

## 6. 前端技术架构

### 6.1 组件分层

建议新增：

```text
client/src/components/app/
  AppShell.vue
  AppSidebar.vue
  AppTopbar.vue
  MobileTabbar.vue
  AdminGate.vue
  ReadonlyNotice.vue

client/src/components/ui/
  DataPanel.vue
  MetricCard.vue
  StatusBadge.vue
  RiskBadge.vue
  EmptyState.vue
  ActionToolbar.vue
  ResponsiveTable.vue
  FilterDrawer.vue
  SafeAreaActionBar.vue

client/src/components/home/
  EventHero.vue
  MatchSpotlight.vue
  RiskQueue.vue
  StageEntryGrid.vue

client/src/components/domain/matches/
client/src/components/domain/odds/
client/src/components/domain/sentiment/
client/src/components/domain/profiles/
client/src/components/domain/decisions/
client/src/components/domain/admin/

client/src/views/evidence/
  EvidenceMatchesView.vue
  EvidenceOddsView.vue
  EvidenceSentimentView.vue
  EvidenceTeamsView.vue
  EvidencePlayersView.vue

client/src/views/admin/
  ImportReviewAdminView.vue
  CollectionReviewAdminView.vue
  SystemSettingsAdminView.vue

client/src/composables/
  useReadonlyMode.ts
  useAdminAction.ts
  useResponsiveTable.ts
  useFilterDrawer.ts
  useRouteFocus.ts
```

### 6.2 ResponsiveTable 与筛选抽屉契约

`ResponsiveTable`：

- 小于 768px 默认卡片模式。
- 每个字段必须以 label/value 呈现。
- 赔率矩阵和宽数据表可局部横向滚动，但容器必须有可见提示和 `overflow-x: auto`，整页不能横向滚动。
- 卡片模式必须保留排序状态、筛选状态和关键状态徽章。

`FilterDrawer`：

- 包含 Apply、Reset、Close。
- 顶部展示已选筛选 chips。
- 支持 Esc/返回关闭、focus trap、关闭后焦点回到触发器。
- 关闭不丢已输入筛选；Reset 才清空。

### 6.3 状态管理

`auth` store 保留并增强：

- 管理员身份。
- Basic Auth header。
- redirect 登录回跳。
- `canWrite` / `isAdmin` 计算属性。
- 401 或 logout 时清空身份和密码。
- 不再提供默认密码兜底；登录页不预填真实默认凭据。
- 密码只保存在内存中；如果刷新后不能恢复 Basic Auth，则显示只读态并要求重新登录。
- 生产或非本机访问要求 HTTPS；本地个人环境可继续使用 Basic Auth。

### 6.4 API 客户端

- 公开读接口无 auth 参数，调用 public/read API。
- 管理读接口和写接口通过统一 helper 注入 Authorization。
- 将现有大量 `authHeader` 参数式 GET 迁移为：公开读无参；管理读/写使用 `requireAdminAuth()`。
- axios interceptor 统一处理：
  - 401：清空 auth，跳转 `/login?redirect=<current>`。
  - 403：显示权限不足，不重复登录循环。
- redirect 必须校验为站内相对路径。
- 不在每个页面手写鉴权逻辑，页面使用 `AdminGate`、`ReadonlyNotice`、`useReadonlyMode`。

### 6.5 Element Plus 使用原则

保留 Element Plus 作为基础组件库，但通过全局 CSS 统一视觉：

- 卡片透明深色面板，但数据面板不透明度不低于 0.88。
- 表格深色高对比样式。
- 按钮最小高度 44px。
- 表单 label 永远可见，不使用 placeholder 代替 label。
- Dialog/Drawer 在手机优先使用全屏或底部抽屉样式。
- Element Plus 图标按钮必须补可访问名称。

## 7. 后端权限与接口设计

### 7.1 安全原则

现有安全配置除健康检查和登录外全部要求认证。新设计不采用 “GET 默认公开”，而采用 **公开 GET 白名单 + 管理 GET 优先匹配 + 写接口默认管理员**。

Spring Security 规则顺序：

1. 先匹配 admin-only 端点，要求 `hasRole("ADMIN")`。
2. 再匹配公开白名单端点，允许匿名访问。
3. 所有 `POST / PUT / PATCH / DELETE` 默认要求 `hasRole("ADMIN")`。
4. 其它未列出的 `/api/**` 默认要求 `hasRole("ADMIN")`，避免新增敏感接口意外公开。

关键 Controller 可以使用 `@PreAuthorize("hasRole('ADMIN')")` 作为双保险。

Swagger：

- dev/local profile 可公开，便于本地联调。
- 非本机或生产部署必须关闭或要求管理员认证。

### 7.2 后端权限矩阵

| 类别 | 端点 | 未登录 | 管理员 | 说明 |
|---|---|---:|---:|---|
| 健康检查 | `GET /api/health` | 200 | 200 | 公开 |
| 登录 | `POST /api/auth/login` | 200 | 200 | 公开 |
| 公开首页 | `GET /api/public/overview` | 200 | 200 | 脱敏 DTO |
| 比赛只读 | `GET /api/matches/**` | 200 | 200 | 不返回 rawPayload |
| 赔率只读 | `GET /api/odds/**` | 200 | 200 | 不返回 rawPayload |
| 舆情只读 | `GET /api/sentiment/**` | 200 | 200 | 不返回待审详情 |
| 公开画像 | `GET /api/profiles/teams/**`、`GET /api/profiles/players/**` | 200 | 200 | 仅已批准事实 |
| 赛前作战只读 | `GET /api/prematch-workbench/**` | 200 | 200 | 若含敏感字段需脱敏 DTO |
| 决策公开摘要 | `GET /api/public/decisions/**` | 200 | 200 | 脱敏摘要，新增公开接口 |
| 核心数据概览 | `GET /api/core-data/overview` | 200 | 200 | 仅计数可公开 |
| JSON 审核读取 | `GET /api/import-jobs/**`、`GET /api/import-items/**` | 401 | 200 | raw JSON / 归档路径，管理员 |
| 入库映射 | `GET /api/core-data/import-items/**` | 401 | 200 | 管理员 |
| 采集审核 | `GET /api/profiles/collections/**` | 401 | 200 | 管理员 |
| 系统设置 | `GET /api/system/**` | 401 | 200 | 归档路径，管理员 |
| 分析/票据敏感明细 | `GET /api/analysis-review/**` | 401 或脱敏替代 | 200 | 原接口保持管理员；公开走 `/api/public/decisions/**` |
| 所有写接口 | `POST/PUT/PATCH/DELETE /api/**` | 401 | 200 | 管理员 |

如果未来添加非管理员账号：

- 未认证返回 401。
- 已认证但非管理员访问管理端点返回 403。
- 当前只有管理员账号时，测试可先覆盖 401 与管理员 200；若新增测试用户再覆盖 403。

### 7.3 公开数据脱敏规则

公开接口禁止返回：

- `rawJson`
- `rawPayload`
- `archivePath`
- `sourcePath` 或本地文件绝对路径
- `ticketNo`
- 单票 `stake`、`returnAmount`、`profitLoss`
- 入库映射详情
- 待审 JSON 详情
- 采集待审项原始内容
- 管理操作人、审核路径、内部备注

公开接口允许返回：

- 比赛摘要、队名、开赛时间、竞彩编号。
- 已批准事实的摘要字段。
- 赔率更新时间、过期状态、盘口类型、脱敏后的市场摘要。
- 风险计数、证据完整性计数、状态枚举。
- 脱敏分析摘要和复盘摘要。
- 聚合 ROI/CLV 趋势只能以区间或概览展示，不展示个人票据与资金明细。

### 7.4 新增公开聚合接口

第一版新增公开聚合接口：

```text
GET /api/public/overview
```

DTO schema：

```text
PublicOverviewResponse
  generatedAt: string
  todayMatches: PublicMatchSpotlight[]
  riskCounters: PublicRiskCounters
  integrityCounters: PublicIntegrityCounters
  oddsFreshness: PublicOddsFreshness
  decisionSummary: PublicDecisionSummary
  adminTodoCounters: PublicAdminTodoCounters

PublicMatchSpotlight
  matchId: number
  matchName: string
  jcCode: string | null
  matchday: string | null
  kickoffTime: string | null
  homeTeam: string
  awayTeam: string
  publicStatus: string
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | "BLOCKED"

PublicRiskCounters
  highRiskMatches: number
  staleOdds: number
  incompleteEvidence: number
  unresolvedConflicts: number

PublicIntegrityCounters
  complete: number
  partial: number
  blocked: number

PublicOddsFreshness
  fresh: number
  stale: number
  missing: number

PublicDecisionSummary
  analysisCount: number
  reviewCount: number
  publicClvTrendLabel: string | null

PublicAdminTodoCounters
  importReviewPending: number
  collectionReviewPending: number
```

字段级禁止：该 DTO 不得包含 `rawJson/rawPayload/archivePath/ticketNo/个人资金明细/入库映射/待审详情`。

后端模块建议：

```text
server/src/main/java/com/worldcup/publicoverview/api/PublicOverviewController.java
server/src/main/java/com/worldcup/publicoverview/api/dto/PublicOverviewDtos.java
server/src/main/java/com/worldcup/publicoverview/service/PublicOverviewService.java
server/src/test/java/com/worldcup/publicoverview/api/PublicOverviewControllerTest.java
```

前端模块建议：

```text
client/src/api/publicOverview.ts
client/src/__tests__/public-overview-api.test.ts
```

### 7.5 决策与复盘公开接口

现有 `GET /api/analysis-review/**` 返回 rawPayload、票号、金额和盈亏，保持管理员接口。公开页面新增脱敏接口：

```text
GET /api/public/decisions/reports
GET /api/public/decisions/reviews
```

公开字段只包含：比赛、日期、结论类型、置信度、风险摘要、复盘摘要、规则沉淀摘要、状态枚举。完整投注方案、票号、金额、盈亏、rawPayload 仅管理员可见。

## 8. 下注硬规则 UI 映射

| CLAUDE.md 规则 | UI 位置 | 呈现方式 |
|---|---|---|
| 官方核对优先 | 赛前作战顶部 | 比赛编号、开赛时间、玩法、让球、开售状态检查卡 |
| HAD/HHAD 分开 | 赛前作战/赔率页 | 普通胜平负与让球胜平负分区展示，不共用标签 |
| 90 分钟结算 | 赛前作战/决策页 | 固定提示，淘汰赛时高亮 |
| 玩法体系 | 决策与复盘 | HAD/HHAD/比分/TTG/HAFU/串关独立字段 |
| 动态预算 | 决策页管理员态 | “本轮预算未设置”阻断保存，不能沿用旧默认值 |
| 票面 SP | 票据明细管理员态 | 票面 SP 优先，网页赔率仅估算 |
| CLV | 决策与复盘 | 入场赔率、收盘赔率、CLV 指标和趋势 |
| 多源核验 | 赛前作战/证据中心 | 来源数量、冲突、权威等级、过期状态 |
| 本届逐场表现 | 赛前作战 | 每队逐场样本折叠区 |
| 大赛气质 | 赛前作战/分析摘要 | 实力判断、盘口判断、大赛气质判断标签 |
| 战意与 48 队最佳第三名 | 赛前作战 | 小组形势、第三名红线、轮换/默契风险 |
| 行为偏差 | 决策保存前 | 近因、球星、从众、锚定、结果偏差自检 |
| 复盘五层 | 决策与复盘 | 数学、足球、盘口、大赛气质、赔率价值五区块 |

## 9. 测试与验收

### 9.1 自动化验证命令

- `cd client && npm run build`
- `cd client && npm run test:run`
- `cd server && mvn test`

### 9.2 后端权限测试矩阵

新增或调整 MockMvc 测试：

| 场景 | 期望 |
|---|---|
| 未登录 `GET /api/public/overview` | 200 |
| 未登录 `GET /api/matches` | 200 |
| 未登录 `GET /api/import-items` | 401 |
| 未登录 `GET /api/import-jobs/{id}` | 401 |
| 未登录 `GET /api/core-data/import-items/{id}/mappings` | 401 |
| 未登录 `GET /api/profiles/collections/items` | 401 |
| 未登录 `GET /api/system/settings` | 401 |
| 未登录 `GET /api/analysis-review/bets` | 401 |
| 未登录任意写接口 | 401 |
| 管理员访问管理 GET | 200 |
| 管理员执行批准/驳回/入库 | 200 |
| 非管理员访问管理端点 | 403，仅在引入非管理员测试用户后启用 |

### 9.3 前端路由与 API 测试矩阵

- `router.test.ts` 覆盖新路由、旧路由 redirect、`/admin/*` meta、登录回跳。
- auth store 测试覆盖：登录、logout、401 清空、无默认密码兜底、`isAdmin/canWrite`。
- API 测试覆盖：公开 GET 不带 Authorization；管理读/写注入 Authorization；401 跳登录；403 显示权限不足。
- public overview API 测试覆盖路径和 DTO 类型。
- ResponsiveTable 测试覆盖卡片模式字段 label/value。

### 9.4 响应式验收

必须检查：

- 375px 手机。
- 768px 平板。
- 1024px 小桌面。
- 1440px 桌面。
- 手机横屏。

可复现检查：

- 浏览器脚本或人工检查 `document.documentElement.scrollWidth <= window.innerWidth`。
- 底部导航不遮挡列表末尾和底部 CTA。
- 表格在 H5 上转卡片或局部横向滚动。
- 筛选抽屉 Apply/Reset/Close 可用，关闭后焦点回到触发器。
- 登录、管理写操作、驳回原因输入在手机端可用。
- `prefers-reduced-motion` 下不出现大幅动效。
- 每个主要页面至少留一张 375px 和 1440px 截图用于人工复核。

### 9.5 可访问性验收

- 焦点环可见。
- 键盘可访问主导航、底部导航、筛选抽屉、管理写操作。
- 表单 label 可见。
- 状态不只靠颜色表达。
- 深色模式文字对比度达标。
- 图标按钮有可访问名称。
- 当前导航有 `aria-current`。
- Dialog/Drawer focus trap、Esc 关闭、关闭后焦点回退。
- 路由切换聚焦 main 或 H1。
- loading/error 使用 aria-live 或页面内 alert。

## 10. 分阶段实施与回滚策略

### 10.1 阶段顺序

1. 基线测试与旧功能入口矩阵：记录当前路由、接口、页面能力和测试状态。
2. 后端权限矩阵与测试：先保护敏感 GET 和写接口，避免 UI 改造前产生泄露。
3. API helper 与 auth store：移除默认凭据，统一公开读/管理读写调用。
4. `/api/public/overview` 与 `/api/public/decisions/**`：提供首页和公开复盘所需脱敏 DTO。
5. 设计 tokens、AppShell、MobileTabbar、基础 UI 组件。
6. 路由重定向和 admin route guard。
7. 赛事总览首页。
8. 赛前作战室迁移。
9. 证据中心五个子页迁移。
10. 决策与复盘迁移。
11. 管理后台 JSON 审核、采集审核、系统设置迁移。
12. 全量响应式、权限、构建、测试与截图验收。

### 10.2 回滚策略

- 旧路由保留 redirect 或 alias，不在第一轮删除旧 view。
- 每迁移一个模块就运行相关测试和人工冒烟。
- 管理后台最后迁移，确保公开只读和权限保护先稳定。
- 如果新页面出现阻塞问题，可临时将旧路由 alias 指回旧 view。
- 安全配置优先保持保守：不确定是否公开的接口按管理员处理。

## 11. 旧功能入口矩阵

| 现有功能 | 新位置 | 是否保留 | 验收点 |
|---|---|---|---|
| 登录 | `/login` | 保留并改造 | 回跳、无默认凭据、401 清空 |
| 首页 Dashboard | `/` | 重写 | 公开 overview、H5、风险摘要 |
| JSON 审核中心 | `/admin/import-review` | 保留 | 未登录不可读，管理员批准入库 |
| 比赛中心 | `/evidence/matches` | 保留 | 赛程、阵容、事件、统计、证据 |
| 赔率中心 | `/evidence/odds` | 保留 | 盘口、玩法、公司、更新时间 |
| 舆情中心 | `/evidence/sentiment` | 保留 | 外部因素、风险类型、来源 |
| 分析下注复盘中心 | `/decisions` + 管理员明细 | 保留 | 脱敏公开摘要、管理员票据/金额 |
| 赛前作战室 | `/workbench` | 保留并强化 | 单场聚合、多源冲突、已归档方案、票据入口 |
| 球队画像 | `/evidence/teams` | 保留 | 已批准事实公开，采集审核转后台 |
| 球员画像 | `/evidence/players` | 保留 | 已批准事实公开，采集审核转后台 |
| 系统设置 | `/admin/settings` | 保留 | 未登录不可读，归档路径不公开 |
| 采集项审核 | `/admin/collection-review` | 强化入口 | 待审队列、批准、驳回、状态筛选 |

## 12. 非目标

- 不改变 `CLAUDE.md` 中的下注分析硬规则。
- 不删除 JSON 档案留存机制。
- 不把竞彩规则简化为普通体育新闻看板。
- 不为了视觉效果牺牲证据链、赔率、CLV、复盘可读性。
- 不引入大型 3D/WebGL 效果，避免性能和可访问性风险。
- 不由 Java 系统自动生成下注建议、自动加注或自动倍投。

## 13. 首轮审查意见处理结论

已纳入首轮子代理审查中的关键问题：

- 将“GET 默认公开”改为“公开 GET 白名单 + 管理 GET 优先匹配 + 写接口默认管理员”。
- 增加后端权限矩阵、公开数据脱敏规则、`/api/public/overview` 字段级 schema。
- 明确 `/api/analysis-review/**` 原接口保持管理员，公开决策复盘走脱敏 `/api/public/decisions/**`。
- 明确 Basic Auth 存储、默认凭据、登录回跳、401/403、`hasRole("ADMIN")`。
- 补充下注硬规则 UI 映射。
- 补充 H5 页面级布局、ResponsiveTable、FilterDrawer、safe-area、z-index、a11y 细则。
- 补充实施阶段、测试矩阵、回滚策略、旧功能入口矩阵。

## 14. 自检结论

- 核心方向已确认：赛事沉浸、五阶段决策流、PC + H5、公开只读 + 管理写入。
- 已补齐二次审查前必须明确的权限矩阵、脱敏边界、硬规则 UI 映射、H5 组件契约、测试矩阵和回滚策略。
- 仍需通过二次子代理审查确认文档可以进入 implementation plan。
