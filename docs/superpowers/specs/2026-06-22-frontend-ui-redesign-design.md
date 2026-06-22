# 前端整站重设计与 H5 适配设计

日期：2026-06-22
状态：已根据第三轮子代理审查修订，待最终确认
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

未登录打开系统时进入 **赛事沉浸总览首页**，展示今日比赛、关键风险、完整性摘要、只读入口与管理员入口。首页公开数据来自 `/api/public/overview`，只返回脱敏摘要和计数；其它公开页面一律使用 `/api/public/...` 脱敏 DTO，不直接放开现有富详情接口。

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
| 链接文本 | `--wc-primary` | 深色面板 | AA 以上，hover 增加下划线或背景 |
| 当前导航/选中态 | `--wc-on-accent` | `--wc-accent` 或琥珀弱背景 + `--wc-warning` | AA 以上，必须同时有选中指示 |

禁止组合：

- `--wc-text` / `--wc-text-muted` 直接放在 `--wc-accent` 上。
- `--wc-danger`、`--wc-success`、`--wc-warning` 作为纯色背景后再放白字，除非重新测得 AA。
- `--wc-primary-strong` / `--wc-primary-deep` 主要用于背景、边框、装饰或图表，不作为深色背景上的正文前景。
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
- 页面使用语义结构：`header`、`nav`、`main tabindex="-1"`、必要时 `aside`；每页唯一 H1，标题层级不跳级。
- 路由切换后焦点移动到 `main` 或页面 H1。
- Dialog/Drawer 必须支持 focus trap、Esc 关闭、关闭后焦点回到触发器。
- 表单错误必须与字段关联：`aria-invalid`、`aria-describedby`、错误文本靠近字段。
- 卡片列表由表格转换而来时，阅读顺序必须与桌面表头顺序一致。
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
| `/workbench` | 赛前作战 | 公开脱敏只读，写动作需管理员登录 | 使用 `/api/public/prematch-workbench/**`；原 `/api/prematch-workbench/**` 管理员 |
| `/evidence/matches` | 比赛与赛程 | 公开脱敏只读 | 使用 `/api/public/matches/**`；原 `/api/matches/**` 管理员 |
| `/evidence/odds` | 赔率与盘口 | 公开脱敏只读 | 使用 `/api/public/odds/**`；原 `/api/odds/**` 管理员 |
| `/evidence/sentiment` | 舆情与外部因素 | 公开脱敏只读 | 使用 `/api/public/sentiment/**`；原 `/api/sentiment/**` 管理员 |
| `/evidence/teams` | 球队画像 | 公开脱敏只读 | 使用 `/api/public/profiles/teams/**`；原画像接口管理员或内部复用 |
| `/evidence/players` | 球员画像 | 公开脱敏只读 | 使用 `/api/public/profiles/players/**`；原画像接口管理员或内部复用 |
| `/decisions` | 决策与复盘 | 公开脱敏摘要，敏感明细管理员 | 使用 `/api/public/decisions/**`；原 `/api/analysis-review/**` 管理员 |
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
- 阵容、伤停、红黄牌、战意、48 队最佳第三名横向风险。
- 多源证据、来源冲突、数据时效与完整性检查。
- 关键数据必须至少 2 个独立来源交叉验证；不足 2 个来源时完整性状态为 `partial` 或 `blocked`，触发降额、空仓或阻断 `ready_to_bet`。
- 来源冲突必须标出冲突字段、采用依据和未解决状态；未解决冲突不能被包装为“已验证”。
- 赔率快照、Pinnacle/主流公司、票面 SP 提示、赔率过期提醒。
- 已入库分析报告、外部 AI/人工归档方案、实际出票记录入口；公开模式只展示脱敏摘要，管理员才可看票号、单票金额、盈亏、rawPayload、stakeSuggestion。
- 行为偏差自检：近因、球星/牌面、大众盘从众、锚定、结果偏差。
- 展示已批准分析/外部 AI 或人工归档方案与风险摘要；Java 不自动生成新下注建议，页面不提供“生成下注方案”按钮。

本届逐场表现字段契约：

| 字段 | 要求 |
|---|---|
| 对手与比分 | 每队每场单独列出；无本届样本时显示“本届首战，无本届样本” |
| 进失球时间 | 记录关键分钟和领先/落后状态变化 |
| 首发/阵型变化 | 标明阵型、首发调整、轮换 |
| 伤停/红黄牌 | 标明关键球员、停赛、红黄牌影响 |
| 控球与射门质量 | 不只写控球率，必须呈现射门/射正/xG 或机会质量摘要 |
| 定位球与反击 | 单独展示是否有稳定定位球、反击威胁 |
| 防守稳定性 | 展示被压制、失误、守领先能力 |
| 领先/落后应对 | 展示是否急躁、能否破密集、换人效果 |
| 结论归因 | 区分实力体现、战术成功、对手失误、运气因素、比分掩盖问题 |

九维分析入口：

1. 历史交锋与近期战绩。
2. 阵容配置、主力状态、伤停/停赛。
3. 教练状态、战术风格、轮换可能。
4. 小组形势、战意、赛程体能、天气/场地。
5. 赔率与庄家视角。
6. 世界杯/大赛履历。
7. 球队比赛气质。
8. 破密集能力。
9. 冷门路径。

资金与玩法校验面板：

- 本轮预算来源必须明确；未给预算时阻断保存。
- 区分胜负/让球资金盘与比分/TTG/HAFU 等低分玩法资金盘。
- 显示单一逻辑占比，超过胜负类资金 60%-70% 时警告或阻断。
- 凯利只作相对权重，显示分数凯利建议，不作为绝对注额。
- 深盘必须展示保险方向；比分必须展示主比分、卡线比分、冷门比分分层。
- 保险票必须展示票型、金额、覆盖方向、触发理由、所属主逻辑，以及与主逻辑合计后的资金占比。
- 守领先、慢热、破密集差场景提示优先考虑 TTG/HAFU，而非默认猜比分。
- 允许“空仓”并要求记录空仓理由。

写操作：

- 保存分析草稿、导入外部 AI JSON、关联票据、更新状态等必须管理员登录。
- 新方案来源必须是人工或外部 AI JSON，经审核后入库。

H5：

- 首屏顺序：比赛选择器 → 官方核对状态 → 风险总览 → 证据完整性。
- 九维分析、本届逐场表现、赔率、舆情、资金玩法校验、方案、票据使用折叠区。
- 关键 CTA 使用 `SafeAreaActionBar`，避开底部导航和安全区；未登录时显示“需管理员权限才能保存/导入”。
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


### 5.6 页面级响应式布局矩阵

每个主要页面必须同时实现 Desktop / Tablet / H5 布局，不能只做 H5 适配后让 PC 自由堆叠。

| 页面 | 1440 标准桌面 | 1024 小桌面 | 768 平板 | 375 H5 |
|---|---|---|---|---|
| 赛事总览 | Hero + 今日比赛 + KPI 横排；风险队列与五阶段入口两栏 | Hero 压缩，KPI 2×2，风险队列与入口上下排列 | 卡片 2 列，顶部状态栏收敛 | 单列，今日比赛、高风险提醒、赛前入口优先 |
| 赛前作战 | 左侧比赛上下文，中区证据/赔率/九维分析，右侧方案与规则检查侧栏 | 双栏：上下文 + 主内容；方案侧栏下移 | 单列折叠区 + 顶部比赛选择器 | 单列任务流，关键 CTA 用 SafeAreaActionBar |
| 证据中心 | 二级导航 + 表格/矩阵主区，详情可右侧抽屉 | 二级导航顶部化，表格宽度自适应 | 卡片/表格混合，筛选抽屉 | 分类横向滚动，列表卡片，宽表局部横滚 |
| 决策与复盘 | 左侧列表 + 右侧详情 split view；CLV/复盘卡片并列 | 列表与详情上下切换，详情优先 | 卡片列表 + 详情页 | 卡片列表，五层复盘折叠，管理员动作进底部操作栏 |
| 管理后台 | master-detail：审核队列左，详情/映射右，批量操作顶部 | 双栏或详情抽屉，队列保持可见 | 队列卡片 + 详情页 | 更多入口进入；审核详情底部批准/驳回固定操作区 |

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

领域组件优先拆分：

- matches：`MatchCard`、`MatchEventTimeline`、`LineupPanel`、`TeamStatsPanel`、`PlayerStatsPanel`。
- odds：`OddsMarketCard`、`BookmakerSelector`、`OddsFreshnessBadge`、`MarketLineTable`。
- sentiment：`ContextFactorCard`、`RiskAssessmentList`、`SourceReliabilityBadge`。
- profiles：`TeamProfileCard`、`PlayerProfileCard`、`ApprovedFactList`、`CollectionReviewQueue`。
- decisions：`DecisionSummaryCard`、`RuleChecklistPanel`、`ClvTrendPanel`、`ReviewLayerAccordion`。
- admin：`AdminReviewQueue`、`ImportItemDetail`、`MappingResultPanel`、`RejectReasonForm`。

### 6.2 ResponsiveTable 与筛选抽屉契约


`SafeAreaActionBar`：

- H5 详情页如果出现保存、导入、批准、驳回等关键 CTA，优先进入“任务模式”：隐藏 `MobileTabbar`，显示 `SafeAreaActionBar`。
- 如果必须同时显示底部导航和操作栏，`SafeAreaActionBar` 悬浮在 `MobileTabbar` 上方，并定义 `--safe-actionbar-height: 56px`。
- 底部 padding 明确三种模式：仅 tabbar = `var(--mobile-tabbar-height) + env(safe-area-inset-bottom)`；仅 actionbar = `var(--safe-actionbar-height) + env(safe-area-inset-bottom)`；共存 = `var(--mobile-tabbar-height) + var(--safe-actionbar-height) + env(safe-area-inset-bottom)`。
- 任务模式下必须将 `--mobile-tabbar-height` 置为 `0px` 或使用单独 class，避免隐藏 tabbar 后仍保留多余空白。
- 横屏时操作栏可收敛为顶部 toolbar；验收截图必须覆盖 375px 竖屏和手机横屏。

`ResponsiveTable`：

- 小于 768px 默认卡片模式。
- 组件只接受受控排序状态；排序、筛选、行操作不能在不同页面各自私有实现。
- 每个字段必须以 label/value 呈现；卡片阅读顺序为 `mobileSummaryFields` 后接 `columns.priority` 升序，未配置 priority 的字段进入“更多信息”。
- 保留排序状态；可排序字段必须提供 `aria-sort`，卡片模式也要显示当前排序。
- 赔率矩阵和宽数据表可局部横向滚动，但滚动容器必须可键盘聚焦并有可见提示；整页不能横向滚动。
- 卡片模式必须保留筛选状态和关键状态徽章。

Props / slots 契约：

```ts
interface ResponsiveTableColumn<Row> {
  key: keyof Row | string
  label: string
  priority?: number
  sortable?: boolean
  mobileSummary?: boolean
  align?: 'start' | 'center' | 'end'
  formatter?: (value: unknown, row: Row) => string
  ariaLabel?: string
}

interface RowAction<Row> {
  key: string
  label: string
  ariaLabel: (row: Row) => string
  disabled?: (row: Row) => boolean
  danger?: boolean
}

props: {
  rows: Row[]
  columns: ResponsiveTableColumn<Row>[]
  rowKey: keyof Row | ((row: Row) => string)
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  mobileSummaryFields?: string[]
  rowActions?: (row: Row) => RowAction<Row>[]
  horizontalScrollLabel?: string
}

emits: {
  'sort-change': [{ key: string, direction: 'asc' | 'desc' }]
  'row-action': [{ actionKey: string, row: Row }]
}

slots:
  #cell-{key}="{ row, column, value }"  // 动态列 cell slot，key 对应 columns.key
  #mobile-summary="{ row }"
  #row-actions="{ row, actions }"
  #empty
```

`FilterDrawer`：

- 包含 Apply、Reset、Close。
- 区分 draft 与 applied：抽屉内编辑 draft；Apply 后写入 applied；Close 放弃未应用 draft；Reset 清空 draft 并可 Apply。
- 顶部展示 applied 筛选 chips；抽屉内展示 draft 状态。
- 支持 Esc/返回关闭、focus trap、关闭后焦点回到触发器。
- 关闭不丢已应用筛选；只有 Reset + Apply 才清空。

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

现有富详情接口始终不公开给匿名用户。新设计采用 **公开 `/api/public/...` 脱敏接口 + 管理端点优先匹配 + 写接口默认管理员**。

硬规则：

1. 先实现公开 DTO，并通过字段级与值级脱敏测试。
2. 只有通过脱敏测试的 `/api/public/...` 端点才能进入匿名白名单。
3. 现有富详情端点（如 `/api/matches/**`、`/api/odds/**`、`/api/sentiment/**`、`/api/prematch-workbench/**`、`/api/analysis-review/**`）始终管理员；若需公开能力，必须新增或迁移到 `/api/public/...` 脱敏 DTO，原 `/api/...` 路径即使 public DTO 完成也不得进入匿名白名单。
4. 所有 `POST / PUT / PATCH / DELETE` 默认要求 `hasRole("ADMIN")`。
5. 其它未列出的 `/api/**` 默认要求 `hasRole("ADMIN")`，避免新增敏感接口意外公开。

Spring Security 规则顺序：

1. 先精确放行 `GET /api/health`、`POST /api/auth/login`，以及必要的 CORS `OPTIONS`。
2. 再匹配 admin-only 端点，要求 `hasRole("ADMIN")`。
3. 再匹配所有 `POST/PUT/PATCH/DELETE /api/**`，默认要求 `hasRole("ADMIN")`；不得存在宽泛匿名 `/api/public/**` 写接口。
4. 只允许 `GET /api/public/**` 进入匿名白名单，且必须已通过字段级与值级脱敏测试。
5. 兜底 `/api/**` 管理员。

方法级安全：

- 必须启用 `@EnableMethodSecurity`。
- 管理 Controller 或敏感方法必须加 `@PreAuthorize("hasRole('ADMIN')")`，至少覆盖：ImportReviewController、CoreData import/mappings、Profile collection review、SystemSettingsController、AnalysisReviewCenterController 管理明细。
- 现有富读 Controller 也必须作为方法级双保险覆盖：MatchesController、OddsController、SentimentController、PrematchWorkbenchController、ProfileController；public Controller 单独命名并只返回 public DTO。

Swagger：

- dev/local profile 可公开，便于本地联调。
- 非本机或生产部署必须关闭或要求管理员认证，并加入配置/测试覆盖。

默认管理员凭据：

- 登录页不预填真实默认凭据。
- 前端不保留默认密码兜底。
- 非 local/test profile 禁止使用默认 admin 密码；未通过环境变量或本地未提交安全配置注入密码时，服务启动失败。
- 本地个人环境可保留开发默认值，但不得作为生产配置。

### 7.2 后端权限矩阵

| 类别 | 端点 | 未登录 | 管理员 | 说明 |
|---|---|---:|---:|---|
| 健康检查 | `GET /api/health` | 200 | 200 | 公开 |
| 登录 | `POST /api/auth/login` | 200 | 200 | 公开 |
| 公开首页 | `GET /api/public/overview` | 200 | 200 | 脱敏 DTO |
| 公开比赛 | `GET /api/public/matches/**` | 200 | 200 | 脱敏 DTO，不含 payload/rawPayload |
| 公开赔率 | `GET /api/public/odds/**` | 200 | 200 | 脱敏 DTO，不含 rawPayload |
| 公开舆情 | `GET /api/public/sentiment/**` | 200 | 200 | 脱敏 DTO，不含待审详情/rawPayload |
| 公开画像 | `GET /api/public/profiles/**` | 200 | 200 | 已批准事实，不含 approvedBy/reviewNote |
| 公开赛前作战 | `GET /api/public/prematch-workbench/**` | 200 | 200 | 脱敏 DTO，不含票据/金额/归档方案明细 |
| 公开决策复盘 | `GET /api/public/decisions/**` | 200 | 200 | 脱敏摘要 |
| 现有比赛富接口 | `GET /api/matches/**` | 401 | 200 | 始终管理员；公开能力只走 `/api/public/matches/**` |
| 现有赔率富接口 | `GET /api/odds/**` | 401 | 200 | 始终管理员；公开能力只走 `/api/public/odds/**` |
| 现有舆情富接口 | `GET /api/sentiment/**` | 401 | 200 | 始终管理员；公开能力只走 `/api/public/sentiment/**` |
| 现有赛前作战富接口 | `GET /api/prematch-workbench/**` | 401 | 200 | 始终管理员；公开能力只走 `/api/public/prematch-workbench/**` |
| JSON 审核读取 | `GET /api/import-jobs/**`、`GET /api/import-items/**` | 401 | 200 | raw JSON / 归档路径，管理员 |
| 入库映射 | `GET /api/core-data/import-items/**` | 401 | 200 | 管理员 |
| 核心数据概览 | `GET /api/core-data/overview` | 401 | 200 | 原接口管理员；公开计数只走 `/api/public/overview` |
| 采集审核 | `GET /api/profiles/collections/**` | 401 | 200 | 管理员 |
| 现有画像富接口 | `GET /api/profiles/**` | 401 | 200 | 管理员；公开画像只走 `/api/public/profiles/**` |
| 系统设置 | `GET /api/system/**` | 401 | 200 | 归档路径，管理员 |
| 分析/票据敏感明细 | `GET /api/analysis-review/**` | 401 | 200 | 原接口恒管理员；公开只允许 `/api/public/decisions/**` |
| 所有写接口 | `POST/PUT/PATCH/DELETE /api/**` | 401 | 200 | 管理员 |

如果未来添加非管理员账号：

- 未认证返回 401。
- 已认证但非管理员访问管理端点返回 403。
- 当前只有管理员账号时，测试可先覆盖 401 与管理员 200；若新增测试用户再覆盖 403。

### 7.3 公开 DTO 硬化规则

公开 DTO 必须字段白名单化，不复用现有富 DTO。

| 公开端点 | 允许字段示例 | 禁止字段 |
|---|---|---|
| `/api/public/matches/**` | matchId、matchName、jcCode、matchday、kickoffTime、teams、score/status、公开统计摘要 | payload、rawPayload、本地路径、内部备注 |
| `/api/public/odds/**` | matchId、marketType、marketName、selectionName、oddsValue、capturedAt、freshnessStatus、bookmakerName | rawPayload、内部采集字段、文件路径 |
| `/api/public/sentiment/**` | matchId、title、summary、riskType、riskLevel、sourceName、reliabilityLabel | rawPayload、待审详情、内部审核字段 |
| `/api/public/profiles/**` | team/player id、displayName、已批准事实摘要、公开状态标签 | approvedBy、reviewedBy、reviewNote、rawPayload、待审项 |
| `/api/public/prematch-workbench/**` | match summary、官方核对状态、证据完整性、公开风险摘要、公开赔率 freshness | ticketNo、stake、returnAmount、profitLoss、rawPayload、stakeSuggestion、budgetAmount、外部 AI/人工方案明细 |
| `/api/public/decisions/**` | match、conclusionType、confidence、riskSummary、reviewSummary、lessonSummary | ticketNo、stake、profitLoss、returnAmount、rawPayload、完整投注方案明细 |
| `/api/public/overview` | 计数、状态枚举、公开比赛摘要、公开风险摘要 | rawJson、rawPayload、archivePath、ticketNo、个人资金明细、映射详情、待审详情 |

公开接口禁止返回：

- `rawJson`
- `rawPayload`
- `payload`；公开 DTO 永远不得包含名为 `payload` 的字段，安全内容必须拆成命名白名单字段
- `archivePath`
- `sourcePath` 或本地文件绝对路径
- `ticketNo`
- 单票 `stake`、`returnAmount`、`profitLoss`
- `budgetAmount`、`stakeSuggestion` 等具体投注资金建议
- 入库映射详情
- 待审 JSON 详情
- 采集待审项原始内容
- `approvedBy`、`reviewedBy`、`reviewNote`、管理操作人、审核路径、内部备注

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
```

字段级禁止：该 DTO 不得包含 `rawJson/rawPayload/archivePath/ticketNo/个人资金明细/入库映射/待审详情`。

数据源与计算规则：

| 字段 | 来源 | 规则 | 空库默认值 |
|---|---|---|---|
| `generatedAt` | 服务端当前时间 | 北京时间 ISO 字符串 | 当前时间 |
| `todayMatches` | Match 查询服务或表 | 北京时间今日优先；无今日则取未来最近 5 场；按开赛时间升序 | `[]` |
| `riskCounters.highRiskMatches` | integrity / sentiment / odds 聚合 | `riskLevel` HIGH 或 BLOCKED 计数 | `0` |
| `riskCounters.staleOdds` | odds capturedAt | 距当前超过 3 小时视为过期 | `0` |
| `riskCounters.incompleteEvidence` | integrity checks | partial + blocked 计数 | `0` |
| `riskCounters.unresolvedConflicts` | evidence conflict 状态 | 未解决冲突计数 | `0` |
| `integrityCounters` | Prematch integrity | complete/partial/blocked 分组计数 | 全 0 |
| `oddsFreshness` | Odds 快照 | fresh/stale/missing 分组计数 | 全 0 |
| `decisionSummary` | public decisions 服务 | 分析/复盘计数，CLV 只给趋势标签 | 计数 0，趋势 null |
| `adminTodoCounters` | import/profile collection 服务 | 只返回待办数量，不返回详情 | 全 0 |

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

现有 `GET /api/analysis-review/**` 返回 rawPayload、票号、金额和盈亏，未登录恒 401。公开页面新增脱敏接口：

```text
GET /api/public/decisions/reports
GET /api/public/decisions/reviews
```

公开字段只包含：比赛、日期、结论类型、置信度、风险摘要、复盘摘要、规则沉淀摘要、状态枚举。完整投注方案、票号、金额、单票 stake/return/profitLoss、stakeSuggestion、budgetAmount、rawPayload 仅管理员可见。

## 8. 下注硬规则 UI 映射


| CLAUDE.md 规则 | UI 位置 | 呈现方式 |
|---|---|---|
| 官方核对优先 | 赛前作战顶部 | 比赛编号、开赛时间、玩法、让球、开售状态检查卡 |
| HAD/HHAD 分开 | 赛前作战/赔率页 | 普通胜平负与让球胜平负分区展示，不共用标签 |
| 90 分钟结算 | 赛前作战/决策页 | 固定提示，淘汰赛时高亮 |
| 异常场次 | 赛前作战/决策页 | 延期、腰斩、取消显示“待官方裁定”，禁止提前结算 |
| 玩法体系 | 决策与复盘 | HAD/HHAD/比分/TTG/HAFU/串关独立字段 |
| 动态预算 | 决策页管理员态 | 预算来源、两类资金盘、单逻辑 60%-70% 上限、分数凯利、空仓理由 |
| 深盘/比分分层 | 决策页管理员态 | 深盘保险、保险票字段、主比分/卡线/冷门分层、TTG/HAFU 替代提示 |
| 票面 SP | 票据明细管理员态 | 票面 SP 优先，网页赔率仅估算 |
| CLV | 决策与复盘 | 入场赔率、收盘赔率、CLV 指标和趋势 |
| 多源核验 | 赛前作战/证据中心 | 关键数据至少 2 个独立来源；展示来源数量、冲突、权威等级、过期状态；不足时 partial/blocked 并触发降额、空仓或阻断 ready_to_bet |
| 证据类型分层 | 证据中心 | 数据事实、媒体观点、赔率信号、个人推断分标签展示 |
| 本届逐场表现 | 赛前作战 | 每队逐场样本折叠区，字段契约见 5.2 |
| 九维分析框架 | 赛前作战 | 九个固定区块，H5 保留折叠入口 |
| 大赛气质 | 赛前作战/分析摘要 | 实力判断、盘口判断、大赛气质判断标签 |
| 战意与 48 队最佳第三名 | 赛前作战 | 小组形势、第三名红线、轮换/默契风险 |
| 行为偏差 | 决策保存前 | 近因、球星、从众、锚定、结果偏差自检 |
| 复盘五层 | 决策与复盘 | 数学、足球、盘口、大赛气质、赔率价值五区块 |

## 9. 测试与验收


### 9.1 自动化验证命令

- `cd client && npm run build`
- `cd client && npm run test:run`
- `cd server && mvn test`

### 9.2 后端权限与内容级脱敏测试矩阵

新增或调整 MockMvc 测试：

权限测试必须覆盖 7.2 每一行；每个 legacy rich GET 至少有一个代表性路径验证未登录 401、管理员 200。

| 场景 | 期望 |
|---|---|
| 未登录 `GET /api/public/overview` | 200 |
| 未登录 `GET /api/public/matches` | 200 |
| 未登录 `GET /api/public/odds` | 200 |
| 未登录 `GET /api/public/sentiment` | 200 |
| 未登录 `GET /api/public/profiles/teams` | 200 |
| 未登录 `GET /api/public/prematch-workbench/matches` | 200 |
| 未登录 `GET /api/public/decisions/reports` | 200 |
| 未登录 `POST/PUT/PATCH/DELETE /api/public/**` | 401 或 405；不得匿名写 |
| 未登录 `GET /api/matches` | 401；原端点始终管理员，如需公开必须新增或迁移到 `/api/public/matches` |
| 未登录 `GET /api/odds` | 401；公开能力只走 `/api/public/odds` |
| 未登录 `GET /api/sentiment` | 401；公开能力只走 `/api/public/sentiment` |
| 未登录 `GET /api/prematch-workbench/matches` | 401；公开能力只走 `/api/public/prematch-workbench/matches` |
| 未登录 `GET /api/profiles/teams` | 401；公开能力只走 `/api/public/profiles/teams` |
| 未登录 `GET /api/core-data/overview` | 401；公开计数只走 `/api/public/overview` |
| 未登录 `GET /api/analysis-review/overview` | 401；公开摘要只走 `/api/public/decisions/**` |
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

公开接口内容级断言：

- 所有匿名 200 的公开接口必须断言以下字段不存在：`rawJson`、`rawPayload`、`payload`、`archivePath`、`sourcePath`、`ticketNo`、`stake`、`stakeSuggestion`、`budgetAmount`、`returnAmount`、`profitLoss`、`approvedBy`、`reviewedBy`、`reviewNote`、`mappings`、`importItemId`。
- 使用 JSONPath negative assertions，例如：`$..rawPayload` 不存在、`$..ticketNo` 不存在、`$..stake` 不存在。
- 对 public DTO 做 contract/snapshot 测试：字段新增必须显式评估是否可公开。
- 增加值级脱敏测试：构造含票号、金额、`archivePath/sourcePath`、raw JSON 片段、审核人、reviewNote、stakeSuggestion、budgetAmount 的 fixture，断言 `summary/riskSummary/reviewSummary/lessonSummary` 等允许文本字段不含这些模式。
- 匿名白名单必须依赖字段级与值级脱敏测试通过；测试失败时 public endpoint 不得 permitAll。
- 覆盖：overview、public decisions、public matches、public odds、public sentiment、public profiles、public prematch、core public counters。

### 9.3 前端路由、API 与组件测试矩阵

- `router.test.ts` 覆盖新路由、旧路由 redirect、`/admin/*` meta、登录回跳。
- auth store 测试覆盖：登录、logout、401 清空、无默认密码兜底、`isAdmin/canWrite`。
- API 测试覆盖：公开 GET 不带 Authorization；管理读/写注入 Authorization；401 跳登录；403 显示权限不足。
- public overview API 测试覆盖路径和 DTO 类型。
- API 迁移矩阵必须逐文件覆盖：`http.ts`、`matches.ts`、`odds.ts`、`sentiment.ts`、`profiles.ts`、`prematchWorkbench.ts`、`analysisReview.ts`、`coreData.ts`、`importReview.ts`、`system.ts`。
- API 测试必须同步覆盖 §11 旧功能入口矩阵中的每个子能力/API，避免遗漏赔率字典、舆情字典、球队/球员详情等旧能力。
- ResponsiveTable 测试覆盖卡片模式字段 label/value、排序状态、rowActions。
- FilterDrawer 测试覆盖 draft/applied、Apply、Reset、Close 焦点回退。
- AppShell/MobileTabbar/AdminGate/ReadonlyNotice 测试覆盖：未登录隐藏写操作、管理员显示、移动底栏导航项、只读提示、admin route guard。

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
- `SafeAreaActionBar` 与 `MobileTabbar` 共存或替换规则符合 6.2。
- 表格在 H5 上转卡片或局部横向滚动。
- 筛选抽屉 Apply/Reset/Close 可用，关闭后焦点回到触发器。
- 登录、管理写操作、驳回原因输入在手机端可用。
- `prefers-reduced-motion` 下不出现大幅动效。
- 每个主要页面至少留：375×667、667×375、768、1024、1440 截图用于人工复核；如某断点只做自动化检查，必须在验收记录中注明。

### 9.5 可访问性验收

- 焦点环可见。
- 页面使用 `header/nav/main` landmarks；每页唯一 H1，标题层级不跳级。
- `main tabindex="-1"` 或等效方案支持路由切换聚焦。
- 键盘可访问主导航、底部导航、筛选抽屉、管理写操作。
- 表单 label 可见；错误用 `aria-invalid` / `aria-describedby` 关联字段。
- 状态不只靠颜色表达。
- 深色模式文字对比度达标。
- 图标按钮有可访问名称。
- 当前导航有 `aria-current`。
- Dialog/Drawer focus trap、Esc 关闭、关闭后焦点回退。
- loading/error 使用 aria-live 或页面内 alert。

## 10. 分阶段实施与回滚策略


### 10.1 阶段顺序

1. 基线测试与旧功能入口矩阵：记录当前路由、接口、页面能力和测试状态。
2. 公开 DTO 契约与脱敏测试：为 matches/odds/sentiment/profiles/prematch/decisions/overview 建立 `/api/public/...` DTO 字段白名单、mapper、fixture、字段级与值级脱敏 contract tests；此阶段不把未完成端点加入匿名白名单。
3. 后端权限矩阵与测试：公开白名单只包含已通过字段级与值级脱敏测试的 public 端点；保护敏感 GET 和写接口。
4. API helper 与 auth store：移除默认凭据，统一公开读/管理读写调用。
5. `/api/public/overview` 与 `/api/public/decisions/**`：实现聚合 service、Controller、前端 API 接入和匿名 200 + 禁用字段/值级脱敏测试。
6. 设计 tokens、AppShell、MobileTabbar、SafeAreaActionBar、基础 UI 组件。
7. 路由重定向和 admin route guard。
8. 赛事总览首页。
9. 赛前作战室迁移。
10. 证据中心五个子页迁移。
11. 决策与复盘迁移。
12. 管理后台 JSON 审核、采集审核、系统设置迁移。
13. 全量响应式、权限、构建、测试与截图验收。

### 10.2 阶段 DoD / 验收门

| 阶段 | 产物 | 必跑测试 | 冒烟 | 回滚点 |
|---|---|---|---|---|
| 基线 | 旧路由/API/功能矩阵 | 当前 client/server 测试 | 旧页面可打开 | 无改动 |
| 公开 DTO 契约 | `/api/public/...` DTO、mapper、fixture、contract tests | 禁用字段不存在 + 值级脱敏测试 | 测试 fixture 通过 | public 端点不进匿名白名单 |
| 后端权限 | SecurityConfig + method security | 权限矩阵测试 | 未登录管理接口 401 | 回到全接口管理员 |
| API/auth | public/admin API helper | API/auth/router 测试 | 登录、401、回跳 | 旧 API wrapper 保留 |
| public overview/decisions | 聚合 service + Controller + 前端 API | 匿名 200 + 禁用字段/值级脱敏 | 首页与复盘摘要可读 | 对应 public endpoint 不进白名单或指回旧页 |
| AppShell/UI | tokens + Shell + H5 nav | 组件测试 | 375/1440 打开首页 | 旧 App.vue/旧 view alias |
| 每个模块迁移 | 新 view + domain components | 对应 API/组件测试 | 旧路由 redirect、新路由可用 | alias 指回旧 view |
| 管理后台迁移 | admin views | 管理权限和写操作测试 | 批准/驳回/入库可用 | 旧 admin view alias |

### 10.3 API 迁移矩阵

| 文件 | 当前职责 | 新公开 API | 新管理 API | 测试 |
|---|---|---|---|---|
| `http.ts` | API 基础客户端/interceptor | public client 不带 Authorization | admin helper 注入 Authorization | 401 清空并回跳、403 权限不足、无默认密码 |
| `matches.ts` | 比赛中心 | `/api/public/matches/**` | `/api/matches/**` | public matches + admin rich |
| `odds.ts` | 赔率中心 | `/api/public/odds/**` | `/api/odds/**` | freshness + 禁用字段 |
| `sentiment.ts` | 舆情中心 | `/api/public/sentiment/**` | `/api/sentiment/**` | risk/category + 禁用字段 |
| `profiles.ts` | 球队/球员/采集审核 | `/api/public/profiles/**` | `/api/profiles/collections/**`、必要 admin rich | public fact + collection review |
| `prematchWorkbench.ts` | 赛前作战 | `/api/public/prematch-workbench/**` | `/api/prematch-workbench/**` | public脱敏 + admin详情 |
| `analysisReview.ts` | 分析/方案/票据/复盘 | `/api/public/decisions/**` | `/api/analysis-review/**` | public摘要 + admin票据 |
| `coreData.ts` | 概览/入库映射 | `/api/public/overview` 中聚合计数 | `/api/core-data/**` | overview + mappings admin |
| `importReview.ts` | JSON 审核 | 无公开 | `/api/import-jobs/**`、`/api/import-items/**` | admin only |
| `system.ts` | 健康/登录/设置 | `/api/health`、`/api/auth/login` | `/api/system/**` | login + settings admin |

### 10.4 回滚策略

- 旧 view + 旧 API wrapper 保留到全量验收后。
- 新 public API 采用 additive 方式，不替换现有管理 API。
- SecurityConfig 默认保守：不确定是否公开的接口按管理员处理。
- `VITE_USE_NEW_UI` 只控制前端 route map，不改变后端权限；默认关闭时所有新路由 alias 指回旧 view，开启后仅已标记 migrated 的模块使用新 view，未迁移模块仍指向旧 view。router 测试必须覆盖开关两种状态。
- 如果 public DTO 泄露测试失败，不将对应 public 端点加入匿名白名单。
- 如果 API helper 破坏旧页面，旧 wrapper 保留并逐页迁移。
- 每迁移一个模块就运行相关测试和人工冒烟。
- 管理后台最后迁移，确保公开只读和权限保护先稳定。

## 11. 旧功能入口矩阵


| 现有功能/子能力 | 当前 API / 路由 | 新位置 | 权限 | 验收点 |
|---|---|---|---|---|
| 登录 | `/login`、`POST /api/auth/login` | `/login` | 公开 | 回跳、无默认凭据、401 清空 |
| 首页 Dashboard | `/` | `/` | 公开脱敏 | public overview、H5、风险摘要 |
| 核心数据概览 | `GET /api/core-data/overview` | `/api/public/overview` 聚合 | 公开脱敏；原接口管理员 | 只返回计数 |
| JSON 扫描 | `POST /api/import-jobs/scan` | `/admin/import-review` | 管理员 | 未登录 401，管理员可扫描 |
| JSON 任务详情 | `GET /api/import-jobs/{id}` | `/admin/import-review` | 管理员 | 不公开归档路径 |
| JSON 待审项 | `GET /api/import-items`、`GET /api/import-items/{id}` | `/admin/import-review` | 管理员 | raw JSON 不公开 |
| JSON 批准/驳回 | `POST /api/import-items/**` | `/admin/import-review` | 管理员 | 批准、批量批准、驳回 |
| 入库映射 | `GET /api/core-data/import-items/{id}/mappings` | `/admin/import-review` | 管理员 | 未登录 401 |
| 执行入库 | `POST /api/core-data/import-items/{id}/import` | `/admin/import-review` | 管理员 | 入库成功和错误态 |
| 比赛列表/详情 | `GET /api/matches/**` | `/evidence/matches` via `/api/public/matches/**` | 公开脱敏；原接口管理员 | 赛程、详情、payload 不泄露 |
| 比赛阵容 | `GET /api/matches/{id}/lineups` | `/evidence/matches` via `/api/public/matches/{id}/lineups` | 公开脱敏；原接口管理员 | 阵容卡片 H5 |
| 比赛事件 | `GET /api/matches/{id}/events` | `/evidence/matches` via `/api/public/matches/{id}/events` | 公开脱敏；原接口管理员 | 时间线 |
| 球队统计 | `GET /api/matches/{id}/team-stats` | `/evidence/matches` via `/api/public/matches/{id}/team-stats` | 公开脱敏；原接口管理员 | 统计卡片/表格 |
| 球员统计 | `GET /api/matches/{id}/player-stats` | `/evidence/matches` via `/api/public/matches/{id}/player-stats` | 公开脱敏；原接口管理员 | H5 卡片 |
| 赔率列表 | `GET /api/odds` | `/evidence/odds` via `/api/public/odds` | 公开脱敏；原接口管理员 | 列表不含 rawPayload |
| 单场赔率 | `GET /api/odds/matches/{matchId}` | `/evidence/odds` via `/api/public/odds/matches/{matchId}` | 公开脱敏；原接口管理员 | 单场盘口 freshness、bookmaker 摘要 |
| 赔率公司字典 | `GET /api/odds/bookmakers` | `/evidence/odds` via `/api/public/odds/bookmakers` | 公开脱敏；原接口管理员 | bookmakerName 不含内部采集字段 |
| 赔率市场字典 | `GET /api/odds/markets` | `/evidence/odds` via `/api/public/odds/markets` | 公开脱敏；原接口管理员 | HAD/HHAD/TTG/HAFU 等市场不丢 |
| 舆情列表 | `GET /api/sentiment` | `/evidence/sentiment` via `/api/public/sentiment` | 公开脱敏；原接口管理员 | 列表摘要不含 rawPayload |
| 单场舆情 | `GET /api/sentiment/matches/{matchId}` | `/evidence/sentiment` via `/api/public/sentiment/matches/{matchId}` | 公开脱敏；原接口管理员 | 来源可靠度、风险摘要 |
| 舆情分类字典 | `GET /api/sentiment/categories` | `/evidence/sentiment` via `/api/public/sentiment/categories` | 公开脱敏；原接口管理员 | categories 不丢 |
| 风险类型字典 | `GET /api/sentiment/risk-types` | `/evidence/sentiment` via `/api/public/sentiment/risk-types` | 公开脱敏；原接口管理员 | risk-types 不丢 |
| 球队画像列表 | `GET /api/profiles/teams` | `/evidence/teams` via `/api/public/profiles/teams` | 公开已批准事实；原接口管理员 | 不含 approvedBy/reviewNote |
| 球队画像详情 | `GET /api/profiles/teams/{id}` | `/evidence/teams` via `/api/public/profiles/teams/{id}` | 公开已批准事实；原接口管理员 | 已批准事实摘要 |
| 球队球员列表 | `GET /api/profiles/teams/{id}/players` | `/evidence/teams` via `/api/public/profiles/teams/{id}/players` | 公开已批准事实；原接口管理员 | 球员列表 H5 卡片 |
| 球员画像列表 | `GET /api/profiles/players` | `/evidence/players` via `/api/public/profiles/players` | 公开已批准事实；原接口管理员 | 不含 approvedBy/reviewNote |
| 球员画像详情 | `GET /api/profiles/players/{id}` | `/evidence/players` via `/api/public/profiles/players/{id}` | 公开已批准事实；原接口管理员 | 伤停/状态事实摘要 |
| 采集任务创建 | `POST /api/profiles/collections/jobs` | `/admin/collection-review` | 管理员 | 创建任务 |
| 采集待审项 | `GET /api/profiles/collections/items` | `/admin/collection-review` | 管理员 | 待审队列 |
| 采集批准/驳回 | `POST /api/profiles/collections/items/**` | `/admin/collection-review` | 管理员 | 批准、驳回、状态筛选 |
| 赛前作战比赛列表 | `GET /api/prematch-workbench/matches` | `/workbench` via `/api/public/prematch-workbench/matches` | 公开脱敏；原接口管理员 | 单场入口 |
| 赛前作战详情 | `GET /api/prematch-workbench/matches/{id}` | `/workbench` via `/api/public/prematch-workbench/matches/{id}` | 公开脱敏；原接口仅管理员 | 不泄露票据/金额/rawPayload |
| 完整性检查 | `GET /api/prematch-workbench/matches/{id}/integrity` | `/workbench` via `/api/public/prematch-workbench/matches/{id}/integrity` | 公开脱敏；原接口管理员 | complete/partial/blocked |
| 分析 overview | `GET /api/analysis-review/overview` | `/decisions` via `/api/public/decisions/**` | 公开脱敏；原接口管理员 | 聚合不含资金明细 |
| 分析 reports | `GET /api/analysis-review/reports` | `/decisions` | 管理员；公开摘要走 public | rawPayload 不公开 |
| 投注 plans | `GET /api/analysis-review/bet-plans/**` | `/decisions` | 管理员 | stakeSuggestion 不公开 |
| 票据 bets | `GET /api/analysis-review/bets` | `/decisions` | 管理员 | ticketNo/stake/profitLoss 不公开 |
| 复盘 reviews | `GET /api/analysis-review/reviews` | `/decisions` + public reviews | 公开摘要 + 管理员明细 | 五层复盘公开摘要 |
| 系统设置 | `GET /api/system/settings` | `/admin/settings` | 管理员 | 归档路径不公开 |

## 12. 非目标

- 不改变 `CLAUDE.md` 中的下注分析硬规则。
- 不删除 JSON 档案留存机制。
- 不把竞彩规则简化为普通体育新闻看板。
- 不为了视觉效果牺牲证据链、赔率、CLV、复盘可读性。
- 不引入大型 3D/WebGL 效果，避免性能和可访问性风险。
- 不由 Java 系统自动生成下注建议、自动加注或自动倍投。

## 13. 首轮审查意见处理结论

已纳入首轮、第二轮与第三轮子代理审查中的关键问题：

- 将旧稿中宽泛公开业务读取接口的表述改为“仅 `GET /api/public/...` 脱敏 DTO 可公开；现有富详情接口始终管理员”。
- 增加后端权限矩阵、公开 DTO 字段白名单、字段级与值级脱敏测试、`/api/public/overview` 字段级 schema。
- 明确 `/api/analysis-review/**` 原接口未登录恒 401，公开决策复盘走脱敏 `/api/public/decisions/**`。
- 明确 Basic Auth 存储、默认凭据、生产密码注入、登录回跳、401/403、`hasRole("ADMIN")` 与 `@EnableMethodSecurity`。
- 补充下注硬规则 UI 映射。
- 补充 H5 页面级布局、ResponsiveTable、FilterDrawer、safe-area、z-index、a11y 细则。
- 补充实施阶段、测试矩阵、回滚策略、旧功能入口矩阵。

## 14. 自检结论

- 核心方向已确认：赛事沉浸、五阶段决策流、PC + H5、公开只读 + 管理写入。
- 已根据第三轮子代理审查补齐 public DTO 方法限定、旧富接口始终管理员、payload 绝禁、值级脱敏测试、PC 页面级布局、ResponsiveTable props/slot 契约、旧 API 子能力矩阵、阶段边界和回滚开关语义。
- 待最终确认后进入 implementation plan。
