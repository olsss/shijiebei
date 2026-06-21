# -*- coding: utf-8 -*-
"""为一支球队建立档案 data/teams/<队名>.md（从 _模板.md 复制）。

用法（项目根运行）：
  python skill/scripts/new_team.py --name 阿根廷
  python skill/scripts/new_team.py --name 奥地利 --group J --rank 25 --coach 兰尼克
批量：
  python skill/scripts/new_team.py --names 法国,伊拉克,挪威,塞内加尔
"""
import argparse
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEAMS = os.path.join(BASE, "data", "teams")
TPL = os.path.join(TEAMS, "_模板.md")


def make(name, group="", rank="", coach=""):
    with open(TPL, "r", encoding="utf-8") as f:
        body = f.read()
    # 去掉模板抬头说明，替换标题
    body = body.split("\n", 1)[1] if body.startswith("# 球队档案模板") else body
    head = f"# {name} 档案\n"
    if group or rank or coach:
        head += f"\n<!-- 小组:{group} FIFA排名:{rank} 主帅:{coach} （核验后填进基本档案） -->\n"
    out = head + body
    fn = os.path.join(TEAMS, f"{name}.md")
    if os.path.exists(fn):
        print(f"  跳过(已存在): {name}.md")
        return
    with open(fn, "w", encoding="utf-8") as f:
        f.write(out)
    print(f"  已建: data/teams/{name}.md")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--name", default=None)
    p.add_argument("--names", default=None, help="逗号分隔批量建档")
    p.add_argument("--group", default="")
    p.add_argument("--rank", default="")
    p.add_argument("--coach", default="")
    a = p.parse_args()
    names = []
    if a.name:
        names.append(a.name)
    if a.names:
        names += [n.strip() for n in a.names.split(",") if n.strip()]
    if not names:
        raise SystemExit("需要 --name 或 --names")
    for n in names:
        make(n, a.group, a.rank, a.coach)
    print("提示：档案需 §2C 多源核验后填写；战意/排名节按 §2D 维护。")


if __name__ == "__main__":
    main()
