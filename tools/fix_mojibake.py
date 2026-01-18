import argparse
from pathlib import Path

def fix_file(path: Path) -> None:
    original = path.read_text(encoding="utf-8")
    fixed = original.encode("cp1252").decode("utf-8")
    if fixed != original:
        path.write_text(fixed, encoding="utf-8", newline="\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Fix mojibake by reversing CP1252->UTF-8 mis-decoding.")
    parser.add_argument("files", nargs="+", help="Markdown files to fix")
    args = parser.parse_args()
    for file in args.files:
        fix_file(Path(file))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
