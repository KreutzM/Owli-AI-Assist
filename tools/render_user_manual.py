from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import html
import re


REPO_ROOT = Path(__file__).resolve().parent.parent


@dataclass(frozen=True)
class ManualSpec:
    source: Path
    target: Path
    lang: str
    head_title: str
    header_note: str
    updated_label: str


MANUALS = (
    ManualSpec(
        source=REPO_ROOT / "docs" / "user-manual" / "user-manual.de.md",
        target=REPO_ROOT / "app" / "src" / "main" / "assets" / "help" / "de" / "index.html",
        lang="de",
        head_title="Owli-AI Assist - Benutzerhandbuch",
        header_note="Offline-Hilfe. Keine Internetverbindung erforderlich.",
        updated_label="Zuletzt aktualisiert:",
    ),
    ManualSpec(
        source=REPO_ROOT / "docs" / "user-manual" / "user-manual.en.md",
        target=REPO_ROOT / "app" / "src" / "main" / "assets" / "help" / "en" / "index.html",
        lang="en",
        head_title="Owli-AI Assist - User Manual",
        header_note="Offline help content. No internet required.",
        updated_label="Last updated:",
    ),
)


def render_inline(text: str) -> str:
    escaped = html.escape(text, quote=False)
    return re.sub(r"`([^`]+)`", lambda match: f"<code>{match.group(1)}</code>", escaped)


def render_markdown_body(markdown: str) -> str:
    lines = markdown.splitlines()
    output: list[str] = []
    i = 0
    while i < len(lines):
        line = lines[i].rstrip()
        stripped = line.strip()
        if not stripped:
            i += 1
            continue

        if stripped.startswith("# "):
            output.append(f"<h1>{render_inline(stripped[2:])}</h1>")
            i += 1
            continue
        if stripped.startswith("## "):
            output.append(f"<h2>{render_inline(stripped[3:])}</h2>")
            i += 1
            continue
        if stripped.startswith("> "):
            quote_lines: list[str] = []
            while i < len(lines) and lines[i].strip().startswith("> "):
                quote_lines.append(lines[i].strip()[2:])
                i += 1
            output.append('<blockquote class="note">')
            for quote_line in quote_lines:
                output.append(f"<p>{render_inline(quote_line)}</p>")
            output.append("</blockquote>")
            continue
        if re.match(r"\d+\.\s", stripped):
            output.append("<ol>")
            while i < len(lines):
                current = lines[i].strip()
                match = re.match(r"\d+\.\s+(.*)", current)
                if not match:
                    break
                output.append(f"<li>{render_inline(match.group(1))}</li>")
                i += 1
            output.append("</ol>")
            continue
        if stripped.startswith("- "):
            output.append("<ul>")
            while i < len(lines):
                current = lines[i].strip()
                if not current.startswith("- "):
                    break
                output.append(f"<li>{render_inline(current[2:])}</li>")
                i += 1
            output.append("</ul>")
            continue

        output.append(f"<p>{render_inline(stripped)}</p>")
        i += 1
    return "\n".join(output)


def extract_updated_value(markdown: str) -> str:
    for line in markdown.splitlines():
        stripped = line.strip()
        if stripped.startswith("Stand: "):
            return stripped.removeprefix("Stand: ").strip()
        if stripped.startswith("Version: "):
            return stripped.removeprefix("Version: ").strip()
    raise ValueError("Missing 'Stand:' or 'Version:' line in user manual")


def render_manual(spec: ManualSpec) -> None:
    markdown = spec.source.read_text(encoding="utf-8")
    updated_value = extract_updated_value(markdown)
    body_html = render_markdown_body(markdown)
    page = "\n".join(
        [
            "<!doctype html>",
            f'<html lang="{spec.lang}">',
            "<head>",
            '  <meta charset="utf-8">',
            '  <meta name="viewport" content="width=device-width, initial-scale=1">',
            f"  <title>{html.escape(spec.head_title)}</title>",
            '  <link rel="stylesheet" href="../help.css">',
            "</head>",
            "<body>",
            "<!-- Generated from docs/user-manual/*.md via tools/render_user_manual.py -->",
            "<header>",
            f"  <h1>{html.escape(spec.head_title)}</h1>",
            f'  <p class="small">{html.escape(spec.header_note)}</p>',
            "</header>",
            "<main>",
            body_html,
            "</main>",
            "<footer>",
            "  <hr>",
            f'  <p class="small">{html.escape(spec.updated_label)} {html.escape(updated_value)}</p>',
            "</footer>",
            "</body>",
            "</html>",
            "",
        ]
    )
    spec.target.write_text(page, encoding="utf-8")


def main() -> None:
    for spec in MANUALS:
        render_manual(spec)


if __name__ == "__main__":
    main()
