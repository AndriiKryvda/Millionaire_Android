"""Builds questions.json and answers.json from tools/questions_src/*.txt and validates the bank.

Source line format (one question per line, '#' starts a comment line):
    category|age:difficulty[,age:difficulty...]|question text|correct answer|wrong 1|wrong 2|wrong 3

IDs are assigned sequentially in file order. The correct answer's letter is balanced
(each block of 4 consecutive IDs uses every letter once) and wrong answers are shuffled deterministically.
"""

import difflib
import json
import random
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_DIR = ROOT / "tools" / "questions_src"
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "questions"

AGES = ["10-12", "13-14", "15-18", "19-60"]
CATEGORIES = {
    "geography", "history_ua", "history_world", "nature", "physics", "chemistry", "math",
    "literature", "language", "art", "music", "cinema", "sport", "technology", "food",
    "culture", "health",
}
LETTERS = ["A", "B", "C", "D"]
MIN_PER_CELL = 50


def normalize(s: str) -> str:
    return re.sub(r"[\W_]+", "", s.lower())


def parse_sources():
    errors = []
    items = []
    for path in sorted(SRC_DIR.glob("*.txt")):
        for line_no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            where = f"{path.name}:{line_no}"
            parts = [p.strip() for p in line.split("|")]
            if len(parts) != 7:
                errors.append(f"{where}: expected 7 fields, got {len(parts)}")
                continue
            category, ratings_raw, text, correct, *wrong = parts
            ratings = []
            for r in ratings_raw.split(","):
                age, _, diff = r.strip().partition(":")
                if age not in AGES or not diff.isdigit() or not 1 <= int(diff) <= 5:
                    errors.append(f"{where}: bad rating '{r}'")
                    continue
                ratings.append({"age": age, "difficulty": int(diff)})
            items.append({"where": where, "category": category, "ratings": ratings,
                          "text": text, "correct": correct, "wrong": wrong})
    return items, errors


def validate(items, errors):
    seen_texts = {}
    for it in items:
        w = it["where"]
        if it["category"] not in CATEGORIES:
            errors.append(f"{w}: unknown category '{it['category']}'")
        ages = [r["age"] for r in it["ratings"]]
        if not 1 <= len(ages) <= 4 or len(set(ages)) != len(ages):
            errors.append(f"{w}: ratings must have 1..4 unique ages")
        text = it["text"]
        if not 10 <= len(text) <= 200:
            errors.append(f"{w}: question length {len(text)} out of 10..200")
        if not text.endswith(("?", ":")):
            errors.append(f"{w}: question must end with '?' or ':'")
        options = [it["correct"], *it["wrong"]]
        for o in options:
            if not 1 <= len(o) <= 60:
                errors.append(f"{w}: option length {len(o)} out of 1..60: '{o}'")
        if len({re.sub(r"\s+", " ", o.lower()).strip() for o in options}) != 4:
            errors.append(f"{w}: options are not distinct")
        key = normalize(text)
        if key in seen_texts:
            errors.append(f"{w}: duplicate question text (first at {seen_texts[key]})")
        seen_texts.setdefault(key, w)


def similar_pairs(items, threshold=0.85):
    norm = [normalize(it["text"]) for it in items]
    pairs = []
    for i in range(len(items)):
        for j in range(i + 1, len(items)):
            if normalize(items[i]["correct"]) != normalize(items[j]["correct"]):
                continue
            sm = difflib.SequenceMatcher(None, norm[i], norm[j])
            if sm.real_quick_ratio() >= threshold and sm.quick_ratio() >= threshold and sm.ratio() >= threshold:
                pairs.append((items[i]["where"], items[j]["where"], items[i]["text"], items[j]["text"]))
    return pairs


def build(items):
    questions, answers = [], []
    letter_perm = []
    for idx, it in enumerate(items):
        qid = idx + 1
        if idx % 4 == 0:
            letter_perm = LETTERS[:]
            random.Random(1000 + idx // 4).shuffle(letter_perm)
        correct_letter = letter_perm[idx % 4]
        wrong = it["wrong"][:]
        random.Random(qid).shuffle(wrong)
        options, wi = {}, 0
        for letter in LETTERS:
            if letter == correct_letter:
                options[letter] = it["correct"]
            else:
                options[letter] = wrong[wi]
                wi += 1
        questions.append({"id": qid, "text": it["text"], "options": options,
                          "category": it["category"], "ratings": it["ratings"]})
        answers.append({"id": qid, "correct": correct_letter})
    return questions, answers


def report(items):
    cells = defaultdict(int)
    cats_by_age = defaultdict(Counter)
    for it in items:
        for r in it["ratings"]:
            cells[(r["age"], r["difficulty"])] += 1
            cats_by_age[r["age"]][it["category"]] += 1
    print(f"Total questions: {len(items)}")
    print("Coverage (age x difficulty):")
    print("        " + "".join(f"{d:>6}" for d in range(1, 6)))
    low = []
    for age in AGES:
        row = [cells[(age, d)] for d in range(1, 6)]
        print(f"{age:>7} " + "".join(f"{c:>6}" for c in row))
        low += [(age, d + 1, c) for d, c in enumerate(row) if c < MIN_PER_CELL]
    for age in AGES:
        total = sum(cats_by_age[age].values())
        top_cat, top_n = cats_by_age[age].most_common(1)[0]
        share = top_n / total * 100
        flag = "  <-- over 20%" if share > 20 else ""
        print(f"{age}: top category {top_cat} {share:.1f}%{flag}")
    return low


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    items, errors = parse_sources()
    validate(items, errors)
    low = report(items)
    for age, d, c in low:
        errors.append(f"cell {age} x {d} has {c} questions (< {MIN_PER_CELL})")
    pairs = similar_pairs(items)
    for a, b, ta, tb in pairs:
        print(f"WARNING similar: {a} ~ {b}\n   {ta}\n   {tb}")
    if errors:
        print("\nERRORS:")
        for e in errors:
            print("  " + e)
        sys.exit(1)
    questions, answers = build(items)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "questions.json").write_text(
        json.dumps({"schemaVersion": 1, "questions": questions}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")
    (OUT_DIR / "answers.json").write_text(
        json.dumps({"schemaVersion": 1, "answers": answers}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")
    print(f"\nOK: wrote {len(questions)} questions to {OUT_DIR}")
    print("Correct letter distribution:", dict(Counter(a["correct"] for a in answers)))


if __name__ == "__main__":
    main()
