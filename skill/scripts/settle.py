# -*- coding: utf-8 -*-
"""开奖后结算一张票：填赛果/命中/返还/盈亏（§4/§5 数学层）。

用法：
  python settle.py --id 20260620-031-A --result "巴西 3-0" --hit true
  python settle.py --id 20260620-031-A --result "巴西 3-0" --hit true --closing-odds 1.72
  # 命中则返还=投入*总赔率；未命中返还=0。也可手动指定 --return 覆盖。
  # --closing-odds 传该场临场收盘赔率(停售前 live)，自动算 CLV=入场赔率/收盘赔率-1（§5 赔率价值层）。
"""
import argparse
from _common import load, save


def to_bool(s):
    return str(s).lower() in ("true", "1", "yes", "y", "是", "命中")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--id", required=True)
    p.add_argument("--result", required=True, help="字符串赛果/比分，如 巴西 3-0")
    p.add_argument("--hit", required=True)
    p.add_argument("--return", dest="ret", type=float, default=None, help="手动返还，覆盖自动计算")
    p.add_argument("--closing-odds", dest="closing", type=float, default=None,
                   help="该场临场收盘赔率(停售前 live)，用于算 CLV")
    a = p.parse_args()

    data = load()
    bet = next((b for b in data["bets"] if b["bet_id"] == a.id), None)
    if not bet:
        raise SystemExit(f"找不到 bet_id: {a.id}")

    hit = to_bool(a.hit)
    bet["赛果"] = a.result
    bet["命中"] = hit
    if a.ret is not None:
        ret = a.ret
    elif hit:
        odds = bet.get("总赔率") or bet.get("赔率")
        if not odds:
            raise SystemExit("命中但无赔率，请用 --return 手动指定返还")
        ret = round(bet["投入"] * odds, 2)
    else:
        ret = 0.0
    bet["返还"] = ret
    bet["盈亏"] = round(ret - bet["投入"], 2)

    clv_msg = ""
    if a.closing is not None:
        entry = bet.get("总赔率") or bet.get("赔率")
        bet["收盘赔率"] = a.closing
        if entry and a.closing:
            bet["CLV"] = round(entry / a.closing - 1, 4)
            clv_msg = f" 收盘={a.closing} CLV={bet['CLV']:+.2%}"
        else:
            bet["CLV"] = None
            clv_msg = f" 收盘={a.closing}（无入场赔率，CLV 未算）"

    save(data)
    print(f"已结算 {a.id}：命中={hit} 返还={ret} 盈亏={bet['盈亏']}{clv_msg}。"
          f"重建可视化页面: python skill/scripts/build_dashboard.py")


if __name__ == "__main__":
    main()
