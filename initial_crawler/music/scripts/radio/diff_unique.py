"""이전 radio_songs_unique.csv와 비교해 새로 추가된 곡만 radio_songs_added.csv로 뽑는다.

같은 곡 판정은 make_unique.py와 같다: 제목 키가 같고 가수 키가 하나라도 겹치면 같은 곡이다.
회차가 늘면 대표 표기가 바뀔 수 있으므로 원문 (제목, 아티스트)로 비교하지 않는다.

usage: python3 scripts/radio/diff_unique.py <이전 radio_songs_unique.csv> [crawl_output 디렉터리]
"""
import csv
import os
import sys

from make_unique import classify, key
from rules import parse_artist

OLD = sys.argv[1]
OUT_DIR = sys.argv[2] if len(sys.argv) > 2 else "crawl_output"
NEW = os.path.join(OUT_DIR, "radio_songs_unique.csv")
DST = os.path.join(OUT_DIR, "radio_songs_added.csv")


def nodes(row):
    tk, _, _ = classify(row["title"])
    names = [key(n) for n in parse_artist(row["artist"])]
    return {(tk, n) for n in names if n} or {(tk, "-")}


def read(path):
    return list(csv.DictReader(open(path, encoding="utf-8-sig")))


def main():
    old_rows, new_rows = read(OLD), read(NEW)
    old = set().union(*(nodes(r) for r in old_rows))
    added = [r for r in new_rows if not nodes(r) & old]
    with open(DST, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=list(new_rows[0].keys()))
        w.writeheader()
        w.writerows(added)

    by_radio = {}
    for r in added:
        by_radio[r["radio"]] = by_radio.get(r["radio"], 0) + 1
    print(f"이전 {len(old_rows)}곡, 새 {len(new_rows)}곡 → 새로 추가 {len(added)}곡 {by_radio} ({DST})")


if __name__ == "__main__":
    main()
