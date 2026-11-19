"""Builds questions.json and answers.json from tools/questions_src/*.txt + tools/answers.txt and validates the bank.

Question line format (one question per line, '#' starts a comment line):
    id|category|age:difficulty[,age:difficulty...]|question text|A|B|C|D
Answer line format (tools/answers.txt):
    id|letter

IDs are permanent: the app stores them to track already asked questions, so an ID must never
change or be reused. When deleting a question, add its ID to tools/retired_ids.txt.

To add questions, write lines without id with the correct answer first:
    category|ratings|question text|correct answer|wrong 1|wrong 2|wrong 3
and run `python tools/build_questions.py --assign-ids`. It assigns new IDs (max used + 1), arranges
the options as A..D (correct letter balanced in blocks of 4 IDs) and writes the letters to answers.txt.
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
RETIRED_FILE = ROOT / "tools" / "retired_ids.txt"
ANSWERS_FILE = ROOT / "tools" / "answers.txt"

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


def load_retired():
    if not RETIRED_FILE.exists():
        return set()
    lines = RETIRED_FILE.read_text(encoding="utf-8").splitlines()
    return {int(s) for s in (l.split("#")[0].strip() for l in lines) if s}


def load_answers(errors):
    answers = {}
    if not ANSWERS_FILE.exists():
        return answers
    for line_no, raw in enumerate(ANSWERS_FILE.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        qid, _, letter = (p.strip() for p in line.partition("|"))
        if not qid.isdigit() or letter not in LETTERS:
            errors.append(f"{ANSWERS_FILE.name}:{line_no}: bad answer line '{line}'")
        elif int(qid) in answers:
            errors.append(f"{ANSWERS_FILE.name}:{line_no}: duplicate answer for id {qid}")
        else:
            answers[int(qid)] = letter
    return answers


def write_answers(answers):
    lines = ["# Correct answer letter for each question: id|letter"]
    lines += [f"{qid}|{answers[qid]}" for qid in sorted(answers)]
    ANSWERS_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")


def arrange_options(qid, correct, wrong):
    """Returns options in A..D order and the correct letter."""
    letters = LETTERS[:]
    random.Random(1000 + (qid - 1) // 4).shuffle(letters)
    correct_letter = letters[(qid - 1) % 4]
    wrong = wrong[:]
    random.Random(qid).shuffle(wrong)
    rest = iter(wrong)
    return [correct if l == correct_letter else next(rest) for l in LETTERS], correct_letter


def assign_ids(retired, answers):
    """Converts lines without id (correct answer first) to the id|...|A|B|C|D format; returns the count."""
    files = {p: p.read_text(encoding="utf-8").splitlines() for p in sorted(SRC_DIR.glob("*.txt"))}
    used = set(retired) | answers.keys()
    for lines in files.values():
        for l in lines:
            first = l.strip().split("|")[0].strip()
            if not l.strip().startswith("#") and first.isdigit():
                used.add(int(first))
    next_id = max(used, default=0) + 1
    assigned = 0
    for path, lines in files.items():
        changed = False
        for i, l in enumerate(lines):
            s = l.strip()
            parts = [p.strip() for p in s.split("|")]
            if s and not s.startswith("#") and len(parts) == 7:
                options, letter = arrange_options(next_id, parts[3], parts[4:])
                lines[i] = "|".join([str(next_id), *parts[:3], *options])
                answers[next_id] = letter
                next_id += 1
                assigned += 1
                changed = True
        if changed:
            path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    if assigned:
        write_answers(answers)
    return assigned


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
            if len(parts) == 7:
                errors.append(f"{where}: missing id (run with --assign-ids)")
                continue
            if len(parts) != 8:
                errors.append(f"{where}: expected 8 fields, got {len(parts)}")
                continue
            if not parts[0].isdigit() or int(parts[0]) < 1:
                errors.append(f"{where}: bad id '{parts[0]}'")
                continue
            qid, category, ratings_raw, text, *options = parts
            ratings = []
            for r in ratings_raw.split(","):
                age, _, diff = r.strip().partition(":")
                if age not in AGES or not diff.isdigit() or not 1 <= int(diff) <= 5:
                    errors.append(f"{where}: bad rating '{r}'")
                    continue
                ratings.append({"age": age, "difficulty": int(diff)})
            items.append({"id": int(qid), "where": where, "category": category, "ratings": ratings,
                          "text": text, "options": options})
    return items, errors


def validate(items, errors, retired, answers):
    seen_texts = {}
    seen_ids = {}
    for it in items:
        w = it["where"]
        letter = answers.get(it["id"])
        if letter is None:
            errors.append(f"{w}: no answer for id {it['id']} in {ANSWERS_FILE.name}")
            it["correct"] = ""
        else:
            it["correct"] = it["options"][LETTERS.index(letter)]
        if it["id"] in seen_ids:
            errors.append(f"{w}: duplicate id {it['id']} (first at {seen_ids[it['id']]})")
        seen_ids.setdefault(it["id"], w)
        if it["id"] in retired:
            errors.append(f"{w}: id {it['id']} is retired and must not be reused")
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
        options = it["options"]
        for o in options:
            if not 1 <= len(o) <= 60:
                errors.append(f"{w}: option length {len(o)} out of 1..60: '{o}'")
        if len({re.sub(r"\s+", " ", o.lower()).strip() for o in options}) != 4:
            errors.append(f"{w}: options are not distinct")
        key = normalize(text)
        if key in seen_texts:
            errors.append(f"{w}: duplicate question text (first at {seen_texts[key]})")
        seen_texts.setdefault(key, w)
    for qid in sorted(answers.keys() - seen_ids.keys()):
        errors.append(f"{ANSWERS_FILE.name}: answer for unknown id {qid}")
    prev_file = OUT_DIR / "questions.json"
    if prev_file.exists():
        prev_ids = {q["id"] for q in json.loads(prev_file.read_text(encoding="utf-8"))["questions"]}
        for qid in sorted(prev_ids - seen_ids.keys() - retired):
            errors.append(f"id {qid} was removed: add it to {RETIRED_FILE.name}")


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


def build(items, answer_map):
    questions, answers = [], []
    for it in sorted(items, key=lambda x: x["id"]):
        qid = it["id"]
        questions.append({"id": qid, "text": it["text"], "options": dict(zip(LETTERS, it["options"])),
                          "category": it["category"], "ratings": it["ratings"]})
        answers.append({"id": qid, "correct": answer_map[qid]})
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
    retired = load_retired()
    answer_errors = []
    answer_map = load_answers(answer_errors)
    if "--assign-ids" in sys.argv and not answer_errors:
        print(f"Assigned {assign_ids(retired, answer_map)} new IDs")
    items, errors = parse_sources()
    errors = answer_errors + errors
    validate(items, errors, retired, answer_map)
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
    questions, answers = build(items, answer_map)
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
