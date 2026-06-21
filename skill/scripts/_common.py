# -*- coding: utf-8 -*-
"""共用：读写 bets.json。按 §4 以字符串存比分/赛果，防日期化。

注：旧 Excel 表格已弃用（用户决定）。下注记录权威源为 bets.json，
人读用可视化页面 archive/dashboard/index.html（由 build_dashboard.py 生成）。"""
import json
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BETS_PATH = os.path.join(BASE, "archive", "bets.json")
SETTLEMENT_BASIS = "90分钟含伤停补时，不含加时点球"

FIELDS = ["bet_id", "下注日期", "比赛日", "类型", "票号", "比赛", "让球数",
          "下注内容", "投入", "赔率", "总赔率", "收盘赔率", "CLV", "结算口径",
          "赛果", "命中", "返还", "盈亏",
          "证据状态", "结论类型", "备注"]


def normalize_bet(bet):
    """补齐新增字段，兼容旧票。"""
    bet.setdefault("收盘赔率", None)
    bet.setdefault("CLV", None)
    bet.setdefault("结算口径", SETTLEMENT_BASIS)
    return bet


def normalize(data):
    """补齐 bets.json 的当前 schema 必备字段。"""
    for bet in data.get("bets", []):
        normalize_bet(bet)
    return data


def load():
    with open(BETS_PATH, "r", encoding="utf-8") as f:
        return normalize(json.load(f))


def save(data):
    normalize(data)
    with open(BETS_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
