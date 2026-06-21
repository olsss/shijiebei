# -*- coding: utf-8 -*-
"""生成自包含的下注分析看板 archive/dashboard/index.html（数据烤进HTML，双击即看）。

读取：archive/bets.json、archive/analysis/*.json、archive/odds/*.json
风格：深色"竞彩交易台"，盈亏红绿语义，原生 <details> 折叠，离线无依赖。

用法：python skill/scripts/build_dashboard.py
"""
import glob
import html
import json
import os

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
        '<article class="card">'
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
        rows.append(
            f'<tr><td>{esc(b.get("比赛"))}</td><td>{esc(b.get("类型"))}</td>'
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

    body = HTML.replace("__STATS__", stats) \
               .replace("__CARDS__", cards) \
               .replace("__BETS__", bets_table(bets)) \
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
</style></head>
<body>
<header>
  <div><h1>世界杯竞彩 <span class="dot">·</span> 下注分析看板</h1>
  <div class="sub">EDITION __EDITION__ — __NA__ 场分析 / __NB__ 张票</div></div>
  <div class="sub">仅供本人观看 · 非投注建议</div>
</header>
<div class="warn">竞彩仅小额娱乐，不保证盈利；连续失误应减额而非追加（§8 风险边界）。本页数据由脚本生成，赔率为采集时点估算，出票以票面 SP 为准。</div>
<div class="statbar">__STATS__</div>
<div class="section-h">每场分析留档</div>
<div class="cards">__CARDS__</div>
<div class="section-h">下注记录</div>
__BETS__
<div class="foot">由 build_dashboard.py 生成 · 数据源 bets.json + analysis/ + odds/(The Odds API)</div>
</body></html>"""


if __name__ == "__main__":
    main()
