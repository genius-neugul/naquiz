"""
[3단계] movies.json 의 포스터·스틸컷 URL을 로컬로 내려받기 (개발/오프라인 테스트용)
  입력: ../data/movies.json
  저장: images/posters/{movieCd}/{순번:02d}.{확장자}   (01 = 대표 포스터)
        images/stills/{movieCd}/{순번:02d}.{확장자}
  목록: images/posters/manifest.json, images/stills/manifest.json
        {movieCd: [{url, path, bytes}, ...]}
  로그: logs/download_images.log

  실행: python 3_download_images.py                    # 포스터 + 스틸컷
        python 3_download_images.py --kind posters     # 포스터만
        python 3_download_images.py --kind stills      # 스틸컷만
        python 3_download_images.py --size 640         # 원본 대신 640px 썸네일 (용량 ↓)
        python 3_download_images.py --max-per-movie 5  # 영화당 종류별 최대 5장

이어하기 동작
  - 이미 받은 파일(크기 > 0)은 건너뛴다.
  - 다운로드는 .part 임시 파일에 쓴 뒤 완료 시 이름을 바꾸므로,
    도중에 끊겨도 반쪽짜리 이미지가 남지 않는다.

주의: 포스터·스틸컷은 영화사 홍보용 이미지로 저작권이 있다. 로컬 개발/테스트 용도로만 쓰고
      서비스에서는 원본 URL을 참조할 것.
"""
import argparse
import json
import os
import time
from urllib.parse import urlparse

import requests

from common import IMAGES_DIR, OUTPUT_PATH, UA, save_json, setup_logger

log = setup_logger("download_images")

KINDS = {  # kind → (movies.json 필드, 저장 폴더, 로그용 이름)
    "posters": ("posters", os.path.join(IMAGES_DIR, "posters"), "포스터"),
    "stills": ("stills", os.path.join(IMAGES_DIR, "stills"), "스틸컷"),
}
HEADERS = {"User-Agent": UA, "Referer": "https://www.kobis.or.kr/"}
MAX_RETRY = 3
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".gif", ".webp"}


def to_size_url(url: str, size: str) -> str:
    """원본 URL → KOBIS 썸네일 URL (/.../abc.jpg → /.../thumb_x640/thn_abc.jpg)"""
    if size == "original" or "/" not in url:
        return url
    head, name = url.rsplit("/", 1)
    return f"{head}/thumb_x{size}/thn_{name}"


def ext_of(url: str) -> str:
    ext = os.path.splitext(urlparse(url).path)[1].lower()
    return ext if ext in IMAGE_EXTS else ".jpg"


def download(session: requests.Session, url: str, dest: str) -> int:
    tmp = dest + ".part"
    with session.get(url, headers=HEADERS, timeout=30, stream=True) as r:
        r.raise_for_status()
        ctype = r.headers.get("Content-Type", "")
        if not ctype.startswith("image/"):
            raise ValueError(f"이미지가 아닌 응답 (Content-Type: {ctype or '없음'})")
        with open(tmp, "wb") as f:
            for chunk in r.iter_content(64 * 1024):
                f.write(chunk)
    size = os.path.getsize(tmp)
    if size == 0:
        os.remove(tmp)
        raise ValueError("빈 파일")
    os.replace(tmp, dest)
    return size


def download_with_fallback(session, orig_url: str, size: str, dest: str) -> int:
    """썸네일 요청이 실패하면(오래된 영화는 썸네일이 없을 수 있음) 원본으로 한 번 더 시도"""
    url = to_size_url(orig_url, size)
    try:
        return download(session, url, dest)
    except (requests.HTTPError, ValueError):
        if url == orig_url:
            raise
        return download(session, orig_url, dest)


def load_json(path: str, default):
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    return default


def human(n: float) -> str:
    for unit in ("B", "KB", "MB", "GB"):
        if n < 1024:
            return f"{n:.1f}{unit}"
        n /= 1024
    return f"{n:.1f}TB"


def run_kind(kind: str, movies: list[dict], args, session: requests.Session):
    field, base_dir, label = KINDS[kind]
    manifest_path = os.path.join(base_dir, "manifest.json")
    os.makedirs(base_dir, exist_ok=True)
    manifest = load_json(manifest_path, {})

    if not any(field in m for m in movies):
        log.warning("[%s] 입력 파일에 '%s' 필드가 없습니다. 2_crawl_details.py 결과 파일인지 확인하세요.", label, field)
        return

    total = sum(len((m.get(field) or [])[: args.max_per_movie]) for m in movies)
    log.info("[%s] 시작: 영화 %d편, 이미지 %d장 (size=%s) → %s",
             label, len(movies), total, args.size, base_dir)

    stats = {"new": 0, "skip": 0, "fail": 0, "bytes": 0}
    for i, movie in enumerate(movies, start=1):
        code, title = movie["movieCd"], movie.get("titleKo")
        urls = (movie.get(field) or [])[: args.max_per_movie]
        rank = f"#{movie['rank']} " if movie.get("rank") else ""
        tag = f"[{label} {i}/{len(movies)}] {rank}{title}({code})"
        if not urls:
            log.info("%s %s 없음 — 건너뜀", tag, label)
            continue

        movie_dir = os.path.join(base_dir, code)
        os.makedirs(movie_dir, exist_ok=True)
        entries, new, skip, fail = [], 0, 0, 0
        started = time.time()

        for idx, orig_url in enumerate(urls, start=1):
            dest = os.path.join(movie_dir, f"{idx:02d}{ext_of(orig_url)}")
            if os.path.exists(dest) and os.path.getsize(dest) > 0:
                skip += 1
                entries.append({"url": orig_url, "path": dest, "bytes": os.path.getsize(dest)})
                continue

            for attempt in range(1, MAX_RETRY + 1):
                try:
                    size = download_with_fallback(session, orig_url, args.size, dest)
                    new += 1
                    stats["bytes"] += size
                    entries.append({"url": orig_url, "path": dest, "bytes": size})
                    break
                except (requests.RequestException, ValueError, OSError) as e:
                    if attempt < MAX_RETRY:
                        log.warning("%s %02d번 실패 (%d/%d): %s → 재시도", tag, idx, attempt, MAX_RETRY, e)
                        time.sleep(attempt * 2)
                    else:
                        fail += 1
                        log.error("%s %02d번 최종 실패: %s (%s)", tag, idx, e, orig_url)
            time.sleep(args.delay)

        manifest[code] = entries
        save_json(manifest_path, manifest)  # 영화 1편마다 저장
        stats["new"] += new
        stats["skip"] += skip
        stats["fail"] += fail
        log.info("%s 완료 (%.1fs) — 신규 %d | 기존 %d | 실패 %d / 총 %d장",
                 tag, time.time() - started, new, skip, fail, len(urls))

    log.info("[%s] 전체 완료: 신규 %d장(%s) | 기존 %d장 | 실패 %d장 → %s",
             label, stats["new"], human(stats["bytes"]), stats["skip"], stats["fail"], manifest_path)
    if stats["fail"]:
        log.warning("[%s] 실패한 이미지는 다시 실행하면 재시도합니다.", label)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--kind", choices=["all", "posters", "stills"], default="all")
    ap.add_argument("--size", choices=["original", "640"], default="original",
                    help="original(원본) 또는 640(가로 640px 썸네일)")
    ap.add_argument("--max-per-movie", type=int, default=None, help="영화당 종류별 최대 장수")
    ap.add_argument("--delay", type=float, default=0.3, help="이미지 간 요청 간격(초)")
    ap.add_argument("--movies", default=OUTPUT_PATH, help="입력 파일 (기본 ../data/movies.json)")
    args = ap.parse_args()

    movies = load_json(args.movies, None)
    if movies is None:
        log.error("%s 가 없습니다. 먼저 2_crawl_details.py 를 끝까지 실행하세요.", args.movies)
        raise SystemExit(1)

    session = requests.Session()
    kinds = ["posters", "stills"] if args.kind == "all" else [args.kind]
    try:
        for kind in kinds:
            run_kind(kind, movies, args, session)
    except KeyboardInterrupt:
        log.warning("사용자 중단 — 다시 실행하면 받은 파일은 건너뛰고 이어서 진행합니다.")
        raise SystemExit(130)


if __name__ == "__main__":
    main()
