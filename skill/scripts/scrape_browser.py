# -*- coding: utf-8 -*-
"""通用浏览器抓取：用本机 Edge 无头渲染 JS 页面，输出渲染后文本 + 整页截图。

依赖：pip install playwright（复用已装 Edge，channel=msedge，无需下载 Chromium）。
适用：WebFetch 拿不到/被简单挡的 JS 赔率/赛程页。
注意：oddschecker / oddspedia 等用 Cloudflare 人机校验，无头浏览器仍会被拦；
      betexplorer / flashscore / forebet 实测可达（结构各异，需按页解析）。

用法：
  python scrape_browser.py "https://www.flashscore.com/football/world/world-championship/fixtures/" \
    --shot fixtures.png --grep Argentina,Austria,France,Iraq
"""
import argparse
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOT_DIR = os.path.join(BASE, "archive", "odds", "shots")
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36 Edg/126.0")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("url")
    p.add_argument("--shot", default=None, help="截图文件名（存到 archive/odds/shots/）")
    p.add_argument("--grep", default=None, help="逗号分隔关键词，只打印含关键词的行")
    p.add_argument("--wait", type=int, default=5000, help="渲染等待毫秒")
    p.add_argument("--selector", default=None, help="只取某选择器的 innerText 列表")
    a = p.parse_args()

    from playwright.sync_api import sync_playwright
    os.makedirs(SHOT_DIR, exist_ok=True)
    with sync_playwright() as pw:
        b = pw.chromium.launch(channel="msedge", headless=True)
        pg = b.new_page(user_agent=UA, locale="en-US",
                        viewport={"width": 1366, "height": 1800})
        pg.goto(a.url, timeout=45000, wait_until="domcontentloaded")
        pg.wait_for_timeout(a.wait)
        for sel in ("#onetrust-accept-btn-handler", "button:has-text('Accept')"):
            try:
                pg.click(sel, timeout=2000); pg.wait_for_timeout(1000); break
            except Exception:
                pass

        txt = pg.inner_text("body")
        low = txt.lower()
        if any(x in low for x in ["just a moment", "security verification",
                                  "verify you are human", "access denied", "captcha"]):
            print("[BLOCKED] 该站启用了人机校验，无头浏览器被拦。换可达源或人工贴数据。")

        if a.selector:
            items = pg.eval_on_selector_all(a.selector, "els=>els.map(e=>e.innerText)")
            for it in items:
                print("::", " ".join(it.split())[:200])
        else:
            kws = [k.strip() for k in a.grep.split(",")] if a.grep else None
            for line in txt.split("\n"):
                s = " ".join(line.split())
                if not s:
                    continue
                if kws is None or any(k in s for k in kws):
                    print(s[:200])

        if a.shot:
            out = os.path.join(SHOT_DIR, a.shot)
            pg.screenshot(path=out, full_page=True)
            print("SHOT:", out)
        b.close()


if __name__ == "__main__":
    main()
