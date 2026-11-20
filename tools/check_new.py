"""Checks a batch of NEW (pre-id) question lines against format rules and against the whole bank.

Usage: python tools/check_new.py tools/questions_src/part_54.txt [more files...]

Pre-id line format (correct answer first):
    category|age:diff[,age:diff...]|question text|correct|wrong1|wrong2|wrong3

Reports: format errors, exact duplicates, near-duplicate texts (difflib), and "same fact" candidates
(same correct answer + similar word set) against ALL other lines in tools/questions_src (existing
bank and other new batches). Exit code 1 if anything has to be fixed.
"""

import difflib
import re
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_questions import AGES, CATEGORIES, LETTERS, SRC_DIR, ANSWERS_FILE  # noqa: E402

NEAR = 0.88
JACCARD = 0.75


def norm(s):
    return re.sub(r"[\W_]+", "", s.lower())


STOP = {"який", "яка", "яке", "які", "якого", "якій", "скільки", "хто", "що", "яку", "цих", "як", "де", "та", "від",
        "називається", "називають", "називаються", "буде"}
NUM = {"один": "1", "одна": "1", "два": "2", "дві": "2", "три": "3", "чотири": "4", "пять": "5", "шість": "6",
       "сім": "7", "вісім": "8", "девять": "9", "десять": "10", "одинадцять": "11", "дванадцять": "12"}


def stems(s):
    return {w[:5] for w in re.findall(r"\w+", s.lower()) if len(w) > 2 and w not in STOP}


def ans_key(s):
    k = norm(s)
    return NUM.get(k, k)


def words(s):
    return {w for w in re.findall(r"\w+", s.lower()) if len(w) > 3}


def load_answers():
    d = {}
    for l in ANSWERS_FILE.read_text(encoding="utf-8").splitlines():
        if l and not l.startswith("#"):
            a, b = l.split("|")
            d[int(a)] = b
    return d


def read(path, answers):
    out = []
    for n, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        s = raw.strip()
        if not s or s.startswith("#"):
            continue
        p = [x.strip() for x in s.split("|")]
        where = f"{path.name}:{n}"
        if len(p) == 8 and p[0].isdigit():
            correct = (
                p[3 + 1 + LETTERS.index(answers[int(p[0])])]
                if int(p[0]) in answers
                else ""
            )
            out.append((where, p[1], p[2], p[3], correct, p[4:8]))
        elif len(p) == 7:
            out.append((where, p[0], p[1], p[2], p[3], p[3:7]))
        else:
            out.append((where, None, None, s, None, f"bad field count {len(p)}"))
    return out


def main():
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    targets = {Path(a).resolve() for a in sys.argv[1:]}
    answers = load_answers()
    allq, mine = [], []
    for path in sorted(SRC_DIR.glob("*.txt")):
        rows = read(path, answers)
        (mine if path.resolve() in targets else allq).extend(rows)
    errors = 0

    printed = set()

    def err(where, msg):
        nonlocal errors
        if (where, msg) in printed:
            return
        printed.add((where, msg))
        errors += 1
        print(f"{where}: {msg}")

    # format
    for where, cat, rat, text, correct, opts in mine:
        if cat is None:
            err(where, opts)
            continue
        if cat not in CATEGORIES:
            err(where, f"unknown category {cat}")
        for r in rat.split(","):
            a, _, d = r.partition(":")
            if a not in AGES or not d.isdigit() or not 1 <= int(d) <= 5:
                err(where, f"bad rating {r}")
        if len({x.split(":")[0] for x in rat.split(",")}) != len(rat.split(",")):
            err(where, "duplicate age in ratings")
        if not 10 <= len(text) <= 200 or not text.endswith(("?", ":")):
            err(where, "question length or ending (must end with ? or :)")
        if any(not 1 <= len(o) <= 60 for o in opts):
            err(where, "option length out of 1..60")
        if len({re.sub(r"\s+", " ", o.lower()) for o in opts}) != 4:
            err(where, "options not distinct")

    # index of the rest
    by_norm = {}
    by_correct = defaultdict(list)
    for q in allq:
        if q[1] is None:
            continue
        by_norm.setdefault(norm(q[3]), q)
        by_correct[ans_key(q[4])].append((stems(q[3]), q))
    bucket = defaultdict(list)
    for q in allq + mine:
        if q[1] is None:
            continue
        n = norm(q[3])
        for band in (len(n) // 12 - 1, len(n) // 12, len(n) // 12 + 1):
            bucket[n[:5] + str(band)].append((n, q))

    seen_mine = {}
    for q in mine:
        if q[1] is None:
            continue
        n = norm(q[3])
        if n in by_norm:
            err(q[0], f"EXACT DUP of {by_norm[n][0]}: {q[3]}")
        if n in seen_mine:
            err(q[0], f"EXACT DUP inside batch of {seen_mine[n]}")
        seen_mine.setdefault(n, q[0])
        # near dup
        for band in (len(n) // 12 - 1, len(n) // 12, len(n) // 12 + 1):
            for n2, q2 in bucket[n[:5] + str(band)]:
                if q2[0] == q[0]:
                    continue
                if q2 in mine and q2[0] > q[0]:
                    continue
                # templates ("Яка валюта X?") differ only by a key word; with a different answer that is a different fact
                if norm(q2[4]) != norm(q[4]):
                    continue  # different correct answer = different fact (template questions)
                if set(re.findall(r"\d+", q[3])) != set(re.findall(r"\d+", q2[3])):
                    continue  # different numbers/years = different fact
                thr = NEAR
                sm = difflib.SequenceMatcher(None, n, n2)
                if sm.real_quick_ratio() >= thr and sm.quick_ratio() >= thr and sm.ratio() >= thr:
                    err(q[0], f"NEAR DUP of {q2[0]}:\n    {q[3]}\n    {q2[3]}")
        # same fact: same correct answer, same numbers and heavily overlapping stems
        st = stems(q[3])
        ck = ans_key(q[4])
        cands = list(by_correct.get(ck, []))
        cands += [(stems(m[3]), m) for m in mine if m is not q and m[1] and ans_key(m[4]) == ck and m[0] < q[0]]
        for st2, q2 in cands:
            u = len(st | st2)
            if u and len(st & st2) / u >= 0.45 and set(re.findall(r"\d+", q[3])) == set(re.findall(r"\d+", q2[3])):
                err(q[0], f"SAME FACT? as {q2[0]}: {q[3]} -> {q[4]} || {q2[3]} -> {q2[4]}")
    print(f"checked {len(mine)} lines, {errors} problems")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
