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
from datetime import datetime

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
    """默认展示最近的未完赛比赛日；无未完赛时回退到最近可用日期。"""
    today = today or datetime.now().strftime("%Y-%m-%d")
    days = matchday_statuses(analyses, bets, extra_days)
    unfinished = sorted(day for day, pending in days.items() if pending)
    future_unfinished = [day for day in unfinished if day >= today]
    if future_unfinished:
        return future_unfinished[0]
    if unfinished:
        return unfinished[0]
    future_any = sorted(day for day in days if day >= today)
    if future_any:
        return future_any[0]
    all_days = sorted(days)
    return all_days[0] if all_days else ""


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


def schedule_html():
    matches = schedule_matches()
    if not matches:
        return '<div class="muted">暂无赛程文件。</div>'
    cards = []
    for m in matches:
        status_cls = "status-ok" if m["had_open"] == "开售" else ("status-warn" if "待" in m["had_open"] else "status-off")
        cards.append(
            f'<article class="match-card" data-matchday="{esc(m["matchday"])}">'
            f'<div class="match-head"><span class="jc-code">{esc(m["code"])}</span>'
            f'<span class="match-time">{esc(m["matchday"])} {esc(m["time"])}</span>'
            f'<span class="group-pill">{esc(m["group"] or "—")}组</span></div>'
            f'<div class="versus">{esc(m["match"])}</div>'
            f'<div class="market-grid">'
            f'<div><b>HAD</b><span class="{status_cls}">{esc(m["had_open"] or "—")}</span><small>{esc(m["had_sp"] or "—")}</small></div>'
            f'<div><b>HHAD</b><span>{esc(m["handicap"] or "—")}</span><small>{esc(m["hhad_sp"] or "—")}</small></div>'
            f'<div><b>编号日</b><span>{esc(m["code_date"] or "—")}</span><small>竞彩批次</small></div>'
            f'</div><div class="card-note">{esc(m["note"])}</div></article>'
        )
    return (
        '<section class="schedule-box"><div class="section-title-mini">赛程作战台</div>'
        '<div class="muted small">按 skill/data/group-schedule.md 解析成业务卡片；出票仍以中国竞彩官方页面/票面为准。</div>'
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
    cards = []
    for t in profiles:
        c = t["completeness"]
        cls = "q-ok" if c >= 80 else ("q-mid" if c >= 50 else "q-low")
        cards.append(
            f'<article class="team-card"><div class="team-top"><h3>{esc(t["name"])}</h3>'
            f'<span class="quality {cls}">数据完整度 {c}%</span></div>'
            f'<div class="team-updated">最后更新：{esc(t["updated"])}</div>'
            f'<div class="intel-row"><b>基本档案</b><p>{esc(compact_text(t["basic"]))}</p></div>'
            f'<div class="intel-row"><b>人员配置</b><p>{esc(compact_text(t["personnel"]))}</p></div>'
            f'<div class="intel-row"><b>球员状态</b><p>{esc(compact_text(t["players"]))}</p></div>'
            f'<div class="intel-row"><b>战意/排名</b><p>{esc(compact_text(t["motivation"]))}</p></div>'
            f'<div class="intel-row"><b>大赛气质</b><p>{esc(compact_text(t["temperament"]))}</p></div>'
            f'</article>'
        )
    avg = round(sum(t["completeness"] for t in profiles) / len(profiles))
    return (
        '<section class="intel-box"><div class="section-title-mini">球队情报矩阵</div>'
        f'<div class="data-gauge"><span>数据完整度</span><b>{avg}%</b><small>按基本档案/人员/球员状态/战意/气质估算</small></div>'
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
        bullets = re.findall(r"^- \*\*(.+?)\*\*[:：]\s*(.*)", text, re.M)
        body = "".join(f'<div><b>{esc(k)}</b><span>{esc(v or "待补")}</span></div>' for k, v in bullets[:8])
        items.append(f'<article class="timeline-item"><h3>{esc(title)}</h3><div class="timeline-grid">{body}</div></article>')
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
        ("来源快照", f"{complete_sources}/{len(sources)} complete", "q-ok" if complete_sources == len(sources) and sources else "q-mid"),
        ("赔率快照", f"{live_only} 场仅 live", "q-low" if live_only else "q-ok"),
        ("分析留档", f"{draft_analyses} draft", "q-mid" if draft_analyses else "q-ok"),
        ("球队档案", f"{placeholder_teams} 队占位偏多", "q-low" if placeholder_teams else "q-ok"),
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


def prob_bar(imp):
    if not imp or any(imp.get(k) is None for k in ("home", "draw", "away")):
        return '<div class="muted">无基准隐含概率</div>'
    h, d, a = imp["home"], imp["draw"], imp["away"]
    return (
        '<div class="probbar">'
        f'<div class="seg seg-h" style="width:{h*100:.1f}%"><span>{h*100:.0f}%</span></div>'
        f'<div class="seg seg-d" style="width:{d*100:.1f}%"><span>{d*100:.0f}%</span></div>'
        f'<div class="seg seg-a" style="width:{a*100:.1f}%"><span>{a*100:.0f}%</span></div>'
        '</div>'
        '<div class="problab"><span>主胜</span><span>平</span><span>客胜</span></div>'
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
    return (f'<details class="odds"><summary>多公司赔率对比 · 共 {n} 家（Pinnacle 为基准）</summary>'
            f'<table class="otab"><thead>{head}</thead><tbody>{"".join(rows)}</tbody></table>'
            '<div class="hint">凯利&gt;1（高亮）多为 Betfair 交易所结构性高赔，非可吃价值；以返还率高的硬庄口为准。</div>'
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
    refline = ""
    if ref.get("home"):
        refline = (f'<div class="refodds">{esc(ref.get("book","Pinnacle"))} '
                   f'<b>{fmt(ref.get("home"))}</b> / {fmt(ref.get("draw"))} / {fmt(ref.get("away"))}</div>')
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
        f'{refline}{prob_bar(ref.get("implied"))}{flags_html}'
        f'<div class="dims">{dim_html}</div>'
        f'{risks_html}{rec_html}'
        f'<details class="narr"><summary>展开综合分析</summary><div class="narr-body">{md(an.get("narrative_md"))}</div></details>'
        f'{odds_table_html(odds)}'
        '</article>'
    )


def bets_table(bets):
    rows = []
    for b in bets:
        pnl = b.get("盈亏")
        pcls = "" if pnl is None else ("pos" if pnl > 0 else ("neg" if pnl < 0 else ""))
        hit = b.get("命中")
        hit_s = "待开奖" if hit is None else ("命中" if hit else "未中")
        hit_c = "" if hit is None else ("pos" if hit else "neg")
        clv = b.get("CLV")
        clv_cls = "" if clv is None else ("pos" if clv > 0 else ("neg" if clv < 0 else ""))
        matchday = esc(b.get("比赛日"))
        rows.append(
            f'<tr data-matchday="{matchday}"><td>{esc(b.get("比赛"))}</td><td>{esc(b.get("类型"))}</td>'
            f'<td>{esc(b.get("下注内容"))}</td><td class="num">{esc(b.get("投入"))}</td>'
            f'<td class="num">{esc(b.get("赔率") if b.get("赔率") is not None else "—")}</td>'
            f'<td class="num">{esc(b.get("收盘赔率") if b.get("收盘赔率") is not None else "—")}</td>'
            f'<td class="num {clv_cls}">{esc(clv_fmt(clv))}</td>'
            f'<td>{esc(b.get("结算口径") or "90分钟")}</td>'
            f'<td>{esc(b.get("赛果"))}</td><td class="{hit_c}">{hit_s}</td>'
            f'<td class="num">{esc(b.get("返还") if b.get("返还") is not None else "—")}</td>'
            f'<td class="num {pcls}">{esc(pnl) if pnl is not None else "—"}</td></tr>'
        )
    head = ('<tr><th>比赛</th><th>类型</th><th>下注内容</th><th>投入</th><th>赔率</th>'
            '<th>收盘赔率</th><th>CLV</th><th>结算口径</th>'
            '<th>赛果</th><th>命中</th><th>返还</th><th>盈亏</th></tr>')
    return f'<table class="btab"><thead>{head}</thead><tbody>{"".join(rows)}</tbody></table>'


# ---------- 主 ----------
def main():
    bets_doc = load_json(os.path.join(ARCH, "bets.json"))
    bets = bets_doc.get("bets", [])
    analyses = load_all("analysis")
    odds_list = load_all("odds")
    odds_by_file = {o["_file"]: o for o in odds_list}
    analyses.sort(key=lambda a: (a.get("matchday", ""), a.get("match", "")))

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

    cards = "".join(analysis_card(a, odds_by_file) for a in analyses)
    if not cards:
        cards = '<div class="muted" style="padding:40px">暂无分析留档。用 new_analysis.py 生成。</div>'
    schedule_days = schedule_matchdays()
    default_day = default_matchday(analyses, bets, extra_days=schedule_days)
    date_options = date_filter_options(analyses, bets, default_day, extra_days=schedule_days)

    body = HTML.replace("__STATS__", stats) \
               .replace("__SCHEDULE__", schedule_html()) \
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
<meta name="viewport" content="width=device-width,initial-scale=1">
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
<div class="section-h">赛程与官方核对</div>
__SCHEDULE__
<div class="section-h">球队情报</div>
__TEAM_INTEL__
<div class="section-h">逐场表现</div>
__PER_MATCH_TIMELINE__
<div class="section-h">数据完整度</div>
__DATA_QUALITY__
<div class="section-h">每场分析留档</div>
<div class="cards">__CARDS__</div>
<div class="section-h">下注记录</div>
__BETS__
<div class="foot">由 build_dashboard.py 生成 · 展示赛程、球队档案、球员状态、逐场表现、分析留档、下注记录与数据完整度</div>
<script>
function applyDateFilter(){
  const value = document.getElementById('dateFilter').value;
  const items = Array.from(document.querySelectorAll('[data-matchday]'));
  let shown = 0;
  items.forEach(el => {
    const ok = !value || el.dataset.matchday === value;
    el.classList.toggle('hidden-by-date', !ok);
    if (ok) shown += 1;
  });
  document.getElementById('dateFilterCount').textContent =
    value ? `比赛日 ${value}：显示 ${shown} 项` : '显示全部日期';
}
document.getElementById('dateFilter').addEventListener('change', applyDateFilter);
document.getElementById('clearDateFilter').addEventListener('click', () => {
  document.getElementById('dateFilter').value = '';
  applyDateFilter();
});
applyDateFilter();
</script>
</body></html>"""


if __name__ == "__main__":
    main()
