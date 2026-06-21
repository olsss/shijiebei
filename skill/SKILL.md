---
name: world-cup-betting-analysis
description: Use when analyzing, ticketing, recording, auditing, settling, or reviewing World Cup sports-lottery bets in this project; includes match or round analysis, odds/source checks, budget allocation, JSON bet records, ROI and CLV review.
---

# 世界杯竞彩分析与下注记录 Skill

> 本 skill 只服务当前项目。规则源自项目根目录 `CLAUDE.md`，工程方法借鉴 work-cup-2026（扁平数据）与 FIFA-WINNER-SKILL（多源快照/证据加权/JSON 存档/赛后评估）。
> 定位：**竞彩下注向**。不采用"仅供娱乐/禁止注额赔率"护栏，底线为 `rules/betting-rules.md` §8 风险边界——竞彩只能小额娱乐，不保证盈利，连败减额不加注。

## 触发条件
用户提到：分析某场/某轮、要不要下注、怎么出票、让球/比分怎么买、录入赛果、赛后复盘、算 ROI。

## 资源索引
- `rules/betting-rules.md` —— 核心下注规则镜像（CLAUDE.md §1–§15；§10 工具说明见 CLAUDE.md 与本索引）。
- `rules/temperament-rules.md` —— 大赛气质硬规则（§2A）+ 本届逐场表现硬规则（§2B）。
- `data/` —— 48 队基础数据（`48-data.md`）、赛程、小组排名（含 §2D 战意/名次提示与 §15 最佳第三名横向红线）；`data/teams/<队名>.md` 为**每队单独档案**（人员配置/球员状态含年龄身高伤病影响/更衣室与负面消息/战意排名/大赛气质）；`data/per-match/` 为本届逐场表现底稿。
- `workflow/source-snapshot.md` —— 多源交叉验证流程与快照清单格式（§2C）。
- `workflow/odds-collection.md` —— 赔率采集（The Odds API 多公司为主、体彩仅取SP）流程与庄家视角解读。
- `workflow/evidence-weight.md` —— 证据加权评分与置信度降级。
- `workflow/analysis-pipeline.md` —— 九维分析框架编排（§2）。
- `workflow/post-match-review.md` —— 五层复盘 + ROI/CLV（§5）。
- `archive/bets.json` —— 下注记录权威存档（旧 Excel 表格已弃用）；`archive/analysis/` —— 每场下注分析留档（权威，可视化页面渲染源）；`archive/sources/` —— 各场快照；`archive/odds/` —— 各场赔率存档。
- `archive/dashboard/index.html` —— 可视化看板（自包含，双击即看）；由 `build_dashboard.py` 生成。
- `scripts/` —— `fetch_odds_api.py`（The Odds API 拉多公司赔率落档）/`odds_table.py`（赔率对比：返还率/凯利/变化）/`scrape_browser.py`（备用浏览器抓取，Edge）/`new_team.py`（建每队档案）/`new_analysis.py`（建分析留档骨架）/`build_dashboard.py`（生成看板）/`new_bet.py`（写票）/`settle.py`（结算）/`review.py`（ROI+复盘骨架）/`_common.py`（共用模块）。

## 7 阶段流程

### 阶段 1 · 官方核对（betting-rules §1 / §11 / §12）
核对竞彩官方：比赛编号、开赛时间、玩法、让球数、是否开售。HAD/HHAD/比分/**总进球(TTG)/半全场(HAFU)/串关**逐玩法核对开售与封顶（§12）；HAD 与 HHAD **分开记录**。未开售玩法标记不可下注。不凭记忆写盘口。**结算口径：所有玩法只算 90 分钟（含补时），不含加时点球（§11）；淘汰赛尤其注意“晋级 ≠ 90 分钟赢”、延期/腰斩按官方公告不提前结算。**

### 阶段 2 · 数据准备 + 多源核验 + 赔率采集（§2C → source-snapshot.md / odds-collection.md）
从 `data/` 取档案，本届已赛从 `data/per-match/` 取。关键数据（赛果/首发/伤停/红黄牌/进球时间/控球/射正/角球/xG）**≥2 个独立来源**交叉验证，写快照清单到 `archive/sources/`。
**赔率采集**：以 `scripts/fetch_odds_api.py`（The Odds API）拉 ~42 家公司欧赔/让球（Pinnacle 作凯利基准，不含 bet365），存 `archive/odds/`，跑 `scripts/odds_table.py` 出返还率/凯利/变化对比表；体彩仅在出票/结算时取票面 SP。API 不可用时用 `scripts/scrape_browser.py` 备用。
来源冲突写明并按更权威者处理；预测文章不当事实。证据状态标 `complete/partial/blocked`，有缺口不许谎报完整。**数据时效（§14）：首发/伤停 >24h、档案/排名未含最新一轮或 >7 天、赔率 live 距停售 >3h 均视为过期，触发置信度降级或补采。**时间紧无法核查 → 降额或放弃。

### 阶段 3 · 逐场表现分析（§2B → rules/temperament-rules.md）
逐队逐场列本届本场之前的全部比赛（对手/比分/进失球时间/阵型变化/关键伤停红黄/控球射门质量/定位球/反击/防守稳定性/领先落后应对），判断每场是实力/战术/对手失误/运气/比分掩盖问题。无本届样本 → 写明"本届首战，无本届样本"，再补预选赛/洲际杯/历史。当前赛事 > 旧数据。

### 阶段 4 · 九维 + 大赛气质分析（§2 / §2A → workflow/evidence-weight.md）
跑九维框架（交锋/阵容/教练/小组形势/赔率/大赛履历/比赛气质/破密集/冷门路径）。证据加权：基本面+盘口为主，大赛气质·临场为辅；缺阵容/伤停 → 置信度降级。对热门方/必须赢的队反向审查（落后是否急躁、能否破低位、是否无效传中远射）。**48 队 §15 特别项：若涉及小组第三/末轮，必须查 `data/group-rank.md` 的 12 个第三名横向对比与第 8/9 红线，战意不清时降投或观望。**结论标注是**实力判断/盘口判断/大赛气质判断**，三者冲突时降低投入。

### 阶段 5 · 出票决策（§3 动态预算 / §12 玩法）
**预算不固定：本轮总额由用户给出，未给先问**（不沿用旧 100/50）。在给定额内分胜负类与低分玩法类两盘；单一逻辑 ≤ 本轮胜负类资金 60%–70%。凯利（odds_table.py）只作价值信号与相对注额权重，建议分数凯利防过注；无价值场次空仓。选玩法先问“哪种玩法表达同一判断方差最低、赔率最优”——守领先/慢热/破密集差优先 **总进球 0-1/2 球或半全场**（§12），不默认买比分。4 串 1 接受全黑风险，不追损不临场加倍。让球深盘留保险（让胜/让平/让负至少两个方向）。比分分层：主比分 + 卡线比分 + 冷门比分，覆盖 0-0/0-1/1-1/1-2 低比分路径，不单压一个比分。

### 阶段 6 · 子代理审查门（§6）
出票前按需启动：①数学/规则代理（含 90 分钟结算口径§11、玩法开售/封顶§12）②足球逻辑代理（含行为偏差自检§13）③资金分配代理（含本轮给定预算与凯利权重§3）。出现热门/深盘/必须赢/历史气质争议 → 必启 ④大赛气质代理。已有本届样本 → 必启 ⑤本届逐场表现代理。涉赛果/技术统计/伤停/首发/临场 → 必启 ⑥数据源核查代理。
末轮或可能落小组第三的场次，额外检查 §15 最佳第三名横向红线、其它组赛程先后、是否存在“自己也算不清”的战意模糊；未核清时不得重注。

### 阶段 7 · 记录 + 赛后复盘（§4 / §5 → workflow/post-match-review.md）
**每次下注分析都要留档**：每场写一份 `archive/analysis/<id>.json`（九维要点/赔率读数/结论类型/置信度/风险/推荐/长文 narrative；用 `new_analysis.py` 起骨架，默认 `status=draft`，只有补齐硬门槛后才改 `ready_to_bet`）。出票即写 `bets.json`（权威，旧 Excel 已弃用），含**收盘赔率/CLV**字段；未开奖写"待赛果/待开奖"，返还盈亏留空。票面 SP 优先，与网页赔率不同时备注"官网估算，票面 SP 优先"。每次更新后跑 `build_dashboard.py` 刷新可视化看板。开奖后 `settle.py`（带 `--closing-odds` 自动算 CLV）结算返还/净盈亏/ROI。**五层复盘：数学层/足球层/盘口层/大赛气质层/赔率价值层（CLV，§5）**——短期 ROI 是方差、CLV 才是长期水平标尺。每轮至少沉淀 1 条"下次必须避免/必须增加"，回写 `rules/`。

## 冲突优先级
当前赛事真实表现 > 旧世界杯/历史交锋。官方/竞彩数据 > 数据商/比分平台 > 主流媒体 > 预测文章。票面 SP > 网页/接口赔率。规则冲突时以 `CLAUDE.md` 为准。
