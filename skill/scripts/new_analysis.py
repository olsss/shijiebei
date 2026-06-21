# -*- coding: utf-8 -*-
"""生成一份下注分析留档骨架到 archive/analysis/，并从赔率文件带出 Pinnacle 隐含概率。

留档是"每次下注分析"的权威记录，供可视化页面渲染。长文分析填到 narrative_md，
九维要点填 dimensions，结论/风险/推荐票按 §2A/§3 填。

用法（项目根运行）：
  python skill/scripts/new_analysis.py --odds argentina-vs-austria-20260622.json
  python skill/scripts/new_analysis.py --match "阿根廷 vs 奥地利" --matchday 2026-06-23
"""
import argparse
import json
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ODDS_DIR = os.path.join(BASE, "archive", "odds")
ANA_DIR = os.path.join(BASE, "archive", "analysis")


def implied_from_ref(odds_path):
    with open(odds_path, "r", encoding="utf-8") as f:
        d = json.load(f)
    ref = next((c for c in d.get("companies", [])
                if c.get("reference") and c.get("snapshot") == "live"), None) \
        or next((c for c in d.get("companies", []) if c.get("reference")), None)
    if not ref:
        return d.get("match"), d.get("date"), None
    eu = ref["eu"]
    if any(eu.get(k) in (None, 0) for k in ("home", "draw", "away")):
        return d.get("match"), d.get("date"), {"book": ref["name"], **eu, "implied": None}
    inv = {k: 1.0 / eu[k] for k in ("home", "draw", "away")}
    s = sum(inv.values())
    imp = {k: round(inv[k] / s, 3) for k in inv}
    return d.get("match"), d.get("date"), {"book": ref["name"], **{k: eu[k] for k in ("home", "draw", "away")}, "implied": imp}


def slug(s):
    return s.lower().replace(" ", "-").replace(".", "").replace("（", "").replace("）", "")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--odds", default=None, help="archive/odds 下的赔率文件名，用于带出隐含概率")
    p.add_argument("--match", default=None, help="比赛（无赔率文件时手填），如 '阿根廷 vs 奥地利'")
    p.add_argument("--matchday", default=None)
    p.add_argument("--date-analyzed", default=None, help="分析日期 YYYY-MM-DD")
    a = p.parse_args()

    match, matchday, odds_ref = None, a.matchday, None
    if a.odds:
        path = a.odds if os.path.isabs(a.odds) else os.path.join(ODDS_DIR, a.odds)
        match, matchday2, odds_ref = implied_from_ref(path)
        matchday = matchday or matchday2
    if a.match:
        match = a.match
    if not match:
        raise SystemExit("需要 --odds 或 --match 之一确定比赛")

    tpl_path = os.path.join(ANA_DIR, "_模板.json")
    with open(tpl_path, "r", encoding="utf-8") as f:
        obj = json.load(f)

    obj["match"] = match
    obj["matchday"] = matchday or ""
    obj["date_analyzed"] = a.date_analyzed or ""
    obj["id"] = f"{(matchday or '').replace('-','')}-{slug(match).replace('-vs-','-vs-')}".strip("-")
    if odds_ref:
        obj["odds_ref"] = odds_ref
    if a.odds:
        obj["odds_file"] = os.path.basename(path)

    os.makedirs(ANA_DIR, exist_ok=True)
    fn = os.path.join(ANA_DIR, f"{obj['id']}.json")
    if os.path.exists(fn):
        raise SystemExit(f"已存在: {fn}（避免覆盖，请手动编辑）")
    with open(fn, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
    print(f"已建分析留档骨架: {os.path.relpath(fn, BASE)}")
    if odds_ref and odds_ref.get("implied"):
        print(f"  已带出 {odds_ref['book']} 隐含概率: {odds_ref['implied']}")
    print("  填完后重建页面: python skill/scripts/build_dashboard.py")


if __name__ == "__main__":
    main()
