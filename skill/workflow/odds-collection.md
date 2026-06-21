# 赔率采集（世界大公司为主，体彩仅取SP，对应阶段 2/4）

> 落地 §2 维度5（主流公司赔率、赔率变化、庄家视角）+ §2C 多源核验。
> **主采集走 The Odds API**（结构化、绕开反爬）；脚本完成拉取、归一化与对比计算。

## 采集优先级（分析以世界大公司为主，体彩弱化）
> 体彩赔率去水重、参考价值低，**只在出票/结算时取票面 SP（§1）**，分析阶段不作主依据。

1. **The Odds API（首选，主采集）**：一次返回 ~42 家公司的胜平负/让球十进制赔率，含 **Pinnacle**（锐意/标尺，作凯利基准）、William Hill、Betfair、Paddy Power 等。**注意不含 bet365**。脚本 `scripts/fetch_odds_api.py`。
2. **备用浏览器抓取**：API 不可用时用 `scripts/scrape_browser.py`（Playwright + 本机 Edge）。flashscore/forebet/betexplorer 可达；**oddschecker/oddspedia 有 Cloudflare 反爬，无头浏览器也被拦**，不要依赖。
3. **体彩/竞彩官方（仅 SP）**：只取 HAD/HHAD 让球数、是否开售、票面 SP，用于出票核对与结算，不进分析主权重。
4. 预测文章不算赔率来源。

> 凯利基准用 **Pinnacle**（去水最少）。**Betfair 是交易所**，赔率系统性偏高，其相对 Pinnacle 的"凯利>1"是结构性的、**非可吃价值**，勿误读。
> 美式赔率换算：+X→X/100+1，-X→100/X+1；不采信第三方页面给的换算。

## 必采字段（每场）
- 各公司欧赔：home/draw/away；有亚盘/让球补：让球盘口 + 上下水（API 加 `--markets h2h,spreads`）。
- **初赔 vs 即时赔**：两个时间点都要（API 不同时间各拉一次，snapshot 标 open/live），用于看赔率变化（庄家视角的关键）。**停售前最后一拉的 live 即作“收盘赔率”，复盘算 CLV 用（post-match-review.md 赔率价值层）**：CLV = 下注入场赔率 ÷ 收盘赔率 − 1。
- 体彩官方：HHAD 让球数、是否开售、票面 SP（仅出票/结算用）。
- 取数时间、来源 → 写进 `archive/odds/<比赛>.json`，关键场另补 `archive/sources/`（§2C 快照）。
- 时间口径：The Odds API / FIFA match centre 常给 UTC 开赛时间；本项目 `date` / 分析 `matchday` / `bets.json` 比赛日统一转换成北京时间日期，同时保留 `date_utc`、`commence_utc`、`commence_beijing`，避免凌晨场被记到前一天。

## 流程（命令在项目根目录运行）
1. 早盘拉初赔：`python skill/scripts/fetch_odds_api.py --snapshot open --teams <队名逗号分隔>`。
2. 临场前拉即时：`python skill/scripts/fetch_odds_api.py --snapshot live --teams <同上>`（写同一场文件，自动保留 open 快照）。key 读环境变量 `ODDS_API_KEY`，新会话自动生效；当前会话无则 `--key` 传。
3. `python skill/scripts/odds_table.py <文件名>` → 输出对比表：各公司返还率、凯利指数、初赔→即时变化。
4. 出票前用体彩官方核对编号/让球/是否开售，取票面 SP（§1）。
5. 把"赔率信号"作为 evidence-weight 的主权重之一；注意区分赔率信号 vs 个人推断（§2C）。

> 凯利基准回退：若某场 Pinnacle 缺失，脚本会退用"市场均值"作基准，而均值含 Betfair 等交易所偏高赔率，**此时凯利指数仅供参考**，以返还率高的硬庄口为准重新判断。

## 庄家视角解读要点
- **返还率高**（去水少）→ 该公司更可信，凯利指数更有参考价值。
- **凯利指数 >1**：该公司对某结果给的赔率高于市场公允概率隐含值，可能是诱盘也可能是价值，结合大赛气质判断。
- **临场大幅降赔/升赔**：结合伤停、首发、天气核对（player-status 当日刷新），不可只看赔率下注。
- 竞彩 SP 与公司即时赔偏离过大时，按 §1 以票面 SP 结算，分析时标明偏离。
