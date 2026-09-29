"""MBC 라디오 선곡표 전체 회차를 크롤링해 CSV로 저장한다.

- 실행할 때마다 선곡표 목록 페이지에서 각 라디오의 최신 회차(maxSeqId)를 다시 읽는다.
- 파싱 규칙은 MbcRadioCrawler와 같다(코너 구분 행·테마송 제외, 백틱 치환, 회차 내 중복 제거).
- 진행 상황을 <out-dir>/state.json에 기록한다. 네트워크 에러가 나거나 중단되면
  같은 명령을 다시 실행해 멈춘 회차부터 이어서 크롤링한다.
- 재시도해도 HTTP 에러(500 등)를 내는 회차는 페이지 자체가 깨진 것이라 state.json의
  failed에 기록하고 넘어간다.

usage:
  python3 scripts/radio/crawl.py                  # 처음 실행 또는 이어서 실행
  python3 scripts/radio/crawl.py --skip-errors    # 네트워크 에러도 기록만 하고 넘어감
  python3 scripts/radio/crawl.py --reset          # 기록을 지우고 처음부터
"""
import argparse
import csv
import html
import io
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

LIST_URL = "https://miniweb.imbc.com/Music?progCode={code}"
VIEW_URL = "https://miniweb.imbc.com/Music/View?seqID={seq}&progCode={code}"

# (enum 이름, 라디오 이름, progCode, minSeqId, 테마송 목록) — MbcRadio enum과 맞춘다
RADIOS = [
    ("MUSIC_PARTY", "정오의 희망곡 김신영입니다", "FM4U000001226", 3000,
     ["Over The Sea", "So What's New?", "봄바람", "Kids", "Freedom At Midnight"]),
    ("BRUNCH_CAFE", "이석훈의 브런치카페", "FM4U000001334", 1, ["구름 위를 걷다"]),
    ("STARNIGHT", "김이나의 별이 빛나는 밤에", "RASFM210", 2000, ["가을의 기도"]),
]
HEADER = ["radio", "radio_name", "seq", "date", "title", "artist"]

LATEST_SEQ = re.compile(r"seqID=(\d+)")
TBODY = re.compile(r"<tbody>(.*?)</tbody>", re.S)
TR = re.compile(r"<tr[^>]*>(.*?)</tr>", re.S)
TD = re.compile(r"<td[^>]*>(.*?)</td>", re.S)
TAG = re.compile(r"<[^>]+>")
DATE = re.compile(r"\d{4}-\d{2}-\d{2}")


def text(cell):
    return " ".join(html.unescape(TAG.sub(" ", cell)).split())


def fetch(url, retries=4):
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=15) as res:
                return res.read().decode("utf-8", "replace")
        except Exception:
            if attempt == retries - 1:
                raise
            time.sleep(2 ** attempt)


def latest_seq(code):
    seqs = [int(s) for s in LATEST_SEQ.findall(fetch(LIST_URL.format(code=code)))]
    if not seqs:
        raise RuntimeError(f"{code} 목록 페이지에서 회차를 찾지 못함")
    return max(seqs)


def crawl(radio, seq):
    """(seq, rows, error, fatal) 반환. 에러는 예외 대신 문자열로 돌려준다.

    HTTP 에러는 그 회차 페이지 문제라 건너뛰고(fatal=False),
    그 밖의 에러(타임아웃, 연결 끊김 등)는 멈추고 이어서 할 대상이다(fatal=True).
    """
    enum_name, radio_name, code, _, themes = radio
    try:
        page = fetch(VIEW_URL.format(seq=seq, code=code))
    except urllib.error.HTTPError as e:
        return seq, [], f"HTTP {e.code}", False
    except Exception as e:
        return seq, [], f"{type(e).__name__}: {e}", True

    date_match = DATE.search(page)
    date = date_match.group(0) if date_match else ""
    body = TBODY.search(page)
    rows, seen = [], set()
    for tr in TR.findall(body.group(1)) if body else []:
        cells = TD.findall(tr)
        if len(cells) <= 1:  # <오프닝>, <초대석> 등 코너 구분 행
            continue
        title = text(cells[1]).replace("`", "'")
        artist = text(cells[2]) if len(cells) > 2 else ""
        if not title or any(t.lower() in title.lower() for t in themes):
            continue
        if (title, artist) in seen:
            continue
        seen.add((title, artist))
        rows.append([enum_name, radio_name, seq, date, title, artist])
    return seq, rows, None, False


class Checkpoint:
    """CSV와 state.json을 함께 관리한다.

    state.json의 csv_offset은 마지막으로 완료된 회차까지 쓴 CSV 바이트 위치다.
    다시 시작할 때 CSV를 이 위치로 잘라 내므로, 기록 도중 죽어도 중복·깨진 행이 남지 않는다.
    """

    def __init__(self, out_dir, reset):
        os.makedirs(out_dir, exist_ok=True)
        self.csv_path = os.path.join(out_dir, "radio_songs.csv")
        self.state_path = os.path.join(out_dir, "state.json")
        if reset:
            for p in (self.csv_path, self.state_path):
                if os.path.exists(p):
                    os.remove(p)

        if os.path.exists(self.state_path):
            with open(self.state_path, encoding="utf-8") as f:
                self.state = json.load(f)
            self.csv = open(self.csv_path, "r+b")
            self.csv.truncate(self.state["csv_offset"])
            self.csv.seek(self.state["csv_offset"])
        else:
            self.state = {"radios": {}, "failed": [], "csv_offset": 0}
            self.csv = open(self.csv_path, "wb")
            self._write([HEADER], bom=True)
            self.save()

    def radio(self, enum_name, min_seq):
        return self.state["radios"].setdefault(enum_name, {"next_seq": min_seq})

    def _write(self, rows, bom=False):
        buf = io.StringIO()
        csv.writer(buf, lineterminator="\n").writerows(rows)
        self.csv.write(buf.getvalue().encode("utf-8-sig" if bom else "utf-8"))
        self.csv.flush()
        os.fsync(self.csv.fileno())

    def commit(self, rows, radio_state, next_seq):
        """회차 묶음을 CSV에 쓰고 나서 state를 갱신한다(순서가 중요)."""
        self._write(rows)
        radio_state["next_seq"] = next_seq
        self.save()

    def save(self):
        self.state["csv_offset"] = self.csv.tell()
        self.state["updated_at"] = datetime.now().isoformat(timespec="seconds")
        tmp = self.state_path + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(self.state, f, ensure_ascii=False, indent=2)
        os.replace(tmp, self.state_path)  # 원자적 교체


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out-dir", default="crawl_output")
    parser.add_argument("--workers", type=int, default=8)
    parser.add_argument("--skip-errors", action="store_true",
                        help="네트워크 에러가 난 회차도 state.json의 failed에 기록하고 계속 진행")
    parser.add_argument("--reset", action="store_true", help="기존 결과와 기록을 지우고 처음부터")
    args = parser.parse_args()

    cp = Checkpoint(args.out_dir, args.reset)
    chunk = args.workers * 5

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        for radio in RADIOS:
            enum_name, radio_name, code, min_seq, _ = radio
            rs = cp.radio(enum_name, min_seq)
            rs["max_seq"] = max(rs.get("max_seq", 0), latest_seq(code))
            cp.save()
            max_seq = rs["max_seq"]
            print(f"[{enum_name}] {radio_name}: {rs['next_seq']} ~ {max_seq}", flush=True)

            while rs["next_seq"] <= max_seq:
                start = rs["next_seq"]
                seqs = range(start, min(start + chunk, max_seq + 1))
                rows, next_seq = [], start
                for seq, songs, err, fatal in pool.map(lambda s: crawl(radio, s), seqs):
                    if err:
                        if fatal and not args.skip_errors:
                            cp.commit(rows, rs, next_seq)
                            print(f"\n[{enum_name}] seq={seq} 실패: {err}", file=sys.stderr)
                            print(f"seq={seq}부터 다시 시작하려면 같은 명령을 다시 실행하세요. "
                                  f"(계속 실패하면 --skip-errors)", file=sys.stderr)
                            sys.exit(1)
                        cp.state["failed"].append({"radio": enum_name, "seq": seq, "error": err})
                    rows.extend(songs)
                    next_seq = seq + 1
                cp.commit(rows, rs, next_seq)
                print(f"  [{enum_name}] {next_seq - 1}/{max_seq} (+{len(rows)}곡)", flush=True)

    failed = cp.state["failed"]
    print(f"완료 -> {cp.csv_path}" + (f" (실패 회차 {len(failed)}건: state.json 참고)" if failed else ""))


if __name__ == "__main__":
    main()
