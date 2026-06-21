# -*- coding: utf-8 -*-
"""从 The Odds API 拉取世界杯赔率，落档到 archive/odds/ 并可直接喂给 odds_table.py。

Key 读取顺序：环境变量 ODDS_API_KEY > --key 参数。
注意：setx 设的环境变量只对之后新开的会话生效；当前会话没有就用 --key。
免费档 500 credits/月，credits = len(markets) × len(regions) / 请求。
默认 h2h,spreads,totals × (eu,uk) = 3×2 = 6 credits/次（胜平负+让球+大小球）。
省额度可 --markets h2h 只拉胜平负=2 credits/次。
玩法来源边界（2026-06 实测 soccer_fifa_world_cup，已对单场 endpoint 校验）：
- The Odds API 有：h2h(胜平负)、spreads(让球/亚盘)、totals/alternate_totals(大小球)、
  h2h_3_way_h1/h2(上/下半场胜平负)、btts(双方进球)、draw_no_bet；后四类是附加玩法，
  须走单场 endpoint /events/{id}/odds，本脚本默认只批量拉前三类（h2h,spreads,totals）。
- The Odds API 没有：correct_score(比分)、total_goals(竞彩总进球区间 0-7+)、
  half_time_full_time(竞彩半全场 HT/FT 九选一)——请求这些 key 直接 422 INVALID_MARKET。
所以「比分 / 竞彩总进球区间 / 半全场」三类走 cn_markets（500/中国竞彩官方票面），
用 set_cn_markets.py 灌入，fetch 时会自动保留不被覆盖。

赔率变化（初赔→即时）：先用 --snapshot open 拉一次（出当日早盘），临场前再用 --snapshot live
拉一次写同一场文件的 live 字段；odds_table.py 即可输出 open→live 变化。
时间口径：The Odds API 返回 UTC 开赛时间；落档时 date/matchday 用北京时间日期，同时保留 date_utc/commence_utc。

用法（建议在项目根运行）：
  python skill/scripts/fetch_odds_api.py                # 拉全部世界杯场次，写匹配到的
  python skill/scripts/fetch_odds_api.py --teams France,Iraq,Argentina,Austria
  python skill/scripts/fetch_odds_api.py --snapshot open    # 早盘初赔
  python skill/scripts/fetch_odds_api.py --key <KEY> --markets h2h,spreads
"""
import argparse
import json
import os
import urllib.request
from datetime import datetime, timezone, timedelta
from statistics import median

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ODDS_DIR = os.path.join(BASE, "archive", "odds")
SPORT = "soccer_fifa_world_cup"

# 表格展示用的大公司（Pinnacle 作凯利基准）；其余全部存进 all_books
BIG_BOOKS = ["Pinnacle", "William Hill", "Betfair", "Paddy Power",
             "888sport", "Bet Victor", "Marathon Bet", "Betway"]


def beijing_time_fields(commence_utc):
    """把 API 的 UTC 开赛时间转成竞彩项目统一使用的北京时间字段。"""
    dt = datetime.fromisoformat(commence_utc.replace("Z", "+00:00"))
    bj = dt.astimezone(timezone(timedelta(hours=8)))
    return {
        "date_utc": dt.strftime("%Y-%m-%d"),
        "date_beijing": bj.strftime("%Y-%m-%d"),
        "commence_beijing": bj.strftime("%Y-%m-%d %H:%M"),
        "timezone": "Asia/Shanghai",
    }


def slug(s):
    return s.lower().replace(" ", "-").replace(".", "")


def h2h_triple(bk, home, away):
    """从一个 bookmaker 的 h2h market 取 home/draw/away 十进制赔率。"""
    for m in bk.get("markets", []):
        if m["key"] != "h2h":
            continue
        d = {o["name"]: o["price"] for o in m["outcomes"]}
        return {"home": d.get(home), "draw": d.get("Draw"), "away": d.get(away)}
    return {"home": None, "draw": None, "away": None}


def _has_complete_h2h(triple):
    return all(isinstance(triple.get(k), (int, float)) and triple[k] > 1 for k in ("home", "draw", "away"))


def filter_h2h_books(books, home, away, max_ratio=2.5, min_ratio=0.4):
    """剔除明显错配的 h2h 源。

    The Odds API 偶尔会把单个 bookmaker 的足球 h2h 返回成明显不合理的源数据
    （例如热门主胜约 1.12 时，该公司给 Draw=1.33、Home=3.42）。
    这不是本地解析问题；为避免污染市场均值/凯利，仅当同场至少 3 家完整 h2h
    可形成市场中位数时，按各项赔率相对中位数的倍数剔除极端错配源。
    """
    triples = []
    for bk in books:
        triple = h2h_triple(bk, home, away)
        if _has_complete_h2h(triple):
            triples.append((bk, triple))
    if len(triples) < 3:
        return list(books), []

    med = {k: median([t[k] for _, t in triples]) for k in ("home", "draw", "away")}
    excluded_ids = set()
    for bk, triple in triples:
        for k in ("home", "draw", "away"):
            if med[k] <= 0:
                continue
            ratio = triple[k] / med[k]
            if ratio > max_ratio or ratio < min_ratio:
                excluded_ids.add(id(bk))
                break
    kept, excluded = [], []
    for bk in books:
        (excluded if id(bk) in excluded_ids else kept).append(bk)
    return kept, excluded


def spread_pair(bk, home, away):
    """让球/亚盘（spreads）：取主、客的盘口点数与赔率。看板的 markets.让球 用。"""
    for m in bk.get("markets", []):
        if m["key"] != "spreads":
            continue
        d = {o["name"]: o for o in m["outcomes"]}
        h, a = d.get(home), d.get(away)
        if not (h or a):
            return None
        return {"point": (h or {}).get("point"),
                "home": (h or {}).get("price"), "away": (a or {}).get("price")}
    return None


def totals_pair(bk):
    """大小球（totals）：取大/小的线与赔率。注意≠竞彩总进球区间，仅作参考。"""
    for m in bk.get("markets", []):
        if m["key"] != "totals":
            continue
        d = {o["name"]: o for o in m["outcomes"]}
        ov, un = d.get("Over"), d.get("Under")
        if not (ov or un):
            return None
        return {"point": (ov or un or {}).get("point"),
                "over": (ov or {}).get("price"), "under": (un or {}).get("price")}
    return None


def build_markets(books, home, away):
    """把大公司的 spreads/totals 汇成 markets 块（看板直接渲染）。"""
    spreads, totals = [], []
    for name in BIG_BOOKS:
        bk = next((b for b in books if b["title"] == name), None)
        if not bk:
            continue
        sp = spread_pair(bk, home, away)
        if sp and sp.get("home"):
            spreads.append({"name": name, **sp})
        to = totals_pair(bk)
        if to and (to.get("over") or to.get("under")):
            totals.append({"name": name, **to})
    markets = {}
    if spreads:
        markets["让球(spreads)"] = {"source": "The Odds API", "companies": spreads}
    if totals:
        markets["大小球(totals)"] = {"source": "The Odds API", "companies": totals}
    return markets


def market_avg(books, home, away):
    """按公司名去重后取均值（The Odds API 会重复返回 Betfair 交易所/Sportsbook，避免双计）。
    返回 (均值dict, 计入家数)。"""
    books, _ = filter_h2h_books(books, home, away)
    acc = {"home": [], "draw": [], "away": []}
    seen = set()
    for bk in books:
        title = bk.get("title")
        if title in seen:
            continue
        seen.add(title)
        t = h2h_triple(bk, home, away)
        for k in acc:
            if t[k]:
                acc[k].append(t[k])
    avg = {k: round(sum(v) / len(v), 3) if v else None for k, v in acc.items()}
    return avg, len(seen)


def preserve_manual_fields(obj, prev):
    """再次拉取 live 时保留人工/镜像补齐的竞彩字段。

    fetch_odds_api.py 自动生成海外赔率；jc_code、official、cn_markets、
    snapshot_policy 可能由后续中国竞彩/500镜像补齐，不能在下次拉取时重置。
    """
    if not isinstance(prev, dict):
        return obj
    for key in ("jc_code", "official", "cn_markets", "snapshot_policy"):
        val = prev.get(key)
        if val not in (None, "", {}):
            obj[key] = val
    return obj


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--key", default=None)
    p.add_argument("--regions", default="eu,uk")
    p.add_argument("--markets", default="h2h,spreads,totals",
                   help="默认 h2h,spreads,totals=胜平负+让球+大小球；3×2=6 credits/次")
    p.add_argument("--snapshot", default="live", choices=["open", "live"],
                   help="open=早盘初赔，live=临场即时；两次拉取写同一文件以出赔率变化")
    p.add_argument("--teams", default=None, help="逗号分隔，只写含这些队的场次")
    a = p.parse_args()

    key = os.environ.get("ODDS_API_KEY") or a.key
    if not key:
        raise SystemExit("缺少 key：设置环境变量 ODDS_API_KEY 或用 --key 传入")

    url = (f"https://api.the-odds-api.com/v4/sports/{SPORT}/odds/"
           f"?apiKey={key}&regions={a.regions}&markets={a.markets}"
           f"&oddsFormat=decimal&dateFormat=iso")
    r = urllib.request.urlopen(url, timeout=45)
    remaining = r.headers.get("x-requests-remaining")
    data = json.load(r)
    print(f"拉取成功：{len(data)} 场，剩余额度 {remaining}")

    teams = [t.strip() for t in a.teams.split(",")] if a.teams else None
    written = []
    for g in data:
        home, away = g["home_team"], g["away_team"]
        if teams and not (any(t in home for t in teams) or any(t in away for t in teams)):
            continue
        books = g.get("bookmakers", [])
        h2h_books, excluded_h2h_books = filter_h2h_books(books, home, away)
        snap = a.snapshot
        time_fields = beijing_time_fields(g["commence_time"])
        companies = []
        for name in BIG_BOOKS:
            bk = next((b for b in h2h_books if b["title"] == name), None)
            if not bk:
                continue
            companies.append({
                "name": name, "reference": name == "Pinnacle", "snapshot": snap,
                "time": bk.get("last_update", ""), "url": "the-odds-api",
                "tier": "公司直站", "eu": h2h_triple(bk, home, away),
            })
        mavg, navg = market_avg(books, home, away)
        companies.append({
            "name": "市场均值", "reference": not any(c["reference"] for c in companies),
            "snapshot": snap, "time": g["commence_time"],
            "url": f"the-odds-api({navg}家均值,已去重)", "tier": "聚合", "eu": mavg,
        })
        markets = build_markets(books, home, away)
        prev_doc = None
        fn = os.path.join(ODDS_DIR, f"{slug(home)}-vs-{slug(away)}-{g['commence_time'][:10].replace('-','')}.json")
        # 合并：若已存在该场文件，保留其它快照(open/live)的 companies、人工灌入的 cn_markets，避免覆盖
        if os.path.exists(fn):
            try:
                with open(fn, "r", encoding="utf-8") as f:
                    prev = json.load(f)
                prev_doc = prev
                kept = [c for c in prev.get("companies", []) if c.get("snapshot") != snap]
                companies = kept + companies
                if not markets:  # 本次没拉到 spreads/totals 时保留上次的
                    markets = prev.get("markets") or {}
            except Exception:
                pass
        obj = {
            "match": f"{home} vs {away}", "date": time_fields["date_beijing"],
            **time_fields,
            "commence_utc": g["commence_time"], "jc_code": "待竞彩官方核对",
            "source": "The Odds API", "books_count": len(books),
            "official": {"source": "体彩(弱化，仅出票/结算取SP)",
                         "HAD_open": None, "HAD": {"home": None, "draw": None, "away": None},
                         "HHAD": {"handicap": None, "home": None, "draw": None, "away": None}},
            "companies": companies,
            "markets": markets,
            "h2h_books_count": len(h2h_books),
            "excluded_h2h_books": [
                {"name": b["title"], "eu": h2h_triple(b, home, away),
                 "time": b.get("last_update", ""), "reason": "h2h_outlier_vs_market_median"}
                for b in excluded_h2h_books
            ],
            "all_books": [{"name": b["title"], "eu": h2h_triple(b, home, away),
                           "time": b.get("last_update", "")} for b in h2h_books],
            "note": "Pinnacle 作凯利基准；体彩仅出票/结算取SP。The Odds API 不含 bet365；其有胜平负/让球/大小球/上下半场胜平负(h2h_3_way_h1/h2,附加玩法走单场endpoint)/双方进球，但无比分(correct_score)、竞彩总进球区间、半全场(HT/FT)——这三类见 cn_markets，来自500/中国竞彩官方票面。",
        }
        preserve_manual_fields(obj, prev_doc)
        with open(fn, "w", encoding="utf-8") as f:
            json.dump(obj, f, ensure_ascii=False, indent=2)
        written.append(os.path.basename(fn))
        print(f" 写入: {os.path.basename(fn)} ({len(books)}家, snapshot={snap})")

    print(f"\n完成 {len(written)} 场。可跑: python skill/scripts/odds_table.py <文件名>")


if __name__ == "__main__":
    main()
