"""radio_songs.csv → radio_songs_unique.csv

- 가사 없는 곡(Inst·MR, 연주 시그널·BGM)을 지운다. 지운 곡은 radio_songs_removed.csv에 남긴다.
- 같은 곡은 가장 많이 나온 표기 한 행만 남긴다. 합친 방송 수는 play_count로 남긴다.
- crawl.py의 RADIOS에 있는 라디오의 행만 쓴다.

usage: python3 scripts/radio/make_unique.py [crawl_output 디렉터리]
"""
import collections
import csv
import html
import os
import re
import sys

import removed_log
from crawl import RADIOS
from rules import SIGNAL, normalize, parse_artist, parse_title

OUT_DIR = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("-") else "crawl_output"
SRC = os.path.join(OUT_DIR, "radio_songs.csv")
DST = os.path.join(OUT_DIR, "radio_songs_unique.csv")
REMOVED = os.path.join(OUT_DIR, "radio_songs_removed.csv")

INST = "반주·연주 버전(Inst/MR)"
SIGNAL_INST = "연주 시그널·BGM"
SIGNAL_SAME = "연주 시그널·BGM(표기 없이 나온 같은 곡)"

# 가사가 있는 곡에 붙은 표기 → 괄호만 떼고 유지
SIGNAL_GROUP = re.compile(r"\s*\([^()]*(타이틀 뮤직|BGM|시그널 뮤직)[^()]*\)?")
VOCAL_TAG = re.compile(r"\s*\([^()]*(퀴즈용|시그널 송)[^()]*\)")


def key(s):
    """대소문자·공백·문장부호를 무시한 비교 키. 한자·가나·키릴 문자도 남기고, 기호만 있으면(%%) 원문을 쓴다."""
    return re.sub(r"[\W_]+", "", s.lower()) or s.lower().strip()


def classify(title):
    """(제목 키, 제거 사유, 출력용 제목 보정 여부)"""
    t, _, why = parse_title(title)
    if why is None:
        return key(t), None, False
    if why == "Inst/MR":
        return None, INST, False
    if why == "시그널/BGM 표기":
        if VOCAL_TAG.search(normalize(title)):
            t2, _, why2 = parse_title(VOCAL_TAG.sub("", normalize(title)))
            if why2 is None:
                return key(t2), None, True
            if why2 == "Inst/MR":
                return None, INST, False
        return None, SIGNAL_INST, False
    return key(normalize(title)) or title, None, False     # 메들리, 빈 제목: 원문 그대로


def main():
    radios = [r[0] for r in RADIOS]
    rows = [r for r in csv.DictReader(open(SRC, encoding="utf-8-sig")) if r["radio"] in radios]
    for r in rows:  # 원본에 &#39; 같은 엔티티가 남은 행이 있다
        r["title"], r["artist"] = html.unescape(r["title"]), html.unescape(r["artist"])
    parent = {}

    def find(x):
        parent.setdefault(x, x)
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def artist_nodes(tk, artist):
        names = [key(n) for n in parse_artist(artist)]
        return [(tk, n) for n in names if n] or [(tk, "-")]

    removed = []  # (reason, row)
    signal_nodes = set()  # 연주 시그널로 제거한 곡: 표기 없이 나온 같은 곡도 제거한다
    kept = []  # (row, node, tagged)
    for r in rows:
        tk, why, tagged = classify(r["title"])
        if why:
            removed.append((why, r))
            if why == SIGNAL_INST:
                t = re.sub(r"\s*\([^()]*\)?$", "", SIGNAL_GROUP.sub("", normalize(r["title"])))
                signal_nodes.update(artist_nodes(key(parse_title(t)[0] or t), r["artist"]))
            continue
        nodes = artist_nodes(tk, r["artist"])
        for n in nodes[1:]:
            parent[find(n)] = find(nodes[0])
        kept.append((r, nodes[0], tagged))

    groups = collections.defaultdict(list)
    for r, node, tagged in kept:
        groups[find(node)].append((r, tagged))

    out = []
    for root, members in groups.items():
        if any(find(n) == root for n in signal_nodes if n in parent):
            removed += [(SIGNAL_SAME, r) for r, _ in members]
            continue
        # 표기 태그(퀴즈용 등)가 없는 표기를 우선, 그다음 횟수, 그다음 최근 방송
        stat = {}
        for r, tagged in members:
            k = (r["title"], r["artist"])
            cnt, last, tg = stat.get(k, (0, ("", 0), tagged))
            stat[k] = (cnt + 1, max(last, (r["date"], int(r["seq"]))), tg)
        best = max(stat, key=lambda k: (not stat[k][2], stat[k][0], stat[k][1]))
        row = max((r for r, _ in members if (r["title"], r["artist"]) == best),
                  key=lambda r: (r["date"], int(r["seq"])))
        row = dict(row)
        row["play_count"] = len(members)  # crawl.py가 회차 안 같은 (제목, 아티스트)를 지우므로 행 수를 방송 수로 쓴다
        if stat[best][2]:
            row["title"] = VOCAL_TAG.sub("", row["title"]).strip()
        out.append(row)

    out.sort(key=lambda r: (radios.index(r["radio"]), int(r["seq"]), r["title"]))
    with open(DST, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=["radio", "radio_name", "seq", "date", "title", "artist", "play_count"])
        w.writeheader()
        w.writerows(out)

    counts = removed_log.write(REMOVED, "unique", removed)
    print(f"원본 {len(rows)}행, 제거 {dict(counts)} → 곡 {len(out)} ({DST})")
    multi = sorted((m for m in groups.values() if len({(r['title'], r['artist']) for r, _ in m}) > 1),
                   key=lambda m: -len({(r['title'], r['artist']) for r, _ in m}))
    print(f"표기가 2가지 이상 합쳐진 곡 {len(multi)}")
    if "-v" in sys.argv:
        import random
        random.seed(5)
        show = multi[:20] + random.sample(multi[20:], 20)
        for m in show:
            forms = collections.Counter((r["title"], r["artist"]) for r, _ in m)
            print("  ", [f"{t} | {a} ({c})" for (t, a), c in forms.most_common(6)])


if __name__ == "__main__":
    main()
