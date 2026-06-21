# -*- coding: utf-8 -*-
"""生成自包含的下注分析看板 archive/dashboard/index.html（数据烤进HTML，双击即看）。

读取：archive/bets.json、archive/analysis/*.json、archive/odds/*.json，
以及 data/rules/workflow/archive/sources 等 skill 数据源。
风格：深色"竞彩交易台"，盈亏红绿语义，原生 <details> 折叠，离线无依赖。

用法：python skill/scripts/build_dashboard.py
"""
import glob
import html
import json
import os
import re
from datetime import datetime, timedelta

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ARCH = os.path.join(BASE, "archive")
OUT = os.path.join(ARCH, "dashboard", "index.html")


# ---------- 数据 ----------
def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def load_all(subdir):
    out = []
    for p in sorted(glob.glob(os.path.join(ARCH, subdir, "*.json"))):
        if os.path.basename(p).startswith("_"):
            continue
        try:
            d = load_json(p)
            d["_file"] = os.path.basename(p)
            out.append(d)
        except Exception:
            pass
    return out


def read_text(path):
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        return f.read()


def rel_skill_path(path):
    return "skill/" + os.path.relpath(path, BASE).replace("\\", "/")


def iter_skill_data_files():
    """列出页面应完整展示的 skill 文本数据文件。"""
    patterns = [
        os.path.join(BASE, "data", "**", "*.md"),
        os.path.join(BASE, "rules", "**", "*.md"),
        os.path.join(BASE, "workflow", "**", "*.md"),
        os.path.join(ARCH, "bets.json"),
        os.path.join(ARCH, "analysis", "*.json"),
        os.path.join(ARCH, "odds", "*.json"),
        os.path.join(ARCH, "sources", "*.json"),
    ]
    files = []
    for pat in patterns:
        files.extend(glob.glob(pat, recursive=True))
    return sorted(dict.fromkeys(files), key=lambda p: rel_skill_path(p))


# ---------- 计算 ----------
def payout_rate(eu):
    vals = [eu.get(k) for k in ("home", "draw", "away")]
    if any(v in (None, 0) for v in vals):
        return None
    return 1.0 / sum(1.0 / v for v in vals)


def novig(eu):
    vals = {k: eu.get(k) for k in ("home", "draw", "away")}
    if any(v in (None, 0) for v in vals.values()):
        return None
    inv = {k: 1.0 / v for k, v in vals.items()}
    s = sum(inv.values())
    return {k: inv[k] / s for k in inv}


def fmt(x, n=2):
    return "—" if x is None else f"{x:.{n}f}"


def pct(x):
    return "—" if x is None else f"{x*100:.1f}%"


def clv_fmt(x):
    if x is None:
        return "—"
    try:
        return f"{float(x)*100:+.2f}%"
    except Exception:
        return "—"


def esc(s):
    return html.escape(str(s if s is not None else ""))


def is_unfinished_analysis(an):
    res = an.get("result") or {}
    score = str(res.get("score") or "")
    return res.get("hit") is None and (not score or "待" in score)


def matchday_statuses(analyses, bets, extra_days=None):
    """收集页面可筛选日期；任一分析/票未完赛则该日标为未完赛。"""
    days = {}
    for an in analyses:
        day = an.get("matchday")
        if not day:
            continue
        days.setdefault(day, False)
        days[day] = days[day] or is_unfinished_analysis(an)
    for b in bets:
        day = b.get("比赛日")
        if not day:
            continue
        days.setdefault(day, False)
        days[day] = days[day] or (b.get("命中") is None)
    for day in extra_days or []:
        days.setdefault(day, True)
    return days


def default_matchday(analyses, bets, today=None, extra_days=None):
    """默认展示最近的、且有分析/下注内容的未完赛比赛日；都没有再回退到赛程日。

    优先级：有分析或票的未完赛未来日 > 任意未完赛未来日 > 有内容的未完赛日 >
    任意未完赛日 > 最近未来日 > 最早日。把「有内容」排在前面，避免默认落到
    只有赛程、没有分析卡的空日子（旧逻辑会落到 06-22 而分析都在 06-23）。"""
    today = today or datetime.now().strftime("%Y-%m-%d")
    days = matchday_statuses(analyses, bets, extra_days)
    content_days = {a.get("matchday") for a in analyses if a.get("matchday")}
    content_days |= {b.get("比赛日") for b in bets if b.get("比赛日")}
    unfinished = sorted(day for day, pending in days.items() if pending)

    def pick(cands):
        future = [d for d in cands if d >= today]
        return future[0] if future else (cands[0] if cands else None)

    for cands in (
        [d for d in unfinished if d in content_days],   # 有内容的未完赛
        unfinished,                                      # 任意未完赛
        sorted(days),                                    # 任意日
    ):
        got = pick(cands)
        if got:
            return got
    return ""


def date_filter_options(analyses, bets, selected, extra_days=None):
    days = matchday_statuses(analyses, bets, extra_days)
    opts = ['<option value="">全部日期</option>']
    for day in sorted(days):
        sel = " selected" if day == selected else ""
        label = "未完赛" if days[day] else "已完赛"
        opts.append(f'<option value="{esc(day)}"{sel}>{esc(day)}（{label}）</option>')
    return "".join(opts)


def markdown_table_blocks(text):
    blocks, cur = [], []
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("|") and s.endswith("|"):
            cur.append(s)
        else:
            if cur:
                blocks.append(cur)
                cur = []
    if cur:
        blocks.append(cur)
    return blocks


def split_md_row(line):
    return [c.strip() for c in line.strip().strip("|").split("|")]


def is_separator_row(cells):
    return all(re.fullmatch(r":?-{3,}:?", c.replace(" ", "")) for c in cells)


def markdown_table_html(text, css_class="btab schedule-tab"):
    tables = []
    for block in markdown_table_blocks(text):
        if len(block) < 2:
            continue
        header = split_md_row(block[0])
        body_lines = block[1:]
        if body_lines and is_separator_row(split_md_row(body_lines[0])):
            body_lines = body_lines[1:]
        if not body_lines:
            continue
        th = "".join(f"<th>{esc(h)}</th>" for h in header)
        matchday_idx = header.index("比赛日") if "比赛日" in header else None
        rows = []
        for line in body_lines:
            cells = split_md_row(line)
            if len(cells) < len(header):
                cells += [""] * (len(header) - len(cells))
            attr = ""
            if matchday_idx is not None and matchday_idx < len(cells):
                day = cells[matchday_idx]
                if re.fullmatch(r"\d{4}-\d{2}-\d{2}", day):
                    attr = f' data-matchday="{esc(day)}"'
            tds = "".join(f"<td>{esc(c)}</td>" for c in cells[:len(header)])
            rows.append(f"<tr{attr}>{tds}</tr>")
        tables.append(
            f'<table class="{css_class}"><thead><tr>{th}</tr></thead>'
            f'<tbody>{"".join(rows)}</tbody></table>'
        )
    return "".join(tables)


def schedule_matchdays():
    path = os.path.join(BASE, "data", "group-schedule.md")
    if not os.path.exists(path):
        return []
    return sorted(set(re.findall(r"\|\s*(\d{4}-\d{2}-\d{2})\s*\|", read_text(path))))


def schedule_timing_list():
    """每个比赛日 → 当日最晚一场的开球 UTC 时刻（北京时间-8h）。供前端按观看时刻判断
    哪个比赛日「尚未完赛」，从而进入页面时默认展示最近还没踢完的那一组赛程。"""
    days = {}
    for m in schedule_matches():
        day = m.get("matchday", "")
        if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", day):
            continue
        mt = re.fullmatch(r"(\d{1,2}):(\d{2})", (m.get("time") or "").strip())
        if not mt:
            continue
        kickoff = datetime.strptime(f"{day} {mt.group(1)}:{mt.group(2)}", "%Y-%m-%d %H:%M")
        utc = (kickoff - timedelta(hours=8)).strftime("%Y-%m-%dT%H:%M:%SZ")
        if day not in days or utc > days[day]:
            days[day] = utc
    return [{"matchday": d, "lastKickoffUTC": v} for d, v in sorted(days.items())]


# 末场开球后多久视为该比赛日「完赛」、可以默认翻到下一组。
FINISH_BUFFER_H = 2.5


def default_matchday_timed(timing, schedule_days, analyses, bets, now=None):
    """按当前时刻选默认比赛日：最近一个「末场开球+缓冲」仍在未来的比赛日；都踢完则取
    最后一个有赔率/分析内容的日子，再退回旧的有内容逻辑。"""
    now = now or datetime.utcnow()
    upcoming = [t for t in timing
                if datetime.strptime(t["lastKickoffUTC"], "%Y-%m-%dT%H:%M:%SZ")
                + timedelta(hours=FINISH_BUFFER_H) > now]
    if upcoming:
        return upcoming[0]["matchday"]
    if timing:
        return timing[-1]["matchday"]
    return default_matchday(analyses, bets, extra_days=schedule_days)


def schedule_matches():
    """把 group-schedule.md 的赛程表解析成业务数据。"""
    path = os.path.join(BASE, "data", "group-schedule.md")
    if not os.path.exists(path):
        return []
    out = []
    for block in markdown_table_blocks(read_text(path)):
        if len(block) < 3:
            continue
        header = split_md_row(block[0])
        rows = block[1:]
        if rows and is_separator_row(split_md_row(rows[0])):
            rows = rows[1:]
        for line in rows:
            cells = split_md_row(line)
            if len(cells) < len(header):
                cells += [""] * (len(header) - len(cells))
            row = dict(zip(header, cells))
            code = row.get("竞彩编号", "")
            match = row.get("对阵", "")
            if not code or not match:
                continue
            out.append({
                "code": code,
                "code_date": row.get("编号日期", ""),
                "matchday": row.get("比赛日", ""),
                "time": row.get("开赛时间(北京)", ""),
                "match": match,
                "group": row.get("小组", ""),
                "had_open": row.get("HAD是否开售", ""),
                "handicap": row.get("HHAD让球数", ""),
                "had_sp": row.get("HAD SP（胜/平/负）", ""),
                "hhad_sp": row.get("HHAD SP（让胜/让平/让负）", ""),
                "note": row.get("备注", ""),
            })
    return out


# 世界杯参赛队中文名 → The Odds API 英文名，用于把赛程卡片关联到赔率快照。
TEAM_CN2EN = {
    "西班牙": "Spain", "沙特": "Saudi Arabia", "沙特阿拉伯": "Saudi Arabia",
    "比利时": "Belgium", "伊朗": "Iran", "乌拉圭": "Uruguay", "佛得角": "Cape Verde",
    "新西兰": "New Zealand", "埃及": "Egypt", "阿根廷": "Argentina", "奥地利": "Austria",
    "法国": "France", "伊拉克": "Iraq", "挪威": "Norway", "塞内加尔": "Senegal",
    "约旦": "Jordan", "阿尔及利亚": "Algeria", "巴西": "Brazil", "海地": "Haiti",
    "土耳其": "Turkey", "巴拉圭": "Paraguay",
}


def _norm_en(s):
    return re.sub(r"[^a-z]", "", str(s or "").lower())


def odds_match_index(odds_list):
    """把赔率快照按 (北京比赛日, {两队英文名}) 建索引，供赛程卡片精确关联。"""
    idx = {}
    for o in odds_list or []:
        parts = re.split(r"\s+vs\s+", o.get("match", ""), flags=re.I)
        if len(parts) != 2:
            continue
        day = o.get("date_beijing") or o.get("matchday") or o.get("date") or ""
        idx[(day, frozenset(_norm_en(p) for p in parts))] = o
    return idx


def schedule_find_odds(m, idx):
    parts = re.split(r"\s*vs\s*", m.get("match", ""), flags=re.I)
    if len(parts) != 2:
        return None
    en = [TEAM_CN2EN.get(p.strip()) for p in parts]
    if not all(en):
        return None
    return idx.get((m.get("matchday", ""), frozenset(_norm_en(e) for e in en)))


def h2h_rows_html(comps, p_ref):
    """渲染多公司胜平负行（含返还率与凯利），供赔率表与赛程卡片复用。"""
    rows = []
    for c in comps:
        eu = c.get("eu", {})
        r = payout_rate(eu)
        kel = {"home": None, "draw": None, "away": None}
        if p_ref:
            for k in kel:
                o = eu.get(k)
                kel[k] = o * p_ref[k] if o else None

        def kc(v):
            return "k-pos" if (v is not None and v > 1) else ""

        klass = "refrow" if c.get("reference") else ""
        rows.append(
            f'<tr class="{klass}"><td class="bk">{esc(c.get("name"))}</td>'
            f'<td class="num">{fmt(eu.get("home"))}</td><td class="num">{fmt(eu.get("draw"))}</td>'
            f'<td class="num">{fmt(eu.get("away"))}</td><td class="num">{pct(r)}</td>'
            f'<td class="num {kc(kel["home"])}">{fmt(kel["home"],3)}</td>'
            f'<td class="num {kc(kel["draw"])}">{fmt(kel["draw"],3)}</td>'
            f'<td class="num {kc(kel["away"])}">{fmt(kel["away"],3)}</td></tr>'
        )
    return "".join(rows)


def schedule_h2h_panel(m, odds):
    had = m.get("had_open", "")
    sp = m.get("had_sp", "")
    status_cls = "status-ok" if had == "开售" else ("status-warn" if "待" in had else "status-off")
    head = (f'<div class="mkt-official"><span>竞彩官方 HAD</span>'
            f'<b class="{status_cls}">{esc(had or "—")}</b>'
            f'<span class="sp">胜/平/负 SP {esc(sp or "未开售·待采集")}</span></div>')
    if not odds:
        return head + '<div class="mkt-na">多公司胜平负赔率待采集（The Odds API）。</div>'
    comps = [c for c in odds.get("companies", []) if c.get("snapshot") == "live"] or odds.get("companies", [])
    ref = next((c for c in comps if c.get("reference")), None)
    p_ref = novig(ref["eu"]) if ref else None
    n = odds.get("books_count", len(comps))
    head_row = ('<tr><th>公司</th><th>主</th><th>平</th><th>客</th><th>返还率</th>'
                '<th>凯主</th><th>凯平</th><th>凯客</th></tr>')
    return (head + odds_source_line(odds)
            + f'<div class="mkt-scroll"><table class="otab"><thead>{head_row}</thead>'
            f'<tbody>{h2h_rows_html(comps, p_ref)}</tbody></table></div>'
            f'<div class="hint">共 {esc(n)} 家（Pinnacle 高亮为基准）；凯利&gt;1 多为 Betfair 交易所结构性高赔，非可吃价值。</div>')


def schedule_hhad_panel(m, odds):
    hc = m.get("handicap", "")
    sp = m.get("hhad_sp", "")
    head = (f'<div class="mkt-official"><span>竞彩官方 HHAD 让球 {esc(hc or "—")}</span>'
            f'<b class="status-ok">{esc(sp or "—")}</b>'
            f'<span class="sp muted">让胜/让平/让负 SP</span></div>')
    body = (spreads_html(odds) if odds else "") or '<div class="mkt-na">The Odds API 让球/亚盘待采集。</div>'
    src = ('<div class="odds-src">让球 SP 来源 <b>中国竞彩官方</b>（票面优先，§1）'
           ' · 亚盘参考来源 <b>The Odds API spreads</b></div>')
    return head + src + body


def schedule_markets(m, odds):
    tabs = [
        ("胜平负", schedule_h2h_panel(m, odds)),
        ("让球", schedule_hhad_panel(m, odds)),
        ("比分", score_html(odds) + odds_source_line(odds)),
        ("总进球", ttg_html(odds) + odds_source_line(odds)),
        ("半全场", hafu_html(odds) + odds_source_line(odds)),
    ]
    btns = "".join(
        f'<button type="button" class="tab-btn{" active" if i == 0 else ""}" data-tab="{i}">{esc(name)}</button>'
        for i, (name, _) in enumerate(tabs))
    panels = "".join(
        f'<div class="tab-panel{" active" if i == 0 else ""}" data-panel="{i}">{body}</div>'
        for i, (name, body) in enumerate(tabs))
    return f'<div class="mkt-tabs" data-tabs><div class="tab-btns">{btns}</div>{panels}</div>'


def schedule_html(odds_list=None):
    matches = schedule_matches()
    if not matches:
        return '<div class="muted">暂无赛程文件。</div>'
    idx = odds_match_index(odds_list or [])
    cards = []
    for m in matches:
        odds = schedule_find_odds(m, idx)
        odds_flag = ('<span class="odds-flag has-odds">含多玩法赔率</span>' if odds
                     else '<span class="odds-flag no-odds">赔率待采集</span>')
        note = f'<div class="card-note">{esc(m["note"])}</div>' if m.get("note") else ""
        cards.append(
            f'<article class="match-card" data-matchday="{esc(m["matchday"])}">'
            f'<div class="match-head"><span class="jc-code">{esc(m["code"])}</span>'
            f'<span class="match-time">{esc(m["matchday"])} {esc(m["time"])}</span>'
            f'<span class="group-pill">{esc(m["group"] or "—")}组</span>'
            f'<span class="group-pill">编号 {esc(m["code_date"] or "—")}</span>{odds_flag}</div>'
            f'<div class="versus">{esc(m["match"])}</div>'
            f'{schedule_markets(m, odds)}{note}</article>'
        )
    return (
        '<section class="schedule-box"><div class="section-title-mini">赛程作战台</div>'
        '<div class="muted small">按 skill/data/group-schedule.md 解析；每场展开胜平负/让球/比分/总进球/半全场五类玩法赔率，'
        '标注数据来源；出票仍以中国竞彩官方页面/票面 SP 为准（§1/§12）。</div>'
        f'<div class="match-grid">{"".join(cards)}</div></section>'
    )


def parse_md_sections(text):
    sections, current, buf = {}, None, []
    for line in text.splitlines():
        if line.startswith("## "):
            if current is not None:
                sections[current] = "\n".join(buf).strip()
            current = line[3:].strip()
            buf = []
        elif current is not None:
            buf.append(line)
    if current is not None:
        sections[current] = "\n".join(buf).strip()
    return sections


def compact_text(text, limit=180):
    clean = " ".join(str(text or "").replace("|", " ").split())
    if not clean:
        return "待补"
    return clean if len(clean) <= limit else clean[:limit] + "…"


def placeholder_score(text):
    raw = str(text or "")
    if not raw.strip():
        return 0
    bad = sum(raw.count(x) for x in ("待填", "待补", "：\n", "| - |", "来源1", "来源2"))
    if bad >= 4:
        return 25
    if bad:
        return 50
    return 85


def team_days_map():
    """从赛程把 球队名 → 它参赛的比赛日集合 映射出来，供球队情报按日筛选。"""
    out = {}
    for m in schedule_matches():
        day = m.get("matchday", "")
        if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", day or ""):
            continue
        for side in re.split(r"\s*vs\s*", m.get("match", ""), flags=re.I):
            name = side.strip()
            if name:
                out.setdefault(name, set()).add(day)
    return out


def section_to_html(text, table_class="btab intel-tab"):
    """把球队档案里的一个 markdown 段落渲染成 HTML：表格→真表格，- 列表→<ul>，
    其余→段落。解决旧 compact_text 把表格压成一行乱码的问题。"""
    raw = str(text or "").strip()
    if not raw:
        return '<p class="muted">待补</p>'
    tables = markdown_table_html(raw, css_class=table_class)
    lines = [l for l in raw.splitlines()
             if not (l.strip().startswith("|") and l.strip().endswith("|"))]
    bullets, paras = [], []
    for l in lines:
        s = l.strip()
        if not s:
            continue
        if s.startswith("- "):
            bullets.append(s[2:].strip())
        else:
            paras.append(s)
    out = []
    if paras:
        out.append("<p>" + "<br>".join(esc(p) for p in paras) + "</p>")
    if bullets:
        out.append('<ul class="intel-list">'
                   + "".join(f"<li>{esc(b)}</li>" for b in bullets) + "</ul>")
    out.append(tables)
    body = "".join(out).strip()
    return body or '<p class="muted">待补</p>'


def team_profiles():
    teams_dir = os.path.join(BASE, "data", "teams")
    out = []
    for p in sorted(glob.glob(os.path.join(teams_dir, "*.md"))):
        if os.path.basename(p).startswith("_"):
            continue
        text = read_text(p)
        name = os.path.splitext(os.path.basename(p))[0]
        sections = parse_md_sections(text)
        player_section = next((v for k, v in sections.items() if "球员状态" in k), "")
        score_parts = [
            placeholder_score(sections.get("基本档案", "")),
            placeholder_score(sections.get("人员配置", "")),
            placeholder_score(player_section),
            placeholder_score(next((v for k, v in sections.items() if "战意" in k), "")),
            placeholder_score(next((v for k, v in sections.items() if "大赛气质" in k), "")),
        ]
        completeness = round(sum(score_parts) / len(score_parts))
        out.append({
            "name": name,
            "file": rel_skill_path(p),
            "updated": (re.search(r"最后更新时间[:：]\s*([^\n；]+)", text) or [None, "未标注"])[1],
            "sections": sections,
            "basic": sections.get("基本档案", ""),
            "personnel": sections.get("人员配置", ""),
            "players": player_section,
            "motivation": next((v for k, v in sections.items() if "战意" in k), ""),
            "temperament": next((v for k, v in sections.items() if "大赛气质" in k), ""),
            "history": next((v for k, v in sections.items() if "大赛履历" in k), ""),
            "completeness": completeness,
        })
    return out


def team_intel_html():
    profiles = team_profiles()
    if not profiles:
        return '<div class="muted">暂无球队档案。</div>'
    days_map = team_days_map()
    cards = []
    for t in profiles:
        c = t["completeness"]
        cls = "q-ok" if c >= 80 else ("q-mid" if c >= 50 else "q-low")
        days = sorted(days_map.get(t["name"], set()))
        day_attr = " ".join(days)
        day_pill = ("".join(f'<span class="group-pill">{esc(d)}</span>' for d in days)
                    or '<span class="group-pill muted">未排期</span>')
        cards.append(
            f'<article class="team-card" data-matchday="{esc(day_attr)}">'
            f'<div class="team-top"><h3>{esc(t["name"])}</h3>'
            f'<span class="quality {cls}">数据完整度 {c}%</span></div>'
            f'<div class="team-updated">最后更新：{esc(t["updated"])} · 比赛日 {day_pill}</div>'
            f'<div class="intel-row"><b>基本档案</b>{section_to_html(t["basic"])}</div>'
            f'<div class="intel-row"><b>人员配置</b>{section_to_html(t["personnel"])}</div>'
            f'<div class="intel-row"><b>球员状态</b>{section_to_html(t["players"])}</div>'
            f'<div class="intel-row"><b>战意/排名</b>{section_to_html(t["motivation"])}</div>'
            f'<div class="intel-row"><b>大赛气质</b>{section_to_html(t["temperament"])}</div>'
            f'</article>'
        )
    avg = round(sum(t["completeness"] for t in profiles) / len(profiles))
    return (
        '<section class="intel-box"><div class="section-title-mini">球队情报矩阵</div>'
        f'<div class="data-gauge"><span>数据完整度</span><b>{avg}%</b>'
        '<small>按基本档案/人员/球员状态/战意/气质估算 · 默认随上方比赛日联动，仅显示当日参赛队</small></div>'
        f'<div class="team-grid">{"".join(cards)}</div></section>'
    )


def per_match_timeline_html():
    files = [p for p in sorted(glob.glob(os.path.join(BASE, "data", "per-match", "*.md"))) if not os.path.basename(p).startswith("_")]
    if not files:
        return '<div class="muted">暂无逐场表现底稿。</div>'
    items = []
    for p in files:
        text = read_text(p)
        title = (re.search(r"^#\s*(.+)", text, re.M) or [None, os.path.basename(p)])[1]
        days = sorted(set(re.findall(r"\d{4}-\d{2}-\d{2}", text)))
        day_attr = " ".join(days)
        # 按 ##(队/段) 和 ###(具体某场) 结构线性渲染，- 要点渲染成标签格
        parts, grid = [], []

        def flush_grid():
            if grid:
                parts.append(f'<div class="timeline-grid">{"".join(grid)}</div>')
                grid.clear()

        for raw in text.splitlines():
            s = raw.strip()
            if s.startswith("# ") or s.startswith(">"):
                continue
            if s.startswith("### "):
                flush_grid()
                parts.append(f'<div class="tl-sub">{esc(s[4:])}</div>')
            elif s.startswith("## "):
                flush_grid()
                parts.append(f'<div class="tl-team">{esc(s[3:])}</div>')
            elif s.startswith("- "):
                mt = re.match(r"\*\*(.+?)\*\*[:：]\s*(.*)", s[2:])
                if mt:
                    k, v = mt.group(1), mt.group(2)
                else:
                    k, v = "要点", s[2:]
                grid.append(f'<div><b>{esc(k)}</b><span>{esc(v.strip() or "待补")}</span></div>')
        flush_grid()
        body = "".join(parts) or '<div class="muted">待 §2B 逐场填写</div>'
        items.append(f'<article class="timeline-item" data-matchday="{esc(day_attr)}"><h3>{esc(title)}</h3>{body}</article>')
    return '<section class="timeline-box"><div class="section-title-mini">本届逐场表现时间线</div>' + "".join(items) + '</section>'


def data_quality_html():
    sources = load_all("sources")
    odds = load_all("odds")
    analyses = load_all("analysis")
    teams = team_profiles()
    complete_sources = sum(1 for s in sources if s.get("status") == "complete")
    live_only = sum(1 for o in odds if {c.get("snapshot") for c in o.get("companies", [])} == {"live"})
    draft_analyses = sum(1 for a in analyses if a.get("status") == "draft")
    placeholder_teams = sum(1 for t in teams if t["completeness"] < 50)
    tiles = [
        ("来源快照",
         f"{complete_sources}/{len(sources)} 完整" if sources else "暂无",
         "q-ok" if (sources and complete_sources == len(sources)) else "q-mid"),
        ("赔率快照",
         f"{live_only} 场仅 live" if live_only else (f"{len(odds)} 场齐备" if odds else "暂无"),
         "q-low" if live_only else "q-ok"),
        ("分析留档",
         f"{draft_analyses} 草稿待定稿" if draft_analyses else (f"{len(analyses)} 场已定稿" if analyses else "暂无"),
         "q-mid" if draft_analyses else "q-ok"),
        ("球队档案",
         f"{placeholder_teams} 队占位偏多" if placeholder_teams else (f"{len(teams)} 队达标" if teams else "暂无"),
         "q-low" if placeholder_teams else "q-ok"),
    ]
    html_tiles = "".join(f'<div class="quality-tile"><span>{esc(k)}</span><b class="{cls}">{esc(v)}</b></div>' for k, v, cls in tiles)
    return '<section class="quality-box"><div class="section-title-mini">数据完整度雷达</div><div class="quality-grid">' + html_tiles + '</div></section>'


def pretty_file_text(path):
    text = read_text(path)
    if path.lower().endswith(".json"):
        try:
            return json.dumps(json.loads(text), ensure_ascii=False, indent=2)
        except Exception:
            return text
    return text


def skill_data_browser_html():
    groups = [
        ("data", "基础数据/球队/赛程/排名/逐场表现"),
        ("rules", "规则镜像"),
        ("workflow", "流程说明"),
        ("archive/bets.json", "下注记录"),
        ("archive/analysis", "分析留档 JSON"),
        ("archive/odds", "赔率快照 JSON"),
        ("archive/sources", "来源快照 JSON"),
    ]
    files = iter_skill_data_files()
    sections = []
    for key, title in groups:
        prefix = f"skill/{key}"
        selected = [p for p in files if rel_skill_path(p).startswith(prefix)]
        if not selected:
            continue
        details = []
        for p in selected:
            rel = rel_skill_path(p)
            details.append(
                f'<details class="raw-file"><summary>{esc(rel)}</summary>'
                f'<pre>{esc(pretty_file_text(p))}</pre></details>'
            )
        sections.append(
            f'<details class="data-group" open><summary>{esc(title)} · {len(selected)} 文件</summary>'
            f'{"".join(details)}</details>'
        )
    return "".join(sections) or '<div class="muted">未发现可展示的 skill 数据文件。</div>'


# ---------- 迷你 markdown ----------
def md(text):
    if not text:
        return ""
    out, buf, in_ul = [], [], False
    def flush():
        if buf:
            out.append("<p>" + "<br>".join(buf) + "</p>")
            buf.clear()
    for raw in str(text).split("\n"):
        line = raw.rstrip()
        b = esc(line)
        import re
        b = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", b)
        if line.startswith("## "):
            flush()
            if in_ul:
                out.append("</ul>"); in_ul = False
            out.append("<h4>" + b[3:] + "</h4>")
        elif line.startswith("- "):
            flush()
            if not in_ul:
                out.append("<ul>"); in_ul = True
            out.append("<li>" + b[2:] + "</li>")
        elif line.strip() == "":
            flush()
            if in_ul:
                out.append("</ul>"); in_ul = False
        else:
            buf.append(b)
    flush()
    if in_ul:
        out.append("</ul>")
    return "\n".join(out)


# ---------- 组件 ----------
def stat(label, value, cls=""):
    return (f'<div class="stat"><div class="stat-val {cls}">{value}</div>'
            f'<div class="stat-lab">{label}</div></div>')


def prob_bar(imp, labels=("主胜", "平", "客胜")):
    if not imp or any(imp.get(k) is None for k in ("home", "draw", "away")):
        return '<div class="muted">无基准隐含概率</div>'
    h, d, a = imp["home"], imp["draw"], imp["away"]
    lh, ld, la = labels
    return (
        '<div class="probbar">'
        f'<div class="seg seg-h" style="width:{h*100:.1f}%"><span>{h*100:.0f}%</span></div>'
        f'<div class="seg seg-d" style="width:{d*100:.1f}%"><span>{d*100:.0f}%</span></div>'
        f'<div class="seg seg-a" style="width:{a*100:.1f}%"><span>{a*100:.0f}%</span></div>'
        '</div>'
        f'<div class="problab"><span>{esc(lh)}</span><span>{esc(ld)}</span><span>{esc(la)}</span></div>'
    )


def odds_table_html(odds):
    if not odds:
        return ""
    comps = [c for c in odds.get("companies", []) if c.get("snapshot") == "live"] or odds.get("companies", [])
    ref = next((c for c in comps if c.get("reference")), None)
    p_ref = novig(ref["eu"]) if ref else None
    rows = []
    for c in comps:
        eu = c.get("eu", {})
        r = payout_rate(eu)
        kel = {"home": None, "draw": None, "away": None}
        if p_ref:
            for k in kel:
                o = eu.get(k)
                kel[k] = o * p_ref[k] if o else None
        klass = "refrow" if c.get("reference") else ""
        def kc(v):
            if v is None:
                return ""
            return "k-pos" if v > 1 else ""
        rows.append(
            f'<tr class="{klass}"><td class="bk">{esc(c.get("name"))}</td>'
            f'<td class="num">{fmt(eu.get("home"))}</td><td class="num">{fmt(eu.get("draw"))}</td>'
            f'<td class="num">{fmt(eu.get("away"))}</td><td class="num">{pct(r)}</td>'
            f'<td class="num {kc(kel["home"])}">{fmt(kel["home"],3)}</td>'
            f'<td class="num {kc(kel["draw"])}">{fmt(kel["draw"],3)}</td>'
            f'<td class="num {kc(kel["away"])}">{fmt(kel["away"],3)}</td></tr>'
        )
    head = ('<tr><th>公司</th><th>主</th><th>平</th><th>客</th><th>返还率</th>'
            '<th>凯主</th><th>凯平</th><th>凯客</th></tr>')
    n = odds.get("books_count", "")
    return (f'<details class="odds"><summary>胜平负 · 多公司赔率对比 · 共 {n} 家（Pinnacle 为基准）</summary>'
            f'{odds_source_line(odds)}'
            f'<div class="tscroll"><table class="otab"><thead>{head}</thead><tbody>{"".join(rows)}</tbody></table></div>'
            '<div class="hint">凯利&gt;1（高亮）多为 Betfair 交易所结构性高赔，非可吃价值；以返还率高的硬庄口为准。</div>'
            '</details>')


def odds_source_line(odds):
    """赔率来源标注：数据商、家数、快照口径/时间、是否含 bet365、低分玩法来源。"""
    if not odds:
        return ""
    src = odds.get("source") or "未标注"
    n = odds.get("books_count") or len(odds.get("companies", []))
    comps = odds.get("companies", [])
    snaps = sorted({c.get("snapshot") for c in comps if c.get("snapshot")})
    times = sorted({(c.get("time") or "")[:16] for c in comps if c.get("time")})
    bits = [f'胜平负来源 <b>{esc(src)}</b>', f'{esc(n)} 家',
            ('/'.join(esc(s) for s in snaps) or "—") + " 快照"]
    if times:
        bits.append(f'最新 {esc(times[-1])}')
    if "bet365" in (odds.get("note") or ""):
        bits.append("不含 bet365")
    cm = odds.get("cn_markets") or {}
    if cm.get("source"):
        extra = esc(cm["source"]) + (f' · {esc(cm.get("snapshot_time"))}' if cm.get("snapshot_time") else "")
        bits.append(f'比分/总进球/半全场来源 <b>{extra}</b>')
    return '<div class="odds-src">' + " · ".join(bits) + "</div>"


def cn_markets_source_line(odds):
    """比分/总进球/半全场来源标注，避免误读为 The Odds API totals。"""
    cm = (odds or {}).get("cn_markets") or {}
    bits = [f'比分/总进球/半全场来源 <b>{esc(cm.get("source") or "待采集")}</b>']
    if cm.get("snapshot_time"):
        bits.append(esc(cm["snapshot_time"]))
    if cm.get("code"):
        bits.append(f'竞彩编号 {esc(cm["code"])}')
    return '<div class="odds-src">' + " · ".join(bits) + "</div>"


def market_status(odds):
    """返回 (had_open, hhad, hhad_implied)。判断竞彩胜平负是否开售；未开售时用让球。"""
    if not odds:
        return True, None, None
    off = odds.get("official", {}) or {}
    cm_open = (odds.get("cn_markets") or {}).get("open") or {}
    # 默认按「开售/未知」处理，只有被明确标记 false（cn_markets.open.HAD 或
    # official.HAD_open）才判未开售；官方 SP 字段为 null 只是没填，不代表停售。
    had_open = True
    if cm_open.get("HAD") is not None:
        had_open = cm_open["HAD"]
    elif off.get("HAD_open") is not None:
        had_open = off.get("HAD_open")
    hhad = off.get("HHAD") or {}
    himp = None
    if all(hhad.get(k) for k in ("home", "draw", "away")):
        himp = novig({"home": hhad["home"], "draw": hhad["draw"], "away": hhad["away"]})
    return bool(had_open), hhad, himp


def score_html(odds):
    bf = (odds or {}).get("cn_markets", {}).get("比分") if odds else None
    if not bf:
        return '<div class="mkt-na">比分赔率待采集（来源：中国竞彩网官方 Sporttery 计算器）。The Odds API 无比分（correct_score 市场不存在），比分只走中国竞彩。</div>'
    cols = []
    if any(isinstance(v, dict) for v in bf.values()):
        order = ["主胜", "平局", "客胜", "其他"]
        for g in order + [k for k in bf if k not in order]:
            sub = bf.get(g)
            if not isinstance(sub, dict):
                continue
            rows = "".join(f'<div class="row"><em>{esc(k)}</em><span>{fmt(v)}</span></div>'
                           for k, v in sub.items())
            cols.append(f'<div class="score-col"><b>{esc(g)}</b>{rows}</div>')
    else:
        rows = "".join(f'<div class="row"><em>{esc(k)}</em><span>{fmt(v)}</span></div>'
                       for k, v in bf.items())
        cols.append(f'<div class="score-col"><b>比分</b>{rows}</div>')
    return f'<div class="score-grid">{"".join(cols)}</div>'


def ttg_html(odds):
    tg = (odds or {}).get("cn_markets", {}).get("总进球") if odds else None
    if not tg:
        return '<div class="mkt-na">总进球（竞彩 0-7+ 区间）待采集（来源：中国竞彩网官方 Sporttery 计算器）。The Odds API 仅有大小球 over/under 线，无竞彩总进球区间。</div>'
    order = ["0", "1", "2", "3", "4", "5", "6", "7+"]
    keys = [k for k in order if k in tg] + [k for k in tg if k not in order]
    cells = "".join(f'<div><b>{esc(k)}</b><span>{fmt(tg[k])}</span></div>' for k in keys)
    return f'<div class="ttg-row">{cells}</div>'


def hafu_html(odds):
    hf = (odds or {}).get("cn_markets", {}).get("半全场") if odds else None
    if not hf:
        return '<div class="mkt-na">半全场（HT/FT 九选一）待采集（来源：中国竞彩网官方 Sporttery 计算器）。注：The Odds API 有上/下半场胜平负，但无竞彩半全场（HT/FT）市场。</div>'
    order = ["胜/胜", "胜/平", "胜/负", "平/胜", "平/平", "平/负", "负/胜", "负/平", "负/负"]
    keys = [k for k in order if k in hf] + [k for k in hf if k not in order]
    cells = "".join(f'<div><b>{esc(k)}</b><span>{fmt(hf[k])}</span></div>' for k in keys)
    return f'<div class="hafu-grid">{cells}</div>'


def spreads_html(odds):
    mk = (odds or {}).get("markets") or {}
    sp = mk.get("让球(spreads)") or mk.get("spreads")
    if sp and sp.get("companies"):
        rows = "".join(
            f'<tr><td class="bk">{esc(c.get("name"))}</td><td class="num">{esc(c.get("point"))}</td>'
            f'<td class="num">{fmt(c.get("home"))}</td><td class="num">{fmt(c.get("away"))}</td></tr>'
            for c in sp["companies"])
        return ('<div class="mkt-h">让球/亚盘（The Odds API spreads，仅作让球胜负参考）</div>'
                '<div class="tscroll"><table class="otab ptable">'
                f'<thead><tr><th>公司</th><th>盘口</th><th>主</th><th>客</th></tr></thead>'
                f'<tbody>{rows}</tbody></table></div>')
    return ""


def extra_markets_html(odds):
    """只展示用户指定玩法：让球（The Odds API）+ 比分/总进球/半全场（中国竞彩）。"""
    blocks = "".join(
        f'<div class="mkt-wrap"><div class="mkt-h">{name}</div>{body}</div>'
        for name, body in (("比分（90分钟）", score_html(odds)),
                           ("总进球（90分钟）", ttg_html(odds)),
                           ("半全场（90分钟）", hafu_html(odds))))
    return ('<details class="odds"><summary>更多玩法 · 让球 / 比分 / 总进球 / 半全场（§12）</summary>'
            f'{cn_markets_source_line(odds)}{spreads_html(odds)}{blocks}'
            '<div class="hint">比分/总进球(区间)/半全场为中国竞彩玩法（来源：中国竞彩网官方 Sporttery 计算器），一律 90 分钟结算（§11）；'
            'The Odds API 不提供这三类，本看板仅展示胜平负与让球等已请求市场。空缺为未采集，灌数后自动显示。</div>'
            '</details>')


def conf_class(c):
    return {"高": "c-hi", "中": "c-mid", "低": "c-lo"}.get(c, "c-mid")


def analysis_card(an, odds_by_file):
    m = esc(an.get("match"))
    md_day = esc(an.get("matchday"))
    ctype = esc(an.get("conclusion_type"))
    conf = an.get("confidence", "")
    odds = odds_by_file.get(an.get("odds_file"))
    ref = an.get("odds_ref", {})
    had_open, hhad, himp = market_status(odds)
    bar_imp = ref.get("implied")
    bar_labels = ("主胜", "平", "客胜")
    banner = ""
    refline = ""
    if ref.get("home"):
        refline = (f'<div class="refodds">{esc(ref.get("book","Pinnacle"))} '
                   f'<b>{fmt(ref.get("home"))}</b> / {fmt(ref.get("draw"))} / {fmt(ref.get("away"))}</div>')
    if not had_open:
        banner = ('<div class="mkt-banner">⚠ 竞彩胜平负未开售（§7/§12）——改看让球（HHAD）与下方比分/总进球/半全场低分玩法</div>')
        if hhad and all(hhad.get(k) for k in ("home", "draw", "away")):
            bar_labels = ("让胜", "让平", "让负")
            if himp:
                bar_imp = himp
            refline = (f'<div class="refodds">让球 {esc(hhad.get("handicap",""))} '
                       f'<b>{fmt(hhad.get("home"))}</b> / {fmt(hhad.get("draw"))} / {fmt(hhad.get("away"))}'
                       f'<span class="muted"> · 竞彩官方让球 SP</span></div>')
    flags = "".join(f'<li>{esc(x)}</li>' for x in an.get("value_flags", []))
    flags_html = f'<ul class="flags">{flags}</ul>' if flags else ""
    risks = "".join(f'<li>{esc(x)}</li>' for x in an.get("risks", []))
    risks_html = f'<div class="risk"><div class="risk-h">风险提示</div><ul>{risks}</ul></div>' if risks else ""
    dims = an.get("dimensions", {})
    dim_html = "".join(
        f'<div class="dim"><div class="dim-k">{esc(k)}</div><div class="dim-v">{esc(v)}</div></div>'
        for k, v in dims.items() if v)
    rec = an.get("recommended", [])
    rec_html = ""
    if rec:
        items = "".join(
            f'<li><b>{esc(r.get("type"))}</b> {esc(r.get("content"))} '
            f'<span class="muted">{esc(r.get("reason"))}</span></li>' for r in rec)
        rec_html = f'<div class="rec"><div class="risk-h">推荐方向</div><ul>{items}</ul></div>'
    else:
        rec_html = '<div class="rec muted">暂无推荐票（待出票前完成核对）</div>'
    res = an.get("result", {})
    res_badge = f'<span class="resbadge">{esc(res.get("score","待赛果"))}</span>'
    return (
        f'<article class="card" data-matchday="{md_day}">'
        '<div class="card-top">'
        f'<div><h3>{m}</h3><div class="meta">{md_day} · 竞彩 {esc(an.get("jc_code"))}</div></div>'
        f'<div class="badges"><span class="badge bt">{ctype}</span>'
        f'<span class="badge {conf_class(conf)}">置信度 {esc(conf)}</span>{res_badge}</div>'
        '</div>'
        f'{banner}{refline}{prob_bar(bar_imp, bar_labels)}{flags_html}'
        f'<div class="dims">{dim_html}</div>'
        f'{risks_html}{rec_html}'
        f'<details class="narr"><summary>展开综合分析</summary><div class="narr-body">{md(an.get("narrative_md"))}</div></details>'
        f'{odds_table_html(odds)}{extra_markets_html(odds)}'
        '</article>'
    )


def analysis_owner(an):
    i = str(an.get("id", "")).lower()
    if i.endswith("-gpt") or i.endswith("-codex"):
        return {"key": "gpt", "label": "GPT"}
    if i.endswith("-claude"):
        return {"key": "claude", "label": "Claude"}
    src = an.get("决策方") or an.get("owner")
    if src:
        return decision_source_meta(src)
    return {"key": "other", "label": "分析"}


def analysis_owner_block(an):
    """单方（GPT/Claude/…）的结论块，用于合并卡内并列展示。"""
    own = analysis_owner(an)
    ctype = esc(an.get("conclusion_type"))
    conf = an.get("confidence", "")
    flags = "".join(f'<li>{esc(x)}</li>' for x in an.get("value_flags", []))
    flags_html = f'<ul class="flags">{flags}</ul>' if flags else ""
    risks = "".join(f'<li>{esc(x)}</li>' for x in an.get("risks", []))
    risks_html = f'<div class="risk"><div class="risk-h">风险提示</div><ul>{risks}</ul></div>' if risks else ""
    dims = an.get("dimensions", {})
    dim_html = "".join(
        f'<div class="dim"><div class="dim-k">{esc(k)}</div><div class="dim-v">{esc(v)}</div></div>'
        for k, v in dims.items() if v)
    rec = an.get("recommended", [])
    if rec:
        items = "".join(
            f'<li><b>{esc(r.get("type"))}</b> {esc(r.get("content"))} '
            f'<span class="muted">{esc(r.get("reason"))}</span>'
            + (f' <span class="rec-stake">{fmt(r.get("stake"))}元 @{fmt(r.get("odds"))}</span>' if r.get("stake") else "")
            + '</li>' for r in rec)
        rec_html = f'<div class="rec"><div class="risk-h">推荐方向</div><ul>{items}</ul></div>'
    else:
        rec_html = '<div class="rec muted">暂无推荐票（待出票前完成核对）</div>'
    res = an.get("result", {})
    res_badge = f'<span class="resbadge">{esc(res.get("score","待赛果"))}</span>'
    return (
        f'<div class="owner-block owner-{own["key"]}">'
        f'<div class="owner-head">{decision_source_badge(own["label"])}'
        f'<span class="badge bt">{ctype}</span>'
        f'<span class="badge {conf_class(conf)}">置信度 {esc(conf)}</span>{res_badge}</div>'
        f'{flags_html}{rec_html}{risks_html}'
        f'<details class="narr"><summary>展开综合分析 / 九维</summary>'
        f'<div class="narr-body">{md(an.get("narrative_md"))}</div>'
        f'<div class="dims">{dim_html}</div></details>'
        '</div>'
    )


def analysis_group_card(group, odds_by_file):
    """同一场比赛的多方分析合并到一张卡：共享盘口/赔率，内部并列各决策方结论块。"""
    base = group[0]
    m = esc(base.get("match"))
    md_day = esc(base.get("matchday"))
    ref_an = next((a for a in group if a.get("odds_ref", {}).get("home")), base)
    odds = odds_by_file.get(ref_an.get("odds_file"))
    ref = ref_an.get("odds_ref", {})
    had_open, hhad, himp = market_status(odds)
    bar_imp = ref.get("implied")
    bar_labels = ("主胜", "平", "客胜")
    banner = ""
    refline = ""
    if ref.get("home"):
        refline = (f'<div class="refodds">{esc(ref.get("book","Pinnacle"))} '
                   f'<b>{fmt(ref.get("home"))}</b> / {fmt(ref.get("draw"))} / {fmt(ref.get("away"))}</div>')
    if not had_open:
        banner = ('<div class="mkt-banner">⚠ 竞彩胜平负未开售（§7/§12）——改看让球（HHAD）与下方比分/总进球/半全场低分玩法</div>')
        if hhad and all(hhad.get(k) for k in ("home", "draw", "away")):
            bar_labels = ("让胜", "让平", "让负")
            if himp:
                bar_imp = himp
            refline = (f'<div class="refodds">让球 {esc(hhad.get("handicap",""))} '
                       f'<b>{fmt(hhad.get("home"))}</b> / {fmt(hhad.get("draw"))} / {fmt(hhad.get("away"))}'
                       f'<span class="muted"> · 竞彩官方让球 SP</span></div>')
    blocks = "".join(analysis_owner_block(a) for a in group)
    return (
        f'<article class="card" data-matchday="{md_day}">'
        '<div class="card-top">'
        f'<div><h3>{m}</h3><div class="meta">{md_day} · 竞彩 {esc(base.get("jc_code"))}</div></div>'
        f'<div class="badges"><span class="badge">{len(group)} 方分析</span></div>'
        '</div>'
        f'{banner}{refline}{prob_bar(bar_imp, bar_labels)}'
        f'<div class="owner-blocks">{blocks}</div>'
        f'{odds_table_html(odds)}{extra_markets_html(odds)}'
        '</article>'
    )


def decision_source_meta(raw):
    text = str(raw or "").strip()
    low = text.lower()
    if "claude" in low:
        return {"key": "claude", "label": "Claude", "raw": text or "Claude"}
    if "gpt" in low:
        return {"key": "gpt", "label": "GPT", "raw": text or "gpt"}
    if "codex" in low:
        return {"key": "gpt", "label": "GPT", "raw": text or "codex"}
    return {"key": "other", "label": text or "未标注", "raw": text or "未标注"}


def decision_source_badge(raw):
    meta = decision_source_meta(raw)
    title = f' title="{esc(meta["raw"])}"' if meta["raw"] != meta["label"] else ""
    return f'<span class="source-badge source-{meta["key"]}"{title}>{esc(meta["label"])}</span>'


def decision_source_summary_html(bets):
    groups = {}
    order = {"claude": 0, "gpt": 1, "other": 2}
    for b in bets:
        meta = decision_source_meta(b.get("决策方"))
        key = meta["key"]
        g = groups.setdefault(key, {
            "label": meta["label"], "raw": meta["raw"], "count": 0, "stake": 0.0,
            "settled": 0, "pending": 0, "ret": 0.0, "pnl": 0.0
        })
        g["count"] += 1
        g["stake"] += float(b.get("投入") or 0)
        if b.get("命中") is None:
            g["pending"] += 1
        elif b.get("盈亏") is not None:
            g["settled"] += 1
            g["ret"] += float(b.get("返还") or 0)
            g["pnl"] += float(b.get("盈亏") or 0)
    if not groups:
        return '<div class="source-summary muted">暂无下注记录。</div>'
    cards = []
    for key, g in sorted(groups.items(), key=lambda kv: (order.get(kv[0], 9), kv[1]["label"])):
        roi = None
        if g["settled"] and g["stake"]:
            # 未结算票仍在 stake 中；这里的 ROI 用该决策方当前总投入作保守对比口径。
            roi = g["pnl"] / g["stake"] * 100
        pnl_cls = "pos" if g["pnl"] > 0 else ("neg" if g["pnl"] < 0 else "")
        roi_text = "—" if roi is None else f"{roi:+.1f}%"
        cards.append(
            f'<div class="source-card source-card-{key}">'
            f'<div class="source-card-top">{decision_source_badge(g["raw"])}'
            f'<span class="muted">{g["count"]} 笔 · 待开奖 {g["pending"]}</span></div>'
            f'<div class="source-card-grid">'
            f'<div><b>¥{g["stake"]:.0f}</b><span>总投入</span></div>'
            f'<div><b>¥{g["ret"]:.0f}</b><span>返还</span></div>'
            f'<div><b class="{pnl_cls}">{g["pnl"]:+.0f}</b><span>盈亏</span></div>'
            f'<div><b>{roi_text}</b><span>ROI</span></div>'
            f'</div></div>'
        )
    return f'<section class="source-summary"><h3>决策方对比</h3><div class="source-cards">{"".join(cards)}</div></section>'


def bets_table(bets):
    rows = []
    for b in bets:
        src = decision_source_meta(b.get("决策方"))
        src_cls = f'bet-source-{src["key"]}'
        pnl = b.get("盈亏")
        pcls = "" if pnl is None else ("pos" if pnl > 0 else ("neg" if pnl < 0 else ""))
        hit = b.get("命中")
        hit_s = "待开奖" if hit is None else ("命中" if hit else "未中")
        hit_c = "" if hit is None else ("pos" if hit else "neg")
        clv = b.get("CLV")
        clv_cls = "" if clv is None else ("pos" if clv > 0 else ("neg" if clv < 0 else ""))
        matchday = esc(b.get("比赛日"))
        rows.append(
            f'<tr class="bet-row {src_cls}" data-matchday="{matchday}">'
            f'<td>{decision_source_badge(b.get("决策方"))}</td>'
            f'<td>{esc(b.get("比赛"))}</td><td>{esc(b.get("类型"))}</td>'
            f'<td>{esc(b.get("下注内容"))}</td><td class="num">{esc(b.get("投入"))}</td>'
            f'<td class="num">{esc(b.get("赔率") if b.get("赔率") is not None else "—")}</td>'
            f'<td class="num">{esc(b.get("收盘赔率") if b.get("收盘赔率") is not None else "—")}</td>'
            f'<td class="num {clv_cls}">{esc(clv_fmt(clv))}</td>'
            f'<td>{esc(b.get("结算口径") or "90分钟")}</td>'
            f'<td>{esc(b.get("赛果"))}</td><td class="{hit_c}">{hit_s}</td>'
            f'<td class="num">{esc(b.get("返还") if b.get("返还") is not None else "—")}</td>'
            f'<td class="num {pcls}">{esc(pnl) if pnl is not None else "—"}</td></tr>'
        )
    head = ('<tr><th>决策方</th><th>比赛</th><th>类型</th><th>下注内容</th><th>投入</th><th>赔率</th>'
            '<th>收盘赔率</th><th>CLV</th><th>结算口径</th>'
            '<th>赛果</th><th>命中</th><th>返还</th><th>盈亏</th></tr>')
    return (f'{decision_source_summary_html(bets)}'
            f'<div class="tscroll tscroll-bets"><table class="btab">'
            f'<thead>{head}</thead><tbody>{"".join(rows)}</tbody></table></div>')


# ---------- 主 ----------
def main():
    bets_doc = load_json(os.path.join(ARCH, "bets.json"))
    bets = bets_doc.get("bets", [])
    analyses = load_all("analysis")
    odds_list = load_all("odds")
    odds_by_file = {o["_file"]: o for o in odds_list}
    # 同一场内按决策方排序：GPT 在前、Claude 在后，其它居末（决策方对比区块仍 Claude 优先，互不影响）
    def _owner_rank(a):
        i = str(a.get("id", "")).lower()
        if i.endswith("-gpt") or i.endswith("-codex"):
            return 0
        if i.endswith("-claude"):
            return 1
        return 2
    analyses.sort(key=lambda a: (a.get("matchday", ""), a.get("match", ""), _owner_rank(a)))

    settled = [b for b in bets if b.get("盈亏") is not None]
    invest = sum(b["投入"] for b in bets)
    s_invest = sum(b["投入"] for b in settled)
    ret = sum(b["返还"] for b in settled)
    pnl = round(ret - s_invest, 2)
    roi = round(pnl / s_invest * 100, 2) if s_invest else None
    hits = [b for b in settled if b.get("命中")]
    hitrate = round(len(hits) / len(settled) * 100, 1) if settled else None
    pending = [b for b in bets if b.get("命中") is None]

    pnl_cls = "pos" if pnl > 0 else ("neg" if pnl < 0 else "")
    stats = (
        stat("总投入", f"¥{invest:.0f}") +
        stat("已结算返还", f"¥{ret:.0f}") +
        stat("净盈亏", f"{'+' if pnl>0 else ''}{pnl:.0f}", pnl_cls) +
        stat("ROI", "—" if roi is None else f"{roi:+.1f}%", pnl_cls) +
        stat("命中率", "—" if hitrate is None else f"{hitrate:.0f}%") +
        stat("待开奖", str(len(pending)))
    )

    groups = []
    gmap = {}
    for a in analyses:
        k = (a.get("matchday", ""), a.get("match", ""))
        if k not in gmap:
            gmap[k] = []
            groups.append(gmap[k])
        gmap[k].append(a)
    cards = "".join(analysis_group_card(g, odds_by_file) for g in groups)
    if not cards:
        cards = '<div class="muted" style="padding:40px">暂无分析留档。用 new_analysis.py 生成。</div>'
    schedule_days = schedule_matchdays()
    timing = schedule_timing_list()
    default_day = default_matchday_timed(timing, schedule_days, analyses, bets)
    date_options = date_filter_options(analyses, bets, default_day, extra_days=schedule_days)

    body = HTML.replace("__STATS__", stats) \
               .replace("__SCHEDULE__", schedule_html(odds_list)) \
               .replace("__TIMING_JSON__", json.dumps(timing, ensure_ascii=False)) \
               .replace("__TEAM_INTEL__", team_intel_html()) \
               .replace("__PER_MATCH_TIMELINE__", per_match_timeline_html()) \
               .replace("__DATA_QUALITY__", data_quality_html()) \
               .replace("__CARDS__", cards) \
               .replace("__BETS__", bets_table(bets)) \
               .replace("__DATE_OPTIONS__", date_options) \
               .replace("__EDITION__", esc(bets_doc.get("edition", ""))) \
               .replace("__NA__", str(len(analyses))) \
               .replace("__NB__", str(len(bets)))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(body)
    print("已生成看板:", os.path.relpath(OUT, BASE))
    print("双击打开:", OUT)


HTML = """<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<meta name="format-detection" content="telephone=no">
<title>世界杯竞彩 · 下注分析看板</title>
<style>
:root{
  --bg:#0a0c10; --panel:#13161d; --panel2:#171b23; --line:#252b36;
  --txt:#e6edf3; --mut:#8b949e; --acc:#e8b84b; --acc2:#58a6ff;
  --pos:#3fb950; --neg:#f85149; --draw:#6e7681;
  --mono:ui-monospace,"Cascadia Mono","Consolas","JetBrains Mono",monospace;
  --cjk:"PingFang SC","Microsoft YaHei","Hiragino Sans GB",sans-serif;
}
*{box-sizing:border-box}
html,body{margin:0}
body{
  background:
    repeating-linear-gradient(0deg,transparent,transparent 39px,rgba(255,255,255,.015) 39px,rgba(255,255,255,.015) 40px),
    radial-gradient(1200px 600px at 80% -10%,rgba(232,184,75,.06),transparent),
    var(--bg);
  color:var(--txt); font-family:var(--cjk); line-height:1.5;
  padding:32px 28px 80px; max-width:1180px; margin:0 auto;
  -webkit-text-size-adjust:100%; text-size-adjust:100%; overflow-x:hidden;
}
.num,.mono{font-family:var(--mono);font-variant-numeric:tabular-nums}
.muted{color:var(--mut)}
header{border-bottom:1px solid var(--line);padding-bottom:18px;margin-bottom:24px;
  display:flex;justify-content:space-between;align-items:flex-end;gap:20px;flex-wrap:wrap}
header h1{font-size:26px;margin:0;letter-spacing:.5px;font-weight:800}
header h1 .dot{color:var(--acc)}
header .sub{font-family:var(--mono);font-size:12px;color:var(--mut);letter-spacing:1px;text-transform:uppercase;margin-top:6px}
.statbar{display:grid;grid-template-columns:repeat(6,1fr);gap:1px;background:var(--line);
  border:1px solid var(--line);border-radius:10px;overflow:hidden;margin-bottom:30px}
.stat{background:var(--panel);padding:16px 14px}
.stat-val{font-family:var(--mono);font-size:24px;font-weight:700;letter-spacing:.5px}
.stat-lab{font-size:11px;color:var(--mut);margin-top:4px;letter-spacing:1px}
.pos{color:var(--pos)} .neg{color:var(--neg)}
.section-h{font-size:13px;letter-spacing:2px;color:var(--mut);text-transform:uppercase;
  margin:0 0 14px;display:flex;align-items:center;gap:10px}
.section-h::before{content:"";width:3px;height:14px;background:var(--acc);display:inline-block}
.cards{display:flex;flex-direction:column;gap:16px;margin-bottom:36px}
.card{background:var(--panel);border:1px solid var(--line);border-left:3px solid var(--acc);
  border-radius:10px;padding:18px 20px;animation:rise .5s both}
.card:nth-child(2){animation-delay:.06s}.card:nth-child(3){animation-delay:.12s}
.card:nth-child(4){animation-delay:.18s}.card:nth-child(5){animation-delay:.24s}
@keyframes rise{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}
.card-top{display:flex;justify-content:space-between;align-items:flex-start;gap:14px;flex-wrap:wrap}
.card h3{margin:0;font-size:19px;font-weight:700}
.meta{font-family:var(--mono);font-size:12px;color:var(--mut);margin-top:3px}
.badges{display:flex;gap:6px;flex-wrap:wrap;align-items:center}
.badge{font-size:11px;padding:3px 9px;border-radius:20px;border:1px solid var(--line);white-space:nowrap}
.badge.bt{background:rgba(88,166,255,.12);border-color:rgba(88,166,255,.35);color:var(--acc2)}
.source-badge{display:inline-flex;align-items:center;justify-content:center;font-size:11px;font-weight:800;
  letter-spacing:.5px;padding:3px 9px;border-radius:999px;border:1px solid var(--line);white-space:nowrap}
.source-gpt{background:rgba(88,166,255,.15);border-color:rgba(88,166,255,.5);color:#9bd1ff}
.source-claude{background:rgba(188,140,255,.15);border-color:rgba(188,140,255,.5);color:#d6b8ff}
.source-other{background:rgba(110,118,129,.16);border-color:rgba(110,118,129,.45);color:#c9d1d9}
.c-hi{background:rgba(63,185,80,.12);border-color:rgba(63,185,80,.4);color:var(--pos)}
.c-mid{background:rgba(232,184,75,.12);border-color:rgba(232,184,75,.4);color:var(--acc)}
.c-lo{background:rgba(248,81,73,.1);border-color:rgba(248,81,73,.35);color:var(--neg)}
.resbadge{font-family:var(--mono);font-size:11px;padding:3px 9px;border-radius:20px;
  background:var(--panel2);border:1px dashed var(--line);color:var(--mut)}
.refodds{font-family:var(--mono);font-size:13px;color:var(--mut);margin:14px 0 8px}
.refodds b{color:var(--acc);font-size:15px}
.probbar{display:flex;height:26px;border-radius:6px;overflow:hidden;border:1px solid var(--line)}
.seg{display:flex;align-items:center;justify-content:center;min-width:0}
.seg span{font-family:var(--mono);font-size:11px;font-weight:700;color:#0a0c10}
.seg-h{background:var(--acc)} .seg-d{background:var(--draw)} .seg-a{background:var(--acc2)}
.problab{display:flex;justify-content:space-between;font-size:11px;color:var(--mut);margin-top:4px}
.problab span:nth-child(2){flex:0}
ul.flags{margin:10px 0 0;padding-left:18px}
ul.flags li{font-size:12px;color:var(--acc);list-style:"▲ "}
.owner-blocks{display:flex;flex-direction:column;gap:14px;margin-top:8px}
.owner-block{border:1px solid var(--line);border-radius:8px;padding:12px 13px;background:var(--panel2)}
.owner-block.owner-claude{border-left:3px solid #a371f7}
.owner-block.owner-gpt{border-left:3px solid #2ea043}
.owner-head{display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:8px}
.rec-stake{color:var(--acc);font-family:var(--mono);font-size:11.5px;white-space:nowrap}
.dims{display:grid;grid-template-columns:repeat(3,1fr);gap:1px;background:var(--line);
  border:1px solid var(--line);border-radius:8px;overflow:hidden;margin:14px 0}
.dim{background:var(--panel2);padding:9px 11px}
.dim-k{font-size:11px;color:var(--acc);letter-spacing:.5px}
.dim-v{font-size:12.5px;color:var(--txt);margin-top:2px}
.risk,.rec{margin:12px 0;padding:11px 13px;border-radius:8px;background:var(--panel2);border:1px solid var(--line)}
.risk{border-color:rgba(248,81,73,.3)}
.risk-h{font-size:12px;letter-spacing:1px;margin-bottom:5px;color:var(--neg)}
.rec .risk-h{color:var(--pos)}
.risk ul,.rec ul{margin:0;padding-left:18px;font-size:12.5px}
.risk li{color:#ffb3ae}
details{margin-top:12px}
summary{cursor:pointer;font-size:12.5px;color:var(--acc2);user-select:none;
  padding:8px 0;border-top:1px solid var(--line)}
summary:hover{color:var(--acc)}
.narr-body{font-size:13.5px;color:#cdd6e0;padding:6px 2px}
.narr-body h4{color:var(--acc);font-size:14px;margin:14px 0 6px}
.narr-body strong{color:var(--txt)}
table{width:100%;border-collapse:collapse;font-size:12.5px}
.otab,.btab{margin-top:8px}
th{text-align:right;font-size:11px;color:var(--mut);font-weight:600;padding:7px 8px;
  border-bottom:1px solid var(--line);letter-spacing:.5px}
th:first-child{text-align:left}
td{padding:7px 8px;border-bottom:1px solid rgba(37,43,54,.5)}
td.num{text-align:right;font-family:var(--mono);font-variant-numeric:tabular-nums}
td.bk{color:var(--txt)}
tr.refrow td{background:rgba(232,184,75,.07)} tr.refrow td.bk{color:var(--acc);font-weight:700}
.k-pos{color:var(--pos)}
.hint{font-size:11px;color:var(--mut);margin-top:8px;padding-left:2px}
.btab td:first-child{color:var(--txt)}
.source-summary{background:var(--panel);border:1px solid var(--line);border-radius:12px;padding:14px;margin:0 0 14px}
.source-summary h3{margin:0 0 10px;font-size:15px;letter-spacing:1px;color:var(--txt)}
.source-cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:10px}
.source-card{background:var(--panel2);border:1px solid var(--line);border-left:3px solid var(--draw);border-radius:10px;padding:11px}
.source-card-gpt{border-left-color:var(--acc2);background:linear-gradient(145deg,rgba(88,166,255,.08),var(--panel2))}
.source-card-claude{border-left-color:#bc8cff;background:linear-gradient(145deg,rgba(188,140,255,.08),var(--panel2))}
.source-card-top{display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap;margin-bottom:9px}
.source-card-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:1px;background:var(--line);border:1px solid var(--line);border-radius:8px;overflow:hidden}
.source-card-grid div{background:var(--panel);padding:8px}
.source-card-grid b{display:block;font-family:var(--mono);font-size:15px}
.source-card-grid span{display:block;font-size:10px;color:var(--mut);margin-top:2px}
.bet-source-gpt td{background:rgba(88,166,255,.035)}
.bet-source-claude td{background:rgba(188,140,255,.04)}
.bet-source-gpt:hover td,.bet-source-claude:hover td{background:rgba(232,184,75,.07)}
.foot{margin-top:40px;padding-top:16px;border-top:1px solid var(--line);
  font-family:var(--mono);font-size:11px;color:var(--mut);letter-spacing:.5px}
.warn{color:var(--acc);border:1px solid rgba(232,184,75,.3);background:rgba(232,184,75,.06);
  padding:10px 14px;border-radius:8px;font-size:12px;margin-bottom:26px}
.toolbar{display:flex;align-items:center;gap:10px;flex-wrap:wrap;background:var(--panel);
  border:1px solid var(--line);border-radius:10px;padding:12px 14px;margin:0 0 22px}
.toolbar label{font-size:12px;color:var(--mut);letter-spacing:.5px}
.toolbar input,.toolbar select{background:var(--bg);color:var(--txt);border:1px solid var(--line);
  border-radius:7px;padding:7px 9px;font-family:var(--mono)}
.toolbar button{background:var(--panel2);color:var(--txt);border:1px solid var(--line);
  border-radius:7px;padding:7px 10px;cursor:pointer}
.toolbar button:hover{border-color:var(--acc);color:var(--acc)}
.toolbar .filter-count{font-family:var(--mono);font-size:12px;color:var(--mut);margin-left:auto}
.hidden-by-date{display:none!important}
.schedule-box,.intel-box,.timeline-box,.quality-box{background:var(--panel);border:1px solid var(--line);border-radius:14px;padding:16px;margin-bottom:30px;overflow:auto}
.small{font-size:12px;margin-bottom:10px}
.section-title-mini{font-size:17px;font-weight:800;margin-bottom:6px;letter-spacing:.5px}
.match-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(245px,1fr));gap:14px}
.match-card{background:linear-gradient(145deg,#191f2a,#10131a);border:1px solid var(--line);border-radius:14px;padding:14px;box-shadow:0 14px 40px rgba(0,0,0,.18)}
.match-head{display:flex;gap:8px;align-items:center;flex-wrap:wrap;font-family:var(--mono);font-size:11px;color:var(--mut)}
.jc-code{color:#0a0c10;background:var(--acc);padding:3px 7px;border-radius:999px;font-weight:800}
.group-pill{border:1px solid var(--line);padding:2px 7px;border-radius:999px}
.versus{font-size:20px;font-weight:900;margin:12px 0 14px}
.market-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:1px;background:var(--line);border:1px solid var(--line);border-radius:10px;overflow:hidden}
.market-grid div{background:var(--panel2);padding:8px}
.market-grid b{display:block;color:var(--acc2);font-size:11px;letter-spacing:.5px}
.market-grid span{display:block;font-family:var(--mono);font-size:16px;margin:3px 0}
.market-grid small{display:block;color:var(--mut);font-family:var(--mono);font-size:10px;line-height:1.3}
.status-ok{color:var(--pos)}.status-warn{color:var(--acc)}.status-off{color:var(--neg)}
.card-note{color:var(--mut);font-size:12px;margin-top:10px}
.team-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;margin-top:14px}
.team-card{background:linear-gradient(180deg,#171c26,#11151d);border:1px solid var(--line);border-radius:14px;padding:14px}
.team-top{display:flex;justify-content:space-between;gap:10px;align-items:center}
.team-top h3{margin:0;font-size:20px}
.quality{font-family:var(--mono);font-size:11px;border-radius:999px;padding:3px 8px;border:1px solid var(--line)}
.q-ok{color:var(--pos)}.q-mid{color:var(--acc)}.q-low{color:var(--neg)}
.team-updated{color:var(--mut);font-size:12px;margin:6px 0 12px}
.intel-row{border-top:1px solid var(--line);padding:9px 0}
.intel-row b{color:var(--acc);font-size:12px}
.intel-row p{margin:4px 0 0;color:#cdd6e0;font-size:12.5px}
.data-gauge{display:flex;align-items:baseline;gap:12px;background:rgba(88,166,255,.08);border:1px solid rgba(88,166,255,.22);border-radius:12px;padding:10px 12px;margin:12px 0}
.data-gauge span{color:var(--mut);font-size:12px}.data-gauge b{font-family:var(--mono);font-size:24px;color:var(--acc2)}.data-gauge small{color:var(--mut)}
.timeline-item{border-left:3px solid var(--acc2);background:var(--panel2);border-radius:10px;margin:12px 0;padding:12px 14px}
.timeline-item h3{margin:0 0 8px;font-size:16px}
.timeline-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:8px}
.timeline-grid div{background:#0e1118;border:1px solid var(--line);border-radius:8px;padding:8px}
.timeline-grid b{display:block;color:var(--acc);font-size:12px}.timeline-grid span{font-size:12px;color:#cdd6e0}
.quality-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:12px}
.quality-tile{background:var(--panel2);border:1px solid var(--line);border-radius:12px;padding:13px}
.quality-tile span{display:block;color:var(--mut);font-size:12px}.quality-tile b{display:block;font-family:var(--mono);font-size:18px;margin-top:4px}
.odds-src{font-size:11px;color:var(--mut);margin:8px 0 2px;padding-left:2px;line-height:1.5}
.odds-src b{color:var(--acc2)}
.mkt-banner{font-size:12px;color:var(--acc);background:rgba(232,184,75,.08);border:1px solid rgba(232,184,75,.3);border-radius:8px;padding:7px 11px;margin:10px 0}
.mkt-na{font-size:12px;color:var(--mut);background:var(--panel2);border:1px dashed var(--line);border-radius:8px;padding:9px 12px;margin:8px 0}
.mkt-wrap{margin:10px 0}
.mkt-h{font-size:12px;letter-spacing:1px;color:var(--acc);margin:14px 0 6px;font-weight:700}
.score-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}
.score-col b{display:block;font-size:11px;color:var(--acc2);letter-spacing:.5px;margin-bottom:5px;border-bottom:1px solid var(--line);padding-bottom:4px}
.score-col .row{display:flex;justify-content:space-between;font-family:var(--mono);font-size:12px;padding:2px 0;color:#cdd6e0}
.score-col .row em{color:var(--mut);font-style:normal}
.ttg-row{display:grid;grid-template-columns:repeat(8,1fr);gap:1px;background:var(--line);border:1px solid var(--line);border-radius:8px;overflow:hidden}
.ttg-row div{background:var(--panel2);padding:7px 4px;text-align:center}
.ttg-row b{display:block;font-size:11px;color:var(--acc2)}.ttg-row span{display:block;font-family:var(--mono);font-size:13px;margin-top:3px}
.hafu-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:1px;background:var(--line);border:1px solid var(--line);border-radius:8px;overflow:hidden}
.hafu-grid div{background:var(--panel2);padding:7px 6px;text-align:center}
.hafu-grid b{display:block;font-size:10px;color:var(--mut)}.hafu-grid span{display:block;font-family:var(--mono);font-size:13px;margin-top:2px;color:#cdd6e0}
.ptable th,.ptable td{font-size:11.5px}
.intel-tab{margin:4px 0;font-size:11.5px}
.intel-tab th{padding:5px 6px}.intel-tab td{padding:5px 6px}
.intel-list{margin:4px 0 0;padding-left:16px}
.intel-list li{font-size:12.5px;color:#cdd6e0;margin:2px 0}
.tl-team{font-size:13px;color:var(--acc);font-weight:700;margin:12px 0 4px}
.tl-sub{font-size:12px;color:var(--acc2);margin:8px 0 6px}
.odds-flag{font-family:var(--mono);font-size:10px;padding:2px 7px;border-radius:999px;margin-left:auto}
.odds-flag.has-odds{color:var(--pos);border:1px solid rgba(63,185,80,.4);background:rgba(63,185,80,.1)}
.odds-flag.no-odds{color:var(--mut);border:1px solid var(--line)}
.mkt-tabs{margin-top:12px}
.tab-btns{display:flex;gap:4px;flex-wrap:wrap;border-bottom:1px solid var(--line);margin-bottom:10px}
.tab-btn{background:transparent;color:var(--mut);border:1px solid transparent;border-bottom:none;
  border-radius:8px 8px 0 0;padding:6px 11px;font-size:12px;font-family:var(--cjk);cursor:pointer;letter-spacing:.5px}
.tab-btn:hover{color:var(--txt)}
.tab-btn.active{color:var(--acc);background:var(--panel2);border-color:var(--line);font-weight:700}
.tab-panel{display:none;animation:rise .25s both}
.tab-panel.active{display:block}
.mkt-official{display:flex;align-items:baseline;gap:8px;flex-wrap:wrap;background:var(--panel2);
  border:1px solid var(--line);border-radius:8px;padding:8px 11px;margin-bottom:8px;font-size:12px}
.mkt-official span{color:var(--mut)}
.mkt-official b{font-family:var(--mono);font-size:14px}
.mkt-official .sp{font-family:var(--mono);color:#cdd6e0}
.mkt-scroll{max-height:300px;overflow:auto;border:1px solid var(--line);border-radius:8px;margin-top:6px}
.mkt-scroll table{margin:0}.mkt-scroll th{position:sticky;top:0;background:var(--panel);z-index:1}
/* 宽表横向滚动容器：手机上表格在自己的盒子里滑动，不撑破整页 */
.tscroll{overflow-x:auto;-webkit-overflow-scrolling:touch}
.tscroll::-webkit-scrollbar{height:6px}
.tscroll::-webkit-scrollbar-thumb{background:var(--line);border-radius:6px}
@media(max-width:680px){
  body{padding:18px 12px 60px}
  header{gap:8px}
  header h1{font-size:20px}
  .statbar{grid-template-columns:repeat(3,1fr)}
  .stat{padding:12px 10px}
  .stat-val{font-size:19px}
  .dims,.market-grid,.score-grid{grid-template-columns:repeat(2,1fr)}
  .ttg-row{grid-template-columns:repeat(4,1fr)}
  .match-grid{grid-template-columns:1fr}
  .versus{font-size:18px;word-break:break-word}
  .schedule-box,.intel-box,.timeline-box,.quality-box{padding:12px}
  .intel-row{overflow-x:auto}
  table{font-size:11px}
  .tscroll table{min-width:560px}
  .tscroll-bets table{min-width:760px}
  .toolbar{padding:10px 12px}
  .toolbar select{flex:1 1 auto;min-width:0}
  .toolbar .filter-count{margin-left:0;flex-basis:100%}
  .tab-btn{padding:8px 13px}
  summary{padding:11px 0}
  .data-gauge{flex-wrap:wrap;gap:6px}
}
@media(max-width:430px){
  body{padding:14px 9px 52px}
  header h1{font-size:18px}
  .statbar{grid-template-columns:repeat(2,1fr)}
  .stat-val{font-size:18px}
  .versus{font-size:17px}
  .section-title-mini{font-size:15px}
  .team-top h3{font-size:18px}
  .hafu-grid b{font-size:9px}
}
</style></head>
<body>
<header>
  <div><h1>世界杯竞彩 <span class="dot">·</span> 下注分析看板</h1>
  <div class="sub">EDITION __EDITION__ — __NA__ 场分析 / __NB__ 张票</div></div>
  <div class="sub">仅供本人观看 · 非投注建议</div>
</header>
<div class="warn">竞彩仅小额娱乐，不保证盈利；连续失误应减额而非追加（§8 风险边界）。本页数据由脚本生成，赔率为采集时点估算，出票以票面 SP 为准。</div>
<div class="toolbar">
  <label for="dateFilter">按比赛日筛选</label>
  <select id="dateFilter" aria-label="按比赛日筛选">__DATE_OPTIONS__</select>
  <button id="clearDateFilter" type="button">显示全部</button>
  <span id="dateFilterCount" class="filter-count">默认展示最近未完赛赛程</span>
</div>
<div class="statbar">__STATS__</div>
<div data-block><div class="section-h">赛程与官方核对</div>
__SCHEDULE__</div>
<div data-block><div class="section-h">球队情报（按比赛日）</div>
__TEAM_INTEL__</div>
<div data-block><div class="section-h">逐场表现</div>
__PER_MATCH_TIMELINE__</div>
<div><div class="section-h">数据完整度</div>
__DATA_QUALITY__</div>
<div data-block><div class="section-h">每场分析留档</div>
<div class="cards">__CARDS__</div></div>
<div data-block><div class="section-h">下注记录</div>
__BETS__</div>
<div class="foot">由 build_dashboard.py 生成 · 展示赛程、球队档案、球员状态、逐场表现、分析留档、下注记录与数据完整度</div>
<script>
const SCHEDULE_TIMING = __TIMING_JSON__;
const FINISH_BUFFER_MS = 2.5*3600*1000; // 末场开球后 2.5h 视为完赛
// 玩法切换：点 tab 按钮，在所在卡片内切换面板，互不干扰
document.addEventListener('click', e => {
  const btn = e.target.closest('.tab-btn');
  if(!btn) return;
  const box = btn.closest('[data-tabs]');
  const idx = btn.dataset.tab;
  box.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b === btn));
  box.querySelectorAll('.tab-panel').forEach(p => p.classList.toggle('active', p.dataset.panel === idx));
});
// 进入页面按当前时刻挑默认比赛日：最近一个还没踢完的比赛日（末场开球+缓冲仍在未来）
function pickDefaultMatchday(){
  if(!Array.isArray(SCHEDULE_TIMING) || !SCHEDULE_TIMING.length) return null;
  const now = Date.now();
  const upcoming = SCHEDULE_TIMING.filter(d =>
    new Date(d.lastKickoffUTC).getTime() + FINISH_BUFFER_MS > now);
  const chosen = upcoming.length ? upcoming[0] : SCHEDULE_TIMING[SCHEDULE_TIMING.length-1];
  return chosen && chosen.matchday;
}
function applyDateFilter(){
  const value = document.getElementById('dateFilter').value;
  const items = Array.from(document.querySelectorAll('[data-matchday]'));
  let shown = 0;
  items.forEach(el => {
    const days = (el.dataset.matchday || '').split(' ').filter(Boolean);
    const ok = !value || days.includes(value);
    el.classList.toggle('hidden-by-date', !ok);
    if (ok) shown += 1;
  });
  // 筛某一天时，隐藏当天没有任何条目的分区（连标题一起），避免空标题
  document.querySelectorAll('[data-block]').forEach(block => {
    const owned = block.querySelectorAll('[data-matchday]');
    if (!value || owned.length === 0){ block.classList.remove('hidden-by-date'); return; }
    const anyVisible = Array.from(owned).some(el => !el.classList.contains('hidden-by-date'));
    block.classList.toggle('hidden-by-date', !anyVisible);
  });
  document.getElementById('dateFilterCount').textContent =
    value ? `比赛日 ${value}：显示 ${shown} 项` : `显示全部日期（${shown} 项）`;
}
document.getElementById('dateFilter').addEventListener('change', applyDateFilter);
document.getElementById('clearDateFilter').addEventListener('click', () => {
  document.getElementById('dateFilter').value = '';
  applyDateFilter();
});
// 按观看时刻重选默认比赛日（覆盖生成时烤进的默认），静态页面也能随时间自动翻页
(function(){
  const sel = document.getElementById('dateFilter');
  const def = pickDefaultMatchday();
  if(def && Array.from(sel.options).some(o => o.value === def)) sel.value = def;
  applyDateFilter();
})();
</script>
</body></html>"""


if __name__ == "__main__":
    main()
