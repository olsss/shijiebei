# -*- coding: utf-8 -*-
"""出票前硬规则审计。

检查项目是否满足 CLAUDE.md / skill 里最容易漏掉的硬门槛：
- 竞彩官方编号/玩法/让球是否已核对；
- 赔率是否有 open + live 快照；
- 是否有多源 sources 快照；
- 分析是否仍有“待补/待核验”；`status=draft/abandoned` 只提示不阻塞；
- bets.json 是否已迁移到 CLV/90 分钟结算口径字段；
- 数据文件是否标最后更新时间。

用法：
  python skill/scripts/pre_bet_audit.py
  python skill/scripts/pre_bet_audit.py --base C:\\Users\\admin\\Desktop\\世界杯
"""
import argparse
import json
import os
from pathlib import Path


BASE = Path(__file__).resolve().parents[2]
REQUIRED_BET_FIELDS = ("收盘赔率", "CLV", "结算口径")
PENDING_MARKERS = ("待补", "待多源核验", "待当日刷新", "待核验", "待竞彩官方核对")
FRESHNESS_MARKERS = ("最后更新时间", "最后更新")
TEAM_PLACEHOLDER_MARKERS = ("队名 / 大洲 / 小组：", "FIFA 排名：", "| - | - |", "来源1（等级/URL/取数时间/快照路径）：")
DRAFT_ANALYSIS_STATUSES = {"draft", "abandoned"}


def issue(severity, code, path, message):
    return {
        "severity": severity,
        "code": code,
        "path": str(path).replace("\\", "/"),
        "message": message,
    }


def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def iter_json_files(path):
    if not path.exists():
        return []
    return [p for p in sorted(path.glob("*.json")) if not p.name.startswith("_")]


def audit_sources(skill):
    src_dir = skill / "archive" / "sources"
    files = iter_json_files(src_dir)
    if not files:
        return [issue("ERROR", "SOURCES_NONE", src_dir, "没有实际 sources 快照；§2C 多源核验未落档。")]
    out = []
    for p in files:
        try:
            d = load_json(p)
        except Exception as exc:
            out.append(issue("ERROR", "SOURCES_INVALID_JSON", p, f"sources JSON 无法解析：{exc}"))
            continue
        snaps = d.get("snapshots", [])
        if len(snaps) < 2:
            out.append(issue("WARN", "SOURCES_TOO_FEW", p, "少于 2 个独立来源。"))
        if d.get("status") != "complete":
            out.append(issue("WARN", "SOURCES_NOT_COMPLETE", p, f"核验状态不是 complete：{d.get('status')}"))
        elif not d.get("field_coverage"):
            out.append(issue("WARN", "SOURCES_NO_FIELD_COVERAGE", p, "complete 快照缺 field_coverage，无法确认关键字段逐项多源覆盖。"))
    return out


def audit_odds(skill):
    odds_dir = skill / "archive" / "odds"
    out = []
    for p in iter_json_files(odds_dir):
        try:
            d = load_json(p)
        except Exception as exc:
            out.append(issue("ERROR", "ODDS_INVALID_JSON", p, f"赔率 JSON 无法解析：{exc}"))
            continue
        snaps = {c.get("snapshot") for c in d.get("companies", [])}
        if "open" not in snaps:
            out.append(issue("WARN", "ODDS_NO_OPEN", p, "缺 open 初赔快照，无法判断初赔→即时变化。"))
        if "live" not in snaps:
            out.append(issue("ERROR", "ODDS_NO_LIVE", p, "缺 live/收盘赔率快照。"))
        if d.get("jc_code") in (None, "", "待竞彩官方核对"):
            out.append(issue("ERROR", "ODDS_NO_JC_CODE", p, "缺竞彩官方编号核对。"))
        official = d.get("official", {})
        if official.get("HAD_open") is None:
            out.append(issue("WARN", "ODDS_NO_HAD_OPEN", p, "缺 HAD 是否开售核对。"))
        if official.get("HHAD", {}).get("handicap") is None:
            out.append(issue("WARN", "ODDS_NO_HHAD_HANDICAP", p, "缺 HHAD 让球数核对。"))
    return out


def audit_analysis(skill):
    ana_dir = skill / "archive" / "analysis"
    out = []
    for p in iter_json_files(ana_dir):
        try:
            d = load_json(p)
        except Exception as exc:
            out.append(issue("ERROR", "ANALYSIS_INVALID_JSON", p, f"分析 JSON 无法解析：{exc}"))
            continue
        is_draft = d.get("status") in DRAFT_ANALYSIS_STATUSES
        incomplete_severity = "INFO" if is_draft else "ERROR"
        warn_severity = "INFO" if is_draft else "WARN"
        if d.get("jc_code") in (None, "", "待竞彩官方核对"):
            out.append(issue(incomplete_severity, "ANALYSIS_NO_JC", p, "分析文件缺竞彩编号。"))
        if not d.get("recommended"):
            out.append(issue(warn_severity, "ANALYSIS_NO_RECOMMEND", p, "recommended 为空，尚未形成出票方案。"))
        if not d.get("bet_ids"):
            out.append(issue(warn_severity, "ANALYSIS_NO_BET_LINK", p, "bet_ids 为空，分析未与实际票关联。"))
        pending_text = json.dumps(d, ensure_ascii=False)
        if any(marker in pending_text for marker in PENDING_MARKERS):
            out.append(issue(warn_severity, "ANALYSIS_PENDING", p, "仍含待补/待核验内容，出票需降额或放弃。"))
        matchday = str(d.get("matchday", "")).replace("-", "")
        if matchday and not str(d.get("id", "")).startswith(matchday):
            out.append(issue("WARN", "ANALYSIS_ID_MATCHDAY_MISMATCH", p, "id/文件日期与 matchday 不一致。"))
        for src in d.get("sources", []):
            if not isinstance(src, str):
                continue
            norm = src.replace("\\", "/")
            if norm.startswith("archive/sources/") or norm.startswith("skill/archive/sources/"):
                rel = norm.removeprefix("skill/")
                if not (skill / rel).exists():
                    out.append(issue("WARN", "ANALYSIS_SOURCE_MISSING", p, f"分析引用的 sources 快照不存在：{src}"))
    return out


def audit_bets(skill):
    p = skill / "archive" / "bets.json"
    if not p.exists():
        return [issue("ERROR", "BETS_MISSING", p, "缺 bets.json。")]
    try:
        d = load_json(p)
    except Exception as exc:
        return [issue("ERROR", "BETS_INVALID_JSON", p, f"bets.json 无法解析：{exc}")]
    out = []
    for idx, bet in enumerate(d.get("bets", []), 1):
        missing = [field for field in REQUIRED_BET_FIELDS if field not in bet]
        if missing:
            out.append(issue("ERROR", "BET_MISSING_FIELD", p, f"第 {idx} 张票缺字段：{', '.join(missing)}"))
        if bet.get("赔率") is None:
            out.append(issue("WARN", "BET_NO_ENTRY_ODDS", p, f"第 {idx} 张票缺入场赔率，无法自动算返还/CLV。"))
        if bet.get("命中") is None:
            out.append(issue("INFO", "BET_PENDING", p, f"第 {idx} 张票待开奖。"))
    return out


def audit_data_freshness(skill):
    data = skill / "data"
    candidates = [
        data / "48-data.md",
        data / "group-rank.md",
        data / "group-schedule.md",
        data / "player-status.md",
        data / "team-data.md",
    ]
    teams = data / "teams"
    if teams.exists():
        candidates.extend(p for p in sorted(teams.glob("*.md")) if not p.name.startswith("_"))
    out = []
    legacy = data / "32-data.md"
    if legacy.exists():
        out.append(issue("WARN", "DATA_LEGACY_32_SCHEMA", legacy, "仍存在旧 32 强基础数据文件；2026 项目应使用 48-data.md，避免与 §15 48 队赛制冲突。"))
    for p in candidates:
        if not p.exists():
            continue
        txt = p.read_text(encoding="utf-8", errors="ignore")
        if not any(marker in txt for marker in FRESHNESS_MARKERS):
            out.append(issue("WARN", "DATA_NO_FRESHNESS", p, "缺最后更新时间标记，§14 时效无法审计。"))
        if teams.exists() and p.parent == teams and any(marker in txt for marker in TEAM_PLACEHOLDER_MARKERS):
            out.append(issue("WARN", "DATA_TEAM_PLACEHOLDER", p, "球队档案仍含模板占位内容；阵容/战意/大赛气质不能视为已建档。"))
    return out


def audit_project(base=None):
    base = Path(base) if base else BASE
    skill = base / "skill"
    issues = []
    issues.extend(audit_sources(skill))
    issues.extend(audit_odds(skill))
    issues.extend(audit_analysis(skill))
    issues.extend(audit_bets(skill))
    issues.extend(audit_data_freshness(skill))
    return issues


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=str(BASE), help="项目根目录")
    args = p.parse_args()
    issues = audit_project(Path(args.base))
    if not issues:
        print("pre-bet audit: PASS（未发现硬规则缺口）")
        return 0
    order = {"ERROR": 0, "WARN": 1, "INFO": 2}
    for it in sorted(issues, key=lambda x: (order.get(x["severity"], 9), x["code"], x["path"])):
        print(f"[{it['severity']}] {it['code']} {it['path']} :: {it['message']}")
    errors = sum(1 for it in issues if it["severity"] == "ERROR")
    warns = sum(1 for it in issues if it["severity"] == "WARN")
    infos = sum(1 for it in issues if it["severity"] == "INFO")
    print(f"\n汇总：ERROR={errors} WARN={warns} INFO={infos}")
    print("有 ERROR 时不建议出票；WARN 未补齐时必须降额或放弃。")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
