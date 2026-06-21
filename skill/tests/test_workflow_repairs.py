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

    def test_dashboard_bets_table_distinguishes_decision_sources(self):
        dashboard = fresh_module("build_dashboard")
        html = dashboard.bets_table(
            [
                {
                    "比赛日": "2026-06-22",
                    "比赛": "乌拉圭 vs 佛得角",
                    "类型": "HHAD",
                    "下注内容": "乌拉圭-1 让负",
                    "投入": 64,
                    "赔率": 2.89,
                    "收盘赔率": None,
                    "CLV": None,
                    "结算口径": "90分钟含伤停补时，不含加时点球",
                    "赛果": "待赛果",
                    "命中": None,
                    "返还": None,
                    "盈亏": None,
                    "决策方": "Claude (Opus 4.8)",
                },
                {
                    "比赛日": "2026-06-22",
                    "比赛": "西班牙 vs 沙特",
                    "类型": "HHAD",
                    "下注内容": "西班牙-2 让平",
                    "投入": 40,
                    "赔率": 4.55,
                    "收盘赔率": None,
                    "CLV": None,
                    "结算口径": "90分钟含伤停补时，不含加时点球",
                    "赛果": "待赛果",
                    "命中": None,
                    "返还": None,
                    "盈亏": None,
                    "决策方": "gpt",
                },
            ]
        )
        self.assertIn("决策方对比", html)
        self.assertIn("Claude", html)
        self.assertIn("GPT", html)
        self.assertIn("source-badge source-claude", html)
        self.assertIn("source-badge source-gpt", html)
        self.assertIn('class="bet-row bet-source-claude"', html)
        self.assertIn('class="bet-row bet-source-gpt"', html)
        self.assertIn("¥64", html)
        self.assertIn("¥40", html)

    def test_dashboard_supports_matchday_date_filter(self):
        dashboard = fresh_module("build_dashboard")
        card = dashboard.analysis_card(
            {
                "match": "法国 vs 伊拉克",
                "matchday": "2026-06-23",
                "conclusion_type": "盘口判断",
                "confidence": "中",
                "result": {"score": "待赛果"},
                "dimensions": {},
                "recommended": [],
            },
            {},
        )
        table = dashboard.bets_table(
            [
                {
                    "比赛日": "2026-06-23",
                    "比赛": "法国 vs 伊拉克",
                    "类型": "HAD",
                    "下注内容": "法国胜",
                    "投入": 10,
                    "赔率": 1.5,
                    "收盘赔率": None,
                    "CLV": None,
                    "结算口径": "90分钟含伤停补时，不含加时点球",
                    "赛果": "待赛果",
                    "命中": None,
                    "返还": None,
                    "盈亏": None,
                }
            ]
        )
        self.assertIn('data-matchday="2026-06-23"', card)
        self.assertIn('data-matchday="2026-06-23"', table)
        self.assertIn('<select id="dateFilter"', dashboard.HTML)
        self.assertIn("applyDateFilter", dashboard.HTML)

    def test_dashboard_renders_business_intelligence_not_raw_skill_files(self):
        dashboard = fresh_module("build_dashboard")
        matches = dashboard.schedule_matches()
        self.assertTrue(any(m["code"] == "周日037" and m["match"] == "西班牙 vs 沙特" for m in matches))

        schedule_html = dashboard.schedule_html()
        self.assertIn("赛程作战台", schedule_html)
        self.assertIn("match-card", schedule_html)
        self.assertIn("260621", schedule_html)
        self.assertIn("周日037", schedule_html)
        self.assertIn("西班牙 vs 沙特", schedule_html)

        team_html = dashboard.team_intel_html()
        self.assertIn("球队情报矩阵", team_html)
        self.assertIn("阿根廷", team_html)
        self.assertIn("球员状态", team_html)
        self.assertIn("数据完整度", team_html)

        self.assertIn("__SCHEDULE__", dashboard.HTML)
        self.assertIn("__TEAM_INTEL__", dashboard.HTML)
        self.assertIn("__DATA_QUALITY__", dashboard.HTML)
        self.assertNotIn("全部 Skill 数据", dashboard.HTML)

    def test_dashboard_only_displays_requested_market_groups(self):
        dashboard = fresh_module("build_dashboard")
        odds = {
            "source": "The Odds API",
            "books_count": 2,
            "companies": [],
            "markets": {
                "让球(spreads)": {
                    "source": "The Odds API",
                    "companies": [{"name": "Pinnacle", "point": -1.5, "home": 1.91, "away": 1.99}],
                },
                "大小球(totals)": {
                    "source": "The Odds API",
                    "companies": [{"name": "Pinnacle", "point": 2.5, "over": 1.90, "under": 2.00}],
                },
            },
            "cn_markets": {
                "比分": {"1-0": 6.5},
                "总进球": {"0": 8.0, "1": 4.0},
                "半全场": {"胜/胜": 2.2},
            },
        }

        html = dashboard.extra_markets_html(odds)
        self.assertIn("让球 / 比分 / 总进球 / 半全场", html)
        self.assertIn("让球/亚盘", html)
        self.assertIn("比分/总进球/半全场来源", html)
        self.assertIn("比分（90分钟）", html)
        self.assertIn("总进球（90分钟）", html)
        self.assertIn("半全场（90分钟）", html)
        self.assertNotIn("大小球", html)
        self.assertNotIn("totals", html)

    def test_dashboard_cn_market_fallback_mentions_sporttery_not_500(self):
        dashboard = fresh_module("build_dashboard")
        html = dashboard.extra_markets_html({"markets": {}, "cn_markets": {}})

        self.assertIn("Sporttery", html)
        self.assertNotIn("500/中国竞彩官方", html)

    def test_dashboard_defaults_to_nearest_unfinished_matchday_option(self):
        dashboard = fresh_module("build_dashboard")
        analyses = [
            {
                "matchday": "2026-06-20",
                "match": "已赛",
                "result": {"score": "1-0", "hit": True},
            },
            {
                "matchday": "2026-06-23",
                "match": "最近未完赛",
                "result": {"score": "待赛果", "hit": None},
            },
            {
                "matchday": "2026-06-27",
                "match": "更远未完赛",
                "result": {"score": "待赛果", "hit": None},
            },
        ]
        default_day = dashboard.default_matchday(analyses, [], today="2026-06-21")
        options = dashboard.date_filter_options(analyses, [], default_day)
        self.assertEqual(default_day, "2026-06-23")
        self.assertIn('<option value="2026-06-23" selected>2026-06-23（未完赛）</option>', options)
        self.assertIn('<option value="">全部日期</option>', options)

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

    def test_new_analysis_uses_beijing_matchday_from_odds_file(self):
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            odds_dir = base / "archive" / "odds"
            ana_dir = base / "archive" / "analysis"
            odds_dir.mkdir(parents=True)
            ana_dir.mkdir(parents=True)
            (ana_dir / "_模板.json").write_text(
                json.dumps(
                    {
                        "id": "YYYYMMDD-home-vs-away",
                        "date_analyzed": "YYYY-MM-DD",
                        "matchday": "YYYY-MM-DD",
                        "match": "主队 vs 客队",
                        "jc_code": "待竞彩官方核对",
                        "odds_file": "",
                        "odds_ref": {},
                        "recommended": [],
                        "bet_ids": [],
                        "sources": [],
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            odds_file = odds_dir / "argentina-vs-austria-20260622.json"
            odds_file.write_text(
                json.dumps(
                    {
                        "match": "Argentina vs Austria",
                        "date": "2026-06-22",
                        "date_beijing": "2026-06-23",
                        "companies": [
                            {
                                "name": "Pinnacle",
                                "reference": True,
                                "snapshot": "live",
                                "eu": {"home": 1.57, "draw": 4.03, "away": 6.6},
                            }
                        ],
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            new_analysis = fresh_module("new_analysis")
            new_analysis.ODDS_DIR = str(odds_dir)
            new_analysis.ANA_DIR = str(ana_dir)
            argv = ["new_analysis.py", "--odds", odds_file.name, "--date-analyzed", "2026-06-21"]
            with mock.patch.object(sys, "argv", argv):
                new_analysis.main()
            out = ana_dir / "20260623-argentina-vs-austria.json"
            self.assertTrue(out.exists())
            saved = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual(saved["matchday"], "2026-06-23")
            self.assertEqual(saved["id"], "20260623-argentina-vs-austria")
            self.assertEqual(saved["status"], "draft")

    def test_pre_bet_audit_flags_legacy_32_team_data_and_missing_source_files(self):
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            (base / "skill" / "archive" / "analysis").mkdir(parents=True)
            (base / "skill" / "archive" / "sources").mkdir(parents=True)
            (base / "skill" / "archive" / "odds").mkdir(parents=True)
            (base / "skill" / "data").mkdir(parents=True)
            (base / "skill" / "data" / "teams").mkdir(parents=True)
            (base / "skill" / "archive" / "analysis" / "a.json").write_text(
                json.dumps(
                    {
                        "id": "20260623-a-vs-b",
                        "matchday": "2026-06-23",
                        "match": "A vs B",
                        "jc_code": "001",
                        "recommended": [{"type": "HAD", "content": "A胜"}],
                        "bet_ids": ["b1"],
                        "sources": ["archive/sources/missing.json"],
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            (base / "skill" / "archive" / "bets.json").write_text(
                json.dumps({"bets": []}, ensure_ascii=False),
                encoding="utf-8",
            )
            (base / "skill" / "data" / "32-data.md").write_text(
                "# 2026 世界杯 32 强基础数据\n> 最后更新时间：2026-06-21\n",
                encoding="utf-8",
            )
            (base / "skill" / "data" / "48-data.md").write_text(
                "# 2026 世界杯 48 队基础数据\n> 最后更新时间：2026-06-21\n",
                encoding="utf-8",
            )
            audit = fresh_module("pre_bet_audit")
            codes = {issue["code"] for issue in audit.audit_project(base)}
            self.assertIn("DATA_LEGACY_32_SCHEMA", codes)
            self.assertIn("ANALYSIS_SOURCE_MISSING", codes)

    def test_fetch_odds_api_derives_beijing_fields_from_utc_commence_time(self):
        fetch = fresh_module("fetch_odds_api")
        fields = fetch.beijing_time_fields("2026-06-22T17:00:00Z")
        self.assertEqual(fields["date_utc"], "2026-06-22")
        self.assertEqual(fields["date_beijing"], "2026-06-23")
        self.assertEqual(fields["commence_beijing"], "2026-06-23 01:00")
        self.assertEqual(fields["timezone"], "Asia/Shanghai")

    def test_fetch_odds_api_excludes_obvious_h2h_source_outlier(self):
        fetch = fresh_module("fetch_odds_api")
        books = [
            {
                "title": "Pinnacle",
                "markets": [
                    {
                        "key": "h2h",
                        "outcomes": [
                            {"name": "Spain", "price": 1.12},
                            {"name": "Draw", "price": 12.0},
                            {"name": "Saudi Arabia", "price": 23.0},
                        ],
                    }
                ],
            },
            {
                "title": "Betfair",
                "markets": [
                    {
                        "key": "h2h",
                        "outcomes": [
                            {"name": "Spain", "price": 1.12},
                            {"name": "Draw", "price": 13.0},
                            {"name": "Saudi Arabia", "price": 27.0},
                        ],
                    }
                ],
            },
            {
                "title": "William Hill",
                "markets": [
                    {
                        "key": "h2h",
                        "outcomes": [
                            {"name": "Spain", "price": 1.10},
                            {"name": "Draw", "price": 10.0},
                            {"name": "Saudi Arabia", "price": 26.0},
                        ],
                    }
                ],
            },
            {
                "title": "Marathon Bet",
                "markets": [
                    {
                        "key": "h2h",
                        "outcomes": [
                            {"name": "Saudi Arabia", "price": 14.75},
                            {"name": "Spain", "price": 3.42},
                            {"name": "Draw", "price": 1.33},
                        ],
                    }
                ],
            },
        ]

        kept, excluded = fetch.filter_h2h_books(books, "Spain", "Saudi Arabia")
        self.assertEqual([b["title"] for b in kept], ["Pinnacle", "Betfair", "William Hill"])
        self.assertEqual([b["title"] for b in excluded], ["Marathon Bet"])

    def test_fetch_odds_api_preserves_manual_odds_fields_when_merging(self):
        fetch = fresh_module("fetch_odds_api")
        prev = {
            "jc_code": "周日037",
            "official": {"source": "500镜像", "HAD_open": False, "HHAD": {"handicap": "-2"}},
            "cn_markets": {"比分": {"主胜": {"1:0": 9.7}}},
            "snapshot_policy": "首拉为open",
        }
        obj = {
            "jc_code": "待竞彩官方核对",
            "official": {"source": "体彩(弱化，仅出票/结算取SP)", "HAD_open": None, "HHAD": {"handicap": None}},
        }

        fetch.preserve_manual_fields(obj, prev)

        self.assertEqual(obj["jc_code"], "周日037")
        self.assertEqual(obj["official"]["HHAD"]["handicap"], "-2")
        self.assertEqual(obj["cn_markets"]["比分"]["主胜"]["1:0"], 9.7)
        self.assertEqual(obj["snapshot_policy"], "首拉为open")

    def test_pre_bet_audit_flags_team_profile_placeholders(self):
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            (base / "skill" / "archive" / "sources").mkdir(parents=True)
            (base / "skill" / "archive" / "odds").mkdir(parents=True)
            (base / "skill" / "archive" / "analysis").mkdir(parents=True)
            (base / "skill" / "data" / "teams").mkdir(parents=True)
            (base / "skill" / "data" / "48-data.md").write_text(
                "# 48\n> 最后更新时间：2026-06-21\n",
                encoding="utf-8",
            )
            (base / "skill" / "data" / "teams" / "阿根廷.md").write_text(
                "# 阿根廷 档案\n> 最后更新时间：2026-06-21\n- 队名 / 大洲 / 小组：\n- FIFA 排名：\n| - | - | - |\n",
                encoding="utf-8",
            )
            (base / "skill" / "archive" / "bets.json").write_text(
                json.dumps({"bets": []}, ensure_ascii=False),
                encoding="utf-8",
            )
            audit = fresh_module("pre_bet_audit")
            codes = {issue["code"] for issue in audit.audit_project(base)}
            self.assertIn("DATA_TEAM_PLACEHOLDER", codes)

    def test_skill_docs_do_not_contain_stale_budget_excel_or_review_layer_rules(self):
        skill_md = (ROOT / "skill" / "SKILL.md").read_text(encoding="utf-8")
        self.assertIn("name: world-cup-betting-analysis", skill_md)
        self.assertIn("description: Use when", skill_md)
        self.assertNotIn("三层/四层复盘", skill_md)

        claude_md = (ROOT / "CLAUDE.md").read_text(encoding="utf-8")
        self.assertNotIn("必须分三层", claude_md)
        self.assertIn("必须分五层", claude_md)

        review_md = (ROOT / "skill" / "workflow" / "post-match-review.md").read_text(encoding="utf-8")
        self.assertNotIn("竞彩化为四层复盘", review_md)
        self.assertIn("竞彩化为五层复盘", review_md)

        integration_md = (ROOT / "整合方案.md").read_text(encoding="utf-8")
        self.assertNotIn("默认预算：胜平负/让球 100 元，比分 50 元", integration_md)
        self.assertNotIn("下注记录.xlsx", integration_md)
        self.assertNotIn("bets.json`（权威）+ Excel", integration_md)

    def test_pre_bet_audit_downgrades_draft_analysis_blockers(self):
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            (base / "skill" / "archive" / "analysis").mkdir(parents=True)
            (base / "skill" / "archive" / "sources").mkdir(parents=True)
            (base / "skill" / "archive" / "odds").mkdir(parents=True)
            (base / "skill" / "data" / "teams").mkdir(parents=True)
            (base / "skill" / "data").mkdir(exist_ok=True)
            (base / "skill" / "archive" / "analysis" / "draft.json").write_text(
                json.dumps(
                    {
                        "id": "draft",
                        "status": "draft",
                        "jc_code": "待竞彩官方核对",
                        "per_match_form": "待补",
                        "recommended": [],
                        "bet_ids": [],
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            (base / "skill" / "archive" / "bets.json").write_text(
                json.dumps({"bets": []}, ensure_ascii=False),
                encoding="utf-8",
            )
            audit = fresh_module("pre_bet_audit")
            issues = audit.audit_project(base)
            analysis_issues = [i for i in issues if i["path"].endswith("draft.json")]
            self.assertTrue(analysis_issues)
            self.assertNotIn("ERROR", {i["severity"] for i in analysis_issues})
            self.assertIn("INFO", {i["severity"] for i in analysis_issues})

    def test_pre_bet_audit_complete_sources_need_field_coverage(self):
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            (base / "skill" / "archive" / "sources").mkdir(parents=True)
            (base / "skill" / "archive" / "odds").mkdir(parents=True)
            (base / "skill" / "archive" / "analysis").mkdir(parents=True)
            (base / "skill" / "data" / "teams").mkdir(parents=True)
            (base / "skill" / "data").mkdir(exist_ok=True)
            (base / "skill" / "archive" / "sources" / "s.json").write_text(
                json.dumps(
                    {
                        "match": "A vs B",
                        "snapshots": [
                            {"url": "official", "tier": "官方", "用途": "赛程"},
                            {"url": "data", "tier": "数据商", "用途": "伤停"},
                        ],
                        "status": "complete",
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            (base / "skill" / "archive" / "bets.json").write_text(
                json.dumps({"bets": []}, ensure_ascii=False),
                encoding="utf-8",
            )
            audit = fresh_module("pre_bet_audit")
            codes = {issue["code"] for issue in audit.audit_project(base)}
            self.assertIn("SOURCES_NO_FIELD_COVERAGE", codes)


if __name__ == "__main__":
    unittest.main()
