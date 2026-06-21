# -*- coding: utf-8 -*-
"""把比分 / 总进球 / 半全场（中国竞彩玩法）灌进 archive/odds/<场次>.json 的 cn_markets 块。

为什么单独一个脚本：The Odds API 不提供「比分」(correct_score) 和竞彩「半全场」(HT/FT
九选一) 玩法，「总进球」也只有大小球（over/under 线）而非竞彩的总进球区间（0/1/…/7+）；
实测请求这些 market key 直接 422 INVALID_MARKET。（注：上/下半场胜平负 h2h_3_way_h1/h2
是有的，但那是单独半场结果，不是竞彩半全场。）这三类玩法的赔率必须来自
中国竞彩官方计算器 / 500.com 比分页（§1 官方核对优先、§2C 多源核验）。本脚本把这些
数字按看板能直接渲染的 schema 写进对应赔率文件；fetch_odds_api.py 再次拉取胜平负时
会自动保留 cn_markets 不被覆盖。

获取数字的两条路：
  A. 浏览器抓：python skill/scripts/scrape_browser.py "<500比分页URL>" --shot bf.png
     （oddschecker/oddspedia 有 Cloudflare 不可靠；500/中国竞彩官方页相对可达）
     人工把比分/总进球/半全场赔率整理成下方 payload JSON。
  B. 直接抄中国竞彩官方计算器票面数字（最权威，出票/结算口径一致）。

用法：
  # 用文件名定位 + payload 文件
  python skill/scripts/set_cn_markets.py --file argentina-vs-austria-20260622.json --input cn.json
  # 用队名+UTC日期定位（与 fetch_odds_api.py 落档命名一致：slug-vs-slug-YYYYMMDD.json）
  python skill/scripts/set_cn_markets.py --home argentina --away austria --date 20260622 --input cn.json
  # 从 stdin 读 payload
  type cn.json | python skill/scripts/set_cn_markets.py --file argentina-vs-austria-20260622.json

payload(cn.json) 结构（缺的玩法整段省略即可；比分可分组也可平铺）：
{
  "source": "中国竞彩官方计算器",
  "snapshot_time": "2026-06-21 22:00",
  "open": {"HAD": false, "HHAD": true, "比分": true, "总进球": true, "半全场": true},
  "比分": {
    "主胜": {"1:0": 7.5, "2:0": 11, "2:1": 9, "3:0": 21},
    "平局": {"0:0": 9, "1:1": 6, "2:2": 13},
    "客胜": {"0:1": 8, "0:2": 17, "1:2": 12},
    "其他": {"胜其他": 21, "平其他": 18, "负其他": 26}
  },
  "总进球": {"0": 11, "1": 5.4, "2": 3.55, "3": 3.75, "4": 6.0, "5": 11, "6": 26, "7+": 41},
  "半全场": {"胜/胜": 1.95, "胜/平": 15, "胜/负": 41, "平/胜": 5.0, "平/平": 4.1,
            "平/负": 11, "负/胜": 67, "负/平": 21, "负/负": 7.0}
}
"""
import argparse
import json
import os
import sys

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ODDS_DIR = os.path.join(BASE, "archive", "odds")
MARKET_KEYS = ("比分", "总进球", "半全场")


def slug(s):
    return s.lower().replace(" ", "-").replace(".", "")


def locate(args):
    if args.file:
        return os.path.join(ODDS_DIR, args.file)
    if args.home and args.away and args.date:
        fn = f"{slug(args.home)}-vs-{slug(args.away)}-{args.date.replace('-','')}.json"
        return os.path.join(ODDS_DIR, fn)
    raise SystemExit("需用 --file，或同时给 --home --away --date 定位赔率文件")


def load_payload(args):
    if args.input:
        with open(args.input, "r", encoding="utf-8") as f:
            return json.load(f)
    data = sys.stdin.read().strip()
    if not data:
        raise SystemExit("未提供 payload：用 --input <file> 或从 stdin 传 JSON")
    return json.loads(data)


def validate(payload):
    if not isinstance(payload, dict):
        raise SystemExit("payload 必须是 JSON 对象")
    has_any = any(k in payload for k in MARKET_KEYS) or "open" in payload
    if not has_any:
        raise SystemExit(f"payload 至少要含 {MARKET_KEYS} 之一，或 open 字段")
    # 数值健壮性：赔率应为正数，否则提示（不强制中断，但标记）
    warns = []

    def check_odds(label, d):
        for k, v in d.items():
            try:
                if float(v) <= 1.0:
                    warns.append(f"{label} {k}={v} 赔率≤1，疑似异常")
            except (TypeError, ValueError):
                warns.append(f"{label} {k}={v} 非数值")

    bf = payload.get("比分")
    if isinstance(bf, dict):
        for g, sub in bf.items():
            if isinstance(sub, dict):
                check_odds(f"比分/{g}", sub)
            else:
                check_odds("比分", {g: sub})
    for mk in ("总进球", "半全场"):
        if isinstance(payload.get(mk), dict):
            check_odds(mk, payload[mk])
    return warns


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--file", default=None, help="archive/odds/ 下的赔率文件名")
    p.add_argument("--home", default=None)
    p.add_argument("--away", default=None)
    p.add_argument("--date", default=None, help="UTC日期 YYYYMMDD（与赔率文件命名一致）")
    p.add_argument("--input", default=None, help="payload JSON 文件；省略则读 stdin")
    a = p.parse_args()

    path = locate(a)
    if not os.path.exists(path):
        raise SystemExit(f"赔率文件不存在：{path}\n先跑 fetch_odds_api.py 生成该场，再灌 cn_markets。")

    payload = load_payload(a)
    warns = validate(payload)

    with open(path, "r", encoding="utf-8") as f:
        obj = json.load(f)

    cn = obj.get("cn_markets") or {}
    cn.setdefault("source", payload.get("source", "中国竞彩官方计算器"))
    if payload.get("source"):
        cn["source"] = payload["source"]
    if payload.get("snapshot_time"):
        cn["snapshot_time"] = payload["snapshot_time"]
    if "open" in payload:
        cn["open"] = {**cn.get("open", {}), **payload["open"]}
    for mk in MARKET_KEYS:
        if mk in payload:
            cn[mk] = payload[mk]
    obj["cn_markets"] = cn

    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)

    filled = [mk for mk in MARKET_KEYS if mk in cn]
    print(f"已写入 cn_markets 到 {os.path.basename(path)}：{', '.join(filled) or '(仅 open/source)'}")
    print(f"来源：{cn.get('source')}  快照：{cn.get('snapshot_time','未标注')}")
    for w in warns:
        print(f"  ⚠ {w}")
    print("下一步：python skill/scripts/build_dashboard.py 刷新看板。")


if __name__ == "__main__":
    main()
