"""오프닝일 가능성이 높은 곡(표시 없이 회차의 5% 이상에 나오는 곡)을 뽑아 records 파일용 표로 출력한다.

- crawl.py의 RADIOS에 있는 라디오의 행만 쓰고, 가사 없는 곡 제거 규칙(make_unique.py)을 적용한다.
- (정제한 제목, 첫 번째 가수)를 키로 프로그램마다 곡이 나온 회차 수를 센다.
- 그 수를 프로그램의 선곡표가 있는 회차 수로 나눈 값이 5% 이상인 곡을 고른다.

usage: python3 scripts/radio/opening_stats.py [crawl_output 디렉터리]
"""
import collections
import csv
import html
import os
import sys

from crawl import RADIOS
from make_unique import classify, key
from rules import ArtistIndex, normalize, parse_artist_fields, parse_title_fields

OUT_DIR = sys.argv[1] if len(sys.argv) > 1 else "crawl_output"
SRC = os.path.join(OUT_DIR, "radio_songs.csv")
THRESHOLD = 0.05


def main():
    radios = [r[0] for r in RADIOS]
    rows = [r for r in csv.DictReader(open(SRC, encoding="utf-8-sig")) if r["radio"] in radios]
    for r in rows:  # 원본에 &#39; 같은 엔티티가 남은 행이 있다
        r["title"], r["artist"] = html.unescape(r["title"]), html.unescape(r["artist"])
    index = ArtistIndex(r["artist"] for r in rows)

    episodes = collections.defaultdict(set)  # radio -> 선곡표가 있는 회차
    seqs = collections.defaultdict(set)      # (radio, 제목 키, 첫 가수 키) -> 곡이 나온 회차
    names = {}
    for r in rows:
        episodes[r["radio"]].add(int(r["seq"]))
        if classify(r["title"])[1]:
            continue
        title = parse_title_fields(r["title"])[0] or normalize(r["title"])
        artists, _ = parse_artist_fields(r["artist"], index)
        k = (r["radio"], key(title), key(artists[0]) if artists else "")
        seqs[k].add(int(r["seq"]))
        names.setdefault(k, (title, ", ".join(artists)))

    radio_names = {e: n for e, n, *_ in RADIOS}
    found = [(k, s, len(s) / len(episodes[k[0]])) for k, s in seqs.items()]
    found = [f for f in found if f[2] >= THRESHOLD]
    found.sort(key=lambda f: (radios.index(f[0][0]), -f[2]))

    print({radio_names.get(r, r): len(s) for r, s in episodes.items()})
    print("| 프로그램 | 곡 | 아티스트 | 나온 회차 비율 | 회차 범위 |")
    print("|---|---|---|---|---|")
    for k, s, rate in found:
        title, artist = names[k]
        print(f"| {radio_names.get(k[0], k[0])} | {title} | {artist} | {rate * 100:.1f}% ({len(s)}회) "
              f"| {min(s)} ~ {max(s)} |")


if __name__ == "__main__":
    main()
