import contextlib
import importlib
import io
import json
import sys
import tempfile
import unittest
from unittest import mock
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / "skill" / "scripts"
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))


def fresh_module(name):
    sys.modules.pop(name, None)
    return importlib.import_module(name)


@contextlib.contextmanager
def temp_bets_doc(initial):
    with tempfile.TemporaryDirectory() as td:
        path = Path(td) / "bets.json"
        path.write_text(json.dumps(initial, ensure_ascii=False, indent=2), encoding="utf-8")
        common = fresh_module("_common")
        common.BETS_PATH = str(path)
        try:
            yield path
        finally:
            for name in ("new_bet", "settle", "review", "_common"):
                sys.modules.pop(name, None)


class WorkflowRepairTests(unittest.TestCase):
    def test_new_bet_writes_clv_and_settlement_basis_fields(self):
        doc = {"edition": "2026", "currency": "CNY", "schema": {}, "bets": []}
        with temp_bets_doc(doc) as bets_path:
            new_bet = fresh_module("new_bet")
            argv = [
                "new_bet.py",
                "--id", "t1",
                "--date", "2026-06-21",
                "--matchday", "2026-06-22",
                "--type", "总进球",
                "--match", "法国 vs 伊拉克",
                "--content", "总进球 0-1",
                "--stake", "10",
                "--odds", "2.5",
            ]
            with mock.patch.object(sys, "argv", argv):
                new_bet.main()
            saved = json.loads(bets_path.read_text(encoding="utf-8"))
            bet = saved["bets"][0]
            self.assertIsNone(bet["收盘赔率"])
            self.assertIsNone(bet["CLV"])
            self.assertEqual(bet["结算口径"], "90分钟含伤停补时，不含加时点球")

    def test_settle_migrates_old_bet_and_calculates_clv(self):
        doc = {
            "edition": "2026",
            "currency": "CNY",
            "schema": {},
            "bets": [
                {
                    "bet_id": "old",
                    "下注日期": "2026-06-20",
                    "比赛日": "2026-06-20",
                    "类型": "HHAD",
                    "票号": "",
                    "比赛": "巴西 vs 海地",
                    "让球数": -2,
                    "下注内容": "巴西-2 让胜",
                    "投入": 20,
                    "赔率": 2.0,
                    "总赔率": 2.0,
                    "赛果": "待赛果",
                    "命中": None,
                    "返还": None,
                    "盈亏": None,
                    "证据状态": "partial",
                    "结论类型": "盘口判断",
                    "备注": "",
                }
            ],
        }
        with temp_bets_doc(doc) as bets_path:
            settle = fresh_module("settle")
            argv = [
                "settle.py",
                "--id", "old",
                "--result", "巴西 1-0",
                "--hit", "false",
                "--closing-odds", "1.80",
            ]
            with mock.patch.object(sys, "argv", argv):
                settle.main()
            saved = json.loads(bets_path.read_text(encoding="utf-8"))
            bet = saved["bets"][0]
            self.assertEqual(bet["收盘赔率"], 1.8)
            self.assertEqual(bet["CLV"], round(2.0 / 1.8 - 1, 4))
            self.assertEqual(bet["结算口径"], "90分钟含伤停补时，不含加时点球")
            self.assertEqual(bet["盈亏"], -20)

    def test_review_outputs_five_layers_and_clv_summary(self):
        doc = {
            "edition": "2026",
            "currency": "CNY",
            "schema": {},
            "bets": [
                {
                    "bet_id": "b1",
                    "比赛日": "2026-06-20",
                    "比赛": "巴西 vs 海地",
                    "下注内容": "总进球 0-1",
                    "投入": 10,
                    "赔率": 2.0,
                    "总赔率": 2.0,
                    "赛果": "巴西 1-0",
                    "命中": True,
                    "返还": 20,
                    "盈亏": 10,
                    "收盘赔率": 1.8,
                    "CLV": 0.1111,
                }
            ],
        }
        with temp_bets_doc(doc):
            review = fresh_module("review")
            buf = io.StringIO()
            with contextlib.redirect_stdout(buf):
                with mock.patch.object(sys, "argv", ["review.py"]):
                    review.main()
            out = buf.getvalue()
            self.assertIn("五层复盘", out)
            self.assertIn("赔率价值层", out)
            self.assertIn("平均CLV", out)

    def test_dashboard_bets_table_includes_clv_columns(self):
        dashboard = fresh_module("build_dashboard")
        html = dashboard.bets_table(
            [
                {
                    "比赛": "法国 vs 伊拉克",
                    "类型": "总进球",
                    "下注内容": "0-1",
                    "投入": 10,
                    "赔率": 2.0,
                    "收盘赔率": 1.8,
                    "CLV": 0.1111,
                    "结算口径": "90分钟含伤停补时，不含加时点球",
                    "赛果": "待赛果",
                    "命中": None,
                    "返还": None,
                    "盈亏": None,
                }
            ]
        )
        self.assertIn("收盘赔率", html)
        self.assertIn("CLV", html)
        self.assertIn("结算口径", html)
        self.assertIn("+11.11%", html)

    def test_pre_bet_audit_reports_core_blockers(self):
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            (base / "skill" / "archive" / "sources").mkdir(parents=True)
            (base / "skill" / "archive" / "odds").mkdir(parents=True)
            (base / "skill" / "archive" / "analysis").mkdir(parents=True)
            (base / "skill" / "data" / "teams").mkdir(parents=True)
            (base / "skill" / "data").mkdir(exist_ok=True)
            (base / "skill" / "archive" / "sources" / "_模板.json").write_text("{}", encoding="utf-8")
            (base / "skill" / "archive" / "odds" / "m.json").write_text(
                json.dumps(
                    {
                        "match": "A vs B",
                        "jc_code": "待竞彩官方核对",
                        "official": {"HAD_open": None, "HHAD": {"handicap": None}},
                        "companies": [{"snapshot": "live", "eu": {"home": 2, "draw": 3, "away": 4}}],
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            (base / "skill" / "archive" / "analysis" / "a.json").write_text(
                json.dumps({"id": "a", "jc_code": "待竞彩官方核对", "per_match_form": "待补"}, ensure_ascii=False),
                encoding="utf-8",
            )
            (base / "skill" / "archive" / "bets.json").write_text(
                json.dumps({"bets": [{"bet_id": "b", "赔率": 2.0}]}, ensure_ascii=False),
                encoding="utf-8",
            )
            (base / "skill" / "data" / "group-rank.md").write_text("# ranking\n", encoding="utf-8")
            audit = fresh_module("pre_bet_audit")
            codes = {issue["code"] for issue in audit.audit_project(base)}
            self.assertIn("SOURCES_NONE", codes)
            self.assertIn("ODDS_NO_OPEN", codes)
            self.assertIn("ODDS_NO_JC_CODE", codes)
            self.assertIn("ANALYSIS_NO_JC", codes)
            self.assertIn("BET_MISSING_FIELD", codes)
            self.assertIn("DATA_NO_FRESHNESS", codes)


if __name__ == "__main__":
    unittest.main()
