"""Cross-batch near-duplicate check for the question bank.

The build validator only errors on exact text duplicates; this script additionally finds
near-duplicate question texts (difflib ratio >= threshold) across ALL source files, using
prefix+length bucketing to stay fast on 10k questions. Run: python tools/check_dupes.py
"""

import difflib
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_DIR = ROOT / "tools" / "questions_src"
THRESHOLD = 0.82


def normalize(s: str) -> str:
    return re.sub(r"[\W_]+", "", s.lower())


def load():
    items = []
    for path in sorted(SRC_DIR.glob("*.txt")):
        for line_no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            s = raw.strip()
            if not s or s.startswith("#"):
                continue
            parts = [p.strip() for p in s.split("|")]
            if len(parts) == 8:  # id|category|ratings|text|A|B|C|D
                text = parts[3]
            elif len(parts) == 7:  # category|ratings|text|correct|w1|w2|w3 (pre-id)
                text = parts[2]
            else:
                continue
            items.append((f"{path.name}:{line_no}", normalize(text)))
    return items


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    items = load()
    print(f"Loaded {len(items)} questions")

    # exact duplicates
    seen = {}
    exact = []
    for where, n in items:
        if n in seen:
            exact.append((seen[n], where))
        else:
            seen[n] = where
    for a, b in exact:
        print(f"EXACT DUP: {a} == {b}")

    # near duplicates: bucket by first 6 normalized chars and length band
    buckets = defaultdict(list)
    for where, n in items:
        for band in (len(n) // 12 - 1, len(n) // 12, len(n) // 12 + 1):
            buckets[n[:6] + str(band)].append((where, n))
    pairs = set()
    for group in buckets.values():
        for i in range(len(group)):
            for j in range(i + 1, len(group)):
                (wa, na), (wb, nb) = group[i], group[j]
                sm = difflib.SequenceMatcher(None, na, nb)
                if sm.real_quick_ratio() >= THRESHOLD and sm.quick_ratio() >= THRESHOLD and sm.ratio() >= THRESHOLD:
                    pairs.add((min(wa, wb), max(wa, wb)))
    for a, b in sorted(pairs):
        print(f"NEAR DUP: {a} ~ {b}")
    print(f"Done: {len(exact)} exact, {len(pairs)} near-duplicate pairs")


if __name__ == "__main__":
    main()
