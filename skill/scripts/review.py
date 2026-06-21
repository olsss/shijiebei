# -*- coding: utf-8 -*-
"""算 ROI/CLV 并生成五层复盘骨架（§5）。

用法：
  python review.py                 # 全部已结算票
  python review.py --matchday 2026-06-20   # 仅某比赛日
"""
import argparse
from _common import load


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--matchday", default=None)
    a = p.parse_args()

    data = load()
    bets = [b for b in data["bets"] if b.get("盈亏") is not None]
    if a.matchday:
        bets = [b for b in bets if b.get("比赛日") == a.matchday]
    if not bets:
        raise SystemExit("没有已结算的票（盈亏为空）。先用 settle.py 结算。")

    invest = sum(b["投入"] for b in bets)
    ret = sum(b["返还"] for b in bets)
    pnl = round(ret - invest, 2)
    roi = round(pnl / invest * 100, 2) if invest else 0.0
    hits = [b for b in bets if b.get("命中")]
    clvs = [b.get("CLV") for b in bets if isinstance(b.get("CLV"), (int, float))]
    avg_clv = round(sum(clvs) / len(clvs) * 100, 2) if clvs else None

    print("=" * 40)
    print(f"范围: {a.matchday or '全部'} ｜ 票数: {len(bets)}")
    print(f"总投入: {invest}  总返还: {round(ret,2)}  净盈亏: {pnl}  ROI: {roi}%")
    print(f"命中: {len(hits)}/{len(bets)}")
    print(f"平均CLV: {'未记录' if avg_clv is None else str(avg_clv) + '%'} "
          f"（正值=抓到收线价值，负值=逆势）")
    print("=" * 40)
    print("\n【五层复盘骨架 — 请人工补全】")
    print("1. 数学层: 见上方 ROI；逐票命中：")
    for b in bets:
        clv = b.get("CLV")
        clv_s = "CLV=未记录" if clv is None else f"CLV={clv:+.2%}"
        print(f"   - {b['bet_id']} {b['比赛']} {b['下注内容']} → {b['赛果']} "
              f"命中={b['命中']} 盈亏={b['盈亏']} {clv_s}")
    print("2. 足球层: 判断对/错原因（阵容/伤停/战意/教练/临场）：______")
    print("3. 盘口层: 是否被让球线卡住，是否低估平/小胜/冷门：______")
    print("4. 大赛气质层: 是否低估大赛经验/破密集/守领先/低比分韧性：______")
    print("5. 赔率价值层: 逐票复核入场赔率 vs 收盘赔率；正 CLV 是好下注，负 CLV 要回查逆市场原因：______")
    print("\n【规则沉淀 ≥1 条】写入 rules/betting-rules.md 沉淀区并同步 CLAUDE.md §9：")
    print("   YYYY-MM-DD ｜ 触发场次 ｜ 教训 ｜ 下次必须避免/必须增加")


if __name__ == "__main__":
    main()
