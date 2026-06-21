# -*- coding: utf-8 -*-
"""写一张待开奖票到 bets.json（返还/盈亏留空，§4）。

用法示例：
  python new_bet.py --id 20260620-031-B --date 2026-06-20 --matchday 2026-06-20 \
    --type HHAD --match "巴西 vs 海地" --handicap -2 --content "巴西-2 让平" \
    --stake 30 --odds 3.10 --ticket 25061900123 --evidence partial \
    --conclusion 盘口判断 --note "票面SP优先"

说明：--ticket 为竞彩票面票号（出票后补填，先下注可留空）；比分/4串1 类型 --handicap 填 0。
"""
import argparse
from _common import SETTLEMENT_BASIS, load, save


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--id", required=True)
    p.add_argument("--date", required=True)
    p.add_argument("--matchday", required=True)
    p.add_argument("--type", required=True, help="HAD/HHAD/比分/总进球/半全场/串关")
    p.add_argument("--ticket", default="")
    p.add_argument("--match", required=True)
    p.add_argument("--handicap", type=int, default=0)
    p.add_argument("--content", required=True, help="字符串，如 巴西-2 让胜 / 比分 1-0")
    p.add_argument("--stake", type=float, required=True)
    p.add_argument("--odds", type=float, default=None)
    p.add_argument("--total-odds", type=float, default=None)
    p.add_argument("--evidence", default="partial", choices=["complete", "partial", "blocked"])
    p.add_argument("--conclusion", default="盘口判断")
    p.add_argument("--note", default="")
    a = p.parse_args()

    data = load()
    if any(b["bet_id"] == a.id for b in data["bets"]):
        raise SystemExit(f"bet_id 已存在: {a.id}")

    data["bets"].append({
        "bet_id": a.id, "下注日期": a.date, "比赛日": a.matchday, "类型": a.type,
        "票号": a.ticket, "比赛": a.match, "让球数": a.handicap, "下注内容": a.content,
        "投入": a.stake, "赔率": a.odds, "总赔率": a.total_odds if a.total_odds else a.odds,
        "收盘赔率": None, "CLV": None, "结算口径": SETTLEMENT_BASIS,
        "赛果": "待赛果", "命中": None, "返还": None, "盈亏": None,
        "证据状态": a.evidence, "结论类型": a.conclusion, "备注": a.note,
    })
    save(data)
    print(f"已写入 {a.id}。重建可视化页面: python skill/scripts/build_dashboard.py")


if __name__ == "__main__":
    main()
