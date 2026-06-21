# -*- coding: utf-8 -*-
"""赔率对比表：返还率 / 凯利指数 / 初赔→即时变化 / 与竞彩SP偏离。

采集由 skill 联网完成并存入 archive/odds/<file>.json（格式见 _模板.json）；
本脚本只做计算与展示，不抓取。

用法：
  python odds_table.py 巴西-vs-海地-20260620.json
  python odds_table.py --file archive/odds/xxx.json
"""
import argparse
import json
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ODDS_DIR = os.path.join(BASE, "archive", "odds")


def payout_rate(eu):
    """返还率 = 1 / Σ(1/赔率)。缺值返回 None。"""
    vals = [eu.get(k) for k in ("home", "draw", "away")]
    if any(v in (None, 0) for v in vals):
        return None
    return 1.0 / sum(1.0 / v for v in vals)


def novig_probs(eu):
    """去水公允概率（按隐含概率归一化）。"""
    vals = {k: eu.get(k) for k in ("home", "draw", "away")}
    if any(v in (None, 0) for v in vals.values()):
        return None
    inv = {k: 1.0 / v for k, v in vals.items()}
    s = sum(inv.values())
    return {k: inv[k] / s for k in inv}


def load(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def fmt(x, n=3):
    return "-" if x is None else f"{x:.{n}f}"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("file", nargs="?", help="archive/odds 下的文件名或相对/绝对路径")
    p.add_argument("--file", dest="file2")
    a = p.parse_args()
    fname = a.file or a.file2
    if not fname:
        raise SystemExit("请提供赔率文件名，如 巴西-vs-海地-20260620.json")
    path = fname if os.path.isabs(fname) or os.path.exists(fname) else os.path.join(ODDS_DIR, fname)
    if not os.path.exists(path):
        raise SystemExit(f"找不到文件: {path}")

    d = load(path)
    print(f"\n比赛: {d.get('match')}  日期: {d.get('date')}  编号: {d.get('jc_code')}")

    # 竞彩官方 SP
    off = d.get("official", {})
    had = off.get("HAD", {})
    print(f"竞彩官方 HAD(开售={off.get('HAD_open')}): "
          f"主 {fmt(had.get('home'),2)} 平 {fmt(had.get('draw'),2)} 客 {fmt(had.get('away'),2)}")
    hh = off.get("HHAD", {})
    print(f"竞彩官方 HHAD(让{hh.get('handicap')}): "
          f"让胜 {fmt(hh.get('home'),2)} 让平 {fmt(hh.get('draw'),2)} 让负 {fmt(hh.get('away'),2)}")

    # 基准公司公允概率（凯利指数用）
    ref = next((c for c in d.get("companies", []) if c.get("reference") and c.get("snapshot") == "live"), None)
    if not ref:
        ref = next((c for c in d.get("companies", []) if c.get("reference")), None)
    p_ref = novig_probs(ref["eu"]) if ref else None
    if p_ref:
        print(f"\n凯利指数基准: {ref['name']}({ref.get('snapshot')}) "
              f"去水概率 主{fmt(p_ref['home'])} 平{fmt(p_ref['draw'])} 客{fmt(p_ref['away'])}")
    else:
        print("\n[提示] 未设 reference 公司或基准赔率不全，凯利指数无法计算。")

    # 各公司表
    print("\n{:<12}{:<6}{:>8}{:>8}{:>8}{:>9}{:>8}{:>8}{:>8}".format(
        "公司", "快照", "主", "平", "客", "返还率", "凯主", "凯平", "凯客"))
    print("-" * 83)
    for c in d.get("companies", []):
        eu = c.get("eu", {})
        r = payout_rate(eu)
        k = {"home": None, "draw": None, "away": None}
        if p_ref:
            for key in k:
                o = eu.get(key)
                k[key] = o * p_ref[key] if o else None
        print("{:<12}{:<6}{:>8}{:>8}{:>8}{:>9}{:>8}{:>8}{:>8}".format(
            c.get("name", "?")[:11], c.get("snapshot", "")[:5],
            fmt(eu.get("home"), 2), fmt(eu.get("draw"), 2), fmt(eu.get("away"), 2),
            fmt(r), fmt(k["home"]), fmt(k["draw"]), fmt(k["away"])))

    # 初赔→即时 变化（按公司配对）
    print("\n【初赔 → 即时 变化】")
    by_name = {}
    for c in d.get("companies", []):
        by_name.setdefault(c.get("name"), {})[c.get("snapshot")] = c.get("eu", {})
    changed = False
    for name, snaps in by_name.items():
        if "open" in snaps and "live" in snaps:
            changed = True
            for key, lab in (("home", "主"), ("draw", "平"), ("away", "客")):
                o, l = snaps["open"].get(key), snaps["live"].get(key)
                if o and l and abs(l - o) >= 0.01:
                    arrow = "↓降" if l < o else "↑升"
                    print(f"  {name} {lab}: {o:.2f} → {l:.2f} {arrow}{abs(l-o):.2f}")
    if not changed:
        print("  无初赔/即时配对数据。")

    print("\n[解读] 返还率高=去水少更可信；凯利>1=赔率高于基准公允概率（价值或诱盘，结合大赛气质）；"
          "临场大幅升降需对照伤停/首发/天气，不可只看赔率（§2C/odds-collection.md）。")


if __name__ == "__main__":
    main()
