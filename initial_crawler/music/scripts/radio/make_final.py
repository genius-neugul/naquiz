"""radio_songs_unique.csv → radio_songs_final.json

docs/MUSIC_PARSING_RULE.md 1장의 제목·가수 정제 규칙을 적용한다.
- 아티스트가 없는 곡은 지우고 radio_songs_removed.csv에 남긴다.
- 규칙으로 처리하지 못하는 곡은 radio_songs_unresolved.csv에 이유와 함께 따로 모은다.

usage: python3 scripts/radio/make_final.py [crawl_output 디렉터리]
"""
import csv
import json
import os
import re
import sys

import removed_log
from rules import ArtistIndex, normalize, parse_artist_fields, parse_title_fields, split_top

OUT_DIR = sys.argv[1] if len(sys.argv) > 1 else "crawl_output"
SRC = os.path.join(OUT_DIR, "radio_songs_unique.csv")
FINAL = os.path.join(OUT_DIR, "radio_songs_final.json")
UNRESOLVED = os.path.join(OUT_DIR, "radio_songs_unresolved.csv")
REMOVED = os.path.join(OUT_DIR, "radio_songs_removed.csv")

# 처리 못한 곡(CSV)의 컬럼. 사람이 표 형태로 검토하기 쉽게 가수는 ', '로 잇는다
UNRESOLVED_FIELDS = ["radio", "radio_name", "seq", "date", "title", "subtitle", "artist", "artist_sub",
                     "raw_title", "raw_artist", "reason"]
NO_ARTIST = "아티스트 없음"

# 처리하지 못하는 곡의 이유 (docs/MUSIC_PARSING_RULE.md 1-11과 같은 이름)
MULTI_SONG = "여러 곡을 이어 붙인 제목"
MEDLEY = "메들리"
NO_TITLE = "제목 없음"
SLASH_ARTIST = "슬래시(/)로 이어진 아티스트"
DASH_ARTIST = "대시(-)로 이어진 아티스트"
CLASSICAL = "클래식 작품명"
FOREIGN_ONLY = "한글·영문이 없는 제목"

COMPOSERS = ("Bach|Mozart|Beethoven|Chopin|Schubert|Vivaldi|Tchaikovsky|Ravel|Debussy|Puccini|Verdi|Handel|"
             "Haydn|Brahms|Liszt|Mendelssohn|Satie|Rachmaninoff|Rachmaninov|Dvorak|Dvořák|Elgar|Grieg|Pachelbel|"
             "Schumann|Mahler|Glass|Richter|Bizet|Rossini|Mascagni|Saint-Saens|Saint-Saëns|Massenet|Offenbach|"
             "Paganini|Strauss|Sibelius|Faure|Fauré|Gounod|Albinoni|Boccherini|Borodin|Rimsky-Korsakov|Smetana|"
             "Prokofiev|Shostakovich|Stravinsky|Gershwin|Barber|Piazzolla|Morricone|바흐|모차르트|베토벤|쇼팽|슈베르트|"
             "비발디|차이콥스키|라벨|드뷔시|푸치니|베르디|헨델|하이든|브람스|리스트|멘델스존|사티|라흐마니노프|드보르작|엘가")
CLASSICAL_RE = re.compile(rf"^\s*({COMPOSERS})\s*:|\b(Op\.|BWV|RV\.?\s?\d|K\.\s?\d{{2,}}|Hob\.)", re.I)
MULTI_SONG_RE = re.compile(r"[^\W\d_][^/]*\s*/\s*[^\W\d_]")
FOREIGN_RE = re.compile(r"[぀-ヿ一-鿿Ѐ-ӿ]")


def unresolved_reasons(raw_title, raw_artist, title, subtitle):
    reasons = []
    t = normalize(raw_title)
    if t.startswith("*"):
        reasons.append(MEDLEY)
    elif title is None:
        reasons.append(NO_TITLE)
    elif MULTI_SONG_RE.search(title):
        reasons.append(MULTI_SONG)
    if CLASSICAL_RE.search(t):
        reasons.append(CLASSICAL)
    if title and not re.search(r"[가-힣A-Za-z]", title) and FOREIGN_RE.search(title):
        reasons.append(FOREIGN_ONLY)
    a = re.sub(r"^OST\s*/\s*", "", normalize(raw_artist), flags=re.I)
    slash = split_top(a, re.compile(r"\s+/\s+"))
    if len(slash) > 1 and not re.search(r":\s*[A-Za-z]", a):     # 역할 표기 없는 '그룹 / 멤버' 또는 협업
        reasons.append(SLASH_ARTIST)
    if " - " in a:
        reasons.append(DASH_ARTIST)
    return reasons


def main():
    rows = list(csv.DictReader(open(SRC, encoding="utf-8-sig")))
    index = ArtistIndex(r["artist"] for r in rows)
    parsed = [(r, parse_title_fields(r["title"]), parse_artist_fields(r["artist"], index)) for r in rows]

    final, unresolved, removed = [], [], []
    for r, (title, subtitle, _), (artists, subs) in parsed:
        if not artists:
            removed.append((NO_ARTIST, r))
            continue
        song = {"radio": r["radio"], "radio_name": r["radio_name"], "seq": int(r["seq"]), "date": r["date"],
                "title": title if title is not None else normalize(r["title"]), "subtitle": subtitle or "",
                # 가수 여러 명은 배열로 담는다. 이름 안의 쉼표(Earth, Wind & Fire)와 구분할 필요가 없다
                "artists": [{"artist": a, "artist_sub": s} for a, s in zip(artists, subs)],
                "raw_title": r["title"], "raw_artist": r["artist"]}
        reasons = unresolved_reasons(r["title"], r["artist"], title, subtitle)
        if reasons:
            unresolved.append({**{k: v for k, v in song.items() if k != "artists"},
                               "artist": ", ".join(artists), "artist_sub": ", ".join(subs),
                               "reason": "; ".join(reasons)})
        else:
            final.append(song)

    with open(FINAL, "w", encoding="utf-8") as f:
        json.dump(final, f, ensure_ascii=False, indent=2)
        f.write("\n")
    with open(UNRESOLVED, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=UNRESOLVED_FIELDS)
        w.writeheader()
        w.writerows(unresolved)
    counts = removed_log.write(REMOVED, "final", removed)

    by_reason = {}
    for u in unresolved:
        for reason in u["reason"].split("; "):
            by_reason[reason] = by_reason.get(reason, 0) + 1
    print(f"unique {len(rows)}곡 → final {len(final)}곡, 처리 못함 {len(unresolved)}곡 {by_reason}, "
          f"삭제 {dict(counts)}")


if __name__ == "__main__":
    main()
