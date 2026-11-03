#!/usr/bin/env python3
"""UI 用の小さなサブセットフォントを作る。

画面の文言（ui/strings/*.kt）と、かな・記号・ラテン文字だけを含む
軽いフォントを `*_ui.ttf` として書き出す。本文はまずこのサブセットで描き、
稀少な漢字（科目名など）は後から届くフルフォントで補う。

使い方:
    uvx --from fonttools python kmp/tools/subset_fonts.py
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FONT_DIR = ROOT / "kmp/composeApp/src/commonMain/composeResources/font"
STRINGS_DIR = ROOT / "kmp/composeApp/src/commonMain/kotlin/org/openyaske/app/ui/strings"
UI_SOURCES = ROOT / "kmp/composeApp/src/commonMain/kotlin/org/openyaske/app"

EXTRA = (
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    " .,:;!?'\"()[]{}<>/\\|@#$%^&*-_=+~`"
    "、。・，．：；！？「」『』（）〔〕【】〈〉《》"
    "ー〜～…‥±×÷→←↑↓↔›‹‹»«°℃%¥￥$€£"
    "０１２３４５６７８９"
)


def content_corpus(ui: str) -> str:
    """UI の文言に、同梱シラバスの文字を足す。科目名はここでほぼ賄える。"""
    chars = set(ui)
    for path in sorted((ROOT / "kmp/data/syllabus").glob("*.tsv")):
        chars.update(path.read_text(encoding="utf-8", errors="ignore"))
    return "".join(sorted(chars))


def ui_corpus() -> str:
    """UI に出る文字。文言ファイルと、画面に直接書いた文字の両方から拾う。"""
    chars = set(EXTRA)
    paths = list(STRINGS_DIR.glob("*.kt")) + list(UI_SOURCES.rglob("*.kt"))
    for path in paths:
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(r'"([^"\\]*(?:\\.[^"\\]*)*)"', text):
            chars.update(match.group(1))
    return "".join(sorted(chars))


def main() -> int:
    ui = ui_corpus()
    content = content_corpus(ui)
    ui_path = Path("/tmp/openyaske-ui-corpus.txt")
    ui_path.write_text(ui, encoding="utf-8")
    content_path = Path("/tmp/openyaske-content-corpus.txt")
    content_path.write_text(content, encoding="utf-8")
    print(f"ui: {len(ui)} characters / content: {len(content)} characters")

    jobs = [
        ("ui", ui_path),
        ("content", content_path),
    ]
    for suffix, text_file in jobs:
        for source in sorted(FONT_DIR.glob("noto_sans_*.ttf")):
            if source.stem.endswith("_ui") or source.stem.endswith("_content"):
                continue
            target = source.with_name(f"{source.stem}_{suffix}.ttf")
            cmd = [
                "uvx",
                "--from",
                "fonttools",
                "pyftsubset",
                str(source),
                f"--text-file={text_file}",
                f"--output-file={target}",
                "--layout-features=*",
                "--no-hinting",
                "--desubroutinize",
            ]
            subprocess.run(cmd, check=True)
            before = source.stat().st_size
            after = target.stat().st_size
            print(f"{source.name}: {before / 1e6:.1f}MB -> {target.name}: {after / 1e3:.0f}KB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
