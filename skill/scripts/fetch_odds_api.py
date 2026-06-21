# -*- coding: utf-8 -*-
"""从 The Odds API 拉取世界杯赔率，落档到 archive/odds/ 并可直接喂给 odds_table.py。

Key 读取顺序：环境变量 ODDS_API_KEY > --key 参数。
注意：setx 设的环境变量只对之后新开的会话生效；当前会话没有就用 --key。
免费档 500 credits/月，credits = len(markets) × len(regions) / 请求。
默认 h2h × (eu,uk) = 1×2 = 2 credits/次；加 spreads 后 2×2 = 4 credits/次。

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


def market_avg(books, home, away):
    """按公司名去重后取均值（The Odds API 会重复返回 Betfair 交易所/Sportsbook，避免双计）。
    返回 (均值dict, 计入家数)。"""
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


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--key", default=None)
    p.add_argument("--regions", default="eu,uk")
    p.add_argument("--markets", default="h2h")
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
        snap = a.snapshot
        time_fields = beijing_time_fields(g["commence_time"])
        companies = []
        for name in BIG_BOOKS:
            bk = next((b for b in books if b["title"] == name), None)
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
        fn = os.path.join(ODDS_DIR, f"{slug(home)}-vs-{slug(away)}-{g['commence_time'][:10].replace('-','')}.json")
        # 合并：若已存在该场文件，保留其它快照(open/live)的 companies，避免覆盖
        if os.path.exists(fn):
            try:
                with open(fn, "r", encoding="utf-8") as f:
                    prev = json.load(f)
                kept = [c for c in prev.get("companies", []) if c.get("snapshot") != snap]
                companies = kept + companies
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
            "all_books": [{"name": b["title"], "eu": h2h_triple(b, home, away),
                           "time": b.get("last_update", "")} for b in books],
            "note": "Pinnacle 作凯利基准；体彩仅出票/结算取SP。The Odds API 不含 bet365。",
        }
        with open(fn, "w", encoding="utf-8") as f:
            json.dump(obj, f, ensure_ascii=False, indent=2)
        written.append(os.path.basename(fn))
        print(f" 写入: {os.path.basename(fn)} ({len(books)}家, snapshot={snap})")

    print(f"\n完成 {len(written)} 场。可跑: python skill/scripts/odds_table.py <文件名>")


if __name__ == "__main__":
    main()
