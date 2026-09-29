"""
[2단계] movie_codes.json 을 읽어 영화별 상세정보를 크롤링 (역대 순위 + titles.txt 추가 영화)
  입력: movie_codes.json                 (1_fetch_codes.py 결과)
  진행: movies.jsonl                     (영화 1편 = 1줄, 완료될 때마다 즉시 append)
  실패: failed.json                      (재시도 후에도 실패한 영화 목록)
  최종: ../data/movies.json              (전편 완료 시 정리한 배열: 역대 순위순 → 추가 영화는 titles.txt 순)
  로그: logs/crawl_details.log

  실행: python 2_crawl_details.py
        python 2_crawl_details.py --delay 1.5   # 요청 간격(초)
        python 2_crawl_details.py --fresh       # 진행 파일 무시하고 처음부터

이어하기 동작
  - 시작 시 movies.jsonl 이 있으면 이미 완료된 movieCd 를 읽어 건너뛴다.
  - 크롤링 도중 강제 종료되어 마지막 줄이 깨졌더라도 그 줄만 버리고 이어서 진행한다.
  - 이전 실행에서 실패한 영화는 완료 목록에 없으므로 다음 실행 때 자동으로 재시도된다.
"""
import argparse
import json
import os
import re
import time

import requests
from bs4 import BeautifulSoup

from common import (ACTOR_URL, BASE, CODES_PATH, DTL_URL, FAILED_PATH, HTML_HEADERS,
                    JSON_HEADERS, OUTPUT_PATH, PROGRESS_PATH, STAFF_URL, save_json, setup_logger)

log = setup_logger("crawl_details")

DIRECTOR_ROLE_CD = "320301"
RATING_IMG = {  # 등급 이미지 파일명 → 등급 (텍스트가 없을 때 대비)
    "rating_all": "전체관람가", "rating_12": "12세이상관람가",
    "rating_15": "15세이상관람가", "rating_18": "청소년관람불가",
    "rating_limit": "제한상영가",
}
MAX_RETRY = 3


# ────────────────────────── 진행 파일 (이어하기) ──────────────────────────
def load_progress() -> dict[str, dict]:
    """진행 파일에서 완료된 영화들을 읽는다. 깨진 줄은 버린다."""
    done: dict[str, dict] = {}
    if not os.path.exists(PROGRESS_PATH):
        return done

    valid_lines, broken = [], 0
    with open(PROGRESS_PATH, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                item = json.loads(line)
                done[item["movieCd"]] = item
                valid_lines.append(line)
            except (json.JSONDecodeError, KeyError):
                broken += 1

    if broken:
        # 깨진 줄(쓰는 도중 종료된 경우)을 제거한 상태로 파일을 다시 쓴다
        log.warning("진행 파일에서 손상된 줄 %d개를 발견해 제거합니다.", broken)
        tmp = PROGRESS_PATH + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            f.write("\n".join(valid_lines) + ("\n" if valid_lines else ""))
        os.replace(tmp, PROGRESS_PATH)
    return done


def append_progress(item: dict):
    with open(PROGRESS_PATH, "a", encoding="utf-8") as f:
        f.write(json.dumps(item, ensure_ascii=False) + "\n")
        f.flush()
        os.fsync(f.fileno())  # 강제 종료돼도 방금 쓴 줄은 디스크에 남도록


# ────────────────────────────── 파싱 ──────────────────────────────
def split_title(title_box) -> tuple[str | None, str | None]:
    """<div><strong class="tit">명량</strong> (The Admiral: Roaring Currents)</div>"""
    if title_box is None:
        return None, None
    strong = title_box.select_one("strong.tit")
    ko = strong.get_text(strip=True) if strong else None
    full = " ".join(title_box.get_text(" ", strip=True).split())
    rest = full[len(ko):].strip() if ko and full.startswith(ko) else full
    en = None
    if rest.startswith("(") and rest.endswith(")"):
        en = rest[1:-1].strip() or None
    elif rest:
        en = rest
    return ko, en


def parse_summary(text: str | None) -> dict | None:
    """'장편 | 일반영화 | 사극, 액션 | 128분 20초 | 15세이상관람가 | 한국 | 한글자막(CC)'"""
    if not text:
        return None
    parts = [p.strip() for p in text.split("|")]
    keys = ["type", "category", "genres", "runtime", "rating", "nation"]
    info = {"raw": " | ".join(parts)}
    for k, v in zip(keys, parts):
        info[k] = v
    if "genres" in info:
        info["genres"] = [g.strip() for g in info["genres"].split(",") if g.strip()]
    if "runtime" in info:
        m = re.match(r"(\d+)분\s*(\d+)?초?", info["runtime"])
        info["runtimeSec"] = int(m.group(1)) * 60 + int(m.group(2) or 0) if m else None
    info["etc"] = parts[len(keys):]  # 자막/화면해설 등 추가 항목
    return info


def parse_detail_html(html: str) -> dict:
    soup = BeautifulSoup(html, "html.parser")

    # dt 라벨 → dd (같은 라벨이 여러 번 나오면 첫 번째)
    dl: dict[str, object] = {}
    for dt in soup.find_all("dt"):
        key = dt.get_text(strip=True)
        if key and key not in dl:
            dl[key] = dt.find_next_sibling("dd")

    def dd_text(key):
        dd = dl.get(key)
        return " ".join(dd.get_text(" ", strip=True).split()) if dd else None

    title_ko, title_en = split_title(soup.select_one("div.hd_layer > div"))

    summary = parse_summary(dd_text("요약정보"))

    # 등급분류: ① 등급분류/기술 정보의 [관람등급] ② 등급 이미지 ③ 요약정보
    rating = None
    for key, dd in dl.items():
        if key.startswith("등급분류/기술") and dd:
            m = re.search(r"\[관람등급\]\s*([^\[\n]+)", dd.get_text("\n"))
            if m:
                rating = m.group(1).strip()
                break
    if not rating and dl.get("등급분류"):
        img = dl["등급분류"].find("img")
        if img and img.get("src"):
            stem = os.path.splitext(os.path.basename(img["src"]))[0]
            rating = RATING_IMG.get(stem, stem)
    if not rating and summary:
        rating = summary.get("rating")

    open_dt = dd_text("개봉일")
    if open_dt in ("", "해당정보없음", "해당정보 없음"):
        open_dt = None

    syn_el = soup.select_one("p.desc_info")
    synopsis = None
    if syn_el:
        lines = [ln.strip() for ln in syn_el.get_text("\n").split("\n")]
        synopsis = re.sub(r"\n{3,}", "\n\n", "\n".join(lines)).strip() or None

    stills = parse_images(soup, "stl")
    posters = parse_images(soup, "post")
    # 포스터 목록이 없을 때 대비: 상단 대표 이미지 링크
    if not posters:
        main = soup.select_one("a.fl.thumb")
        href = main.get("href") if main else None
        if href and href.startswith("/") and "noimage" not in href:
            posters = [BASE + href.strip()]

    return {
        "titleKo": title_ko,
        "titleEn": title_en,
        "summary": summary,
        "rating": rating,
        "openDt": open_dt,
        "synopsis": synopsis,
        "stills": stills,
        "poster": posters[0] if posters else None,  # 대표 포스터 (목록의 첫 번째)
        "posters": posters,
    }


def parse_images(soup: BeautifulSoup, kind: str) -> list[str]:
    """상세 페이지의 이미지 원본 URL 목록.
    kind='stl' 스틸컷, 'post' 포스터 — 각 썸네일 링크의 onclick="fn_photoDtl(..., 'kind')" 로 구분하고
    원본 경로는 img 의 orgsrc 속성에 있다. (예전 영화는 /upload/up_img/... 경로)"""
    urls = []
    imgs = [a.find("img") for a in soup.select(f"a[onclick*=\"'{kind}'\"]")]
    if kind == "stl":  # 기존 방식(div.steelcut)도 함께 확인
        imgs += soup.select("#stl img, div.steelcut img")
    for img in imgs:
        if img is None:
            continue
        src = (img.get("orgsrc") or img.get("src") or "").strip()
        if not src or "noimage" in src:
            continue
        url = BASE + src if src.startswith("/") else src
        if url not in urls:
            urls.append(url)
    return urls


# ────────────────────────────── 요청 ──────────────────────────────
def fetch_json(session: requests.Session, url: str, data: dict):
    r = session.post(url, data=data, headers=JSON_HEADERS, timeout=20)
    r.raise_for_status()
    text = r.text.strip()
    if not text.startswith("["):
        raise ValueError(f"JSON이 아닌 응답: {url} → {text[:60]!r}")
    return json.loads(text)


def crawl_movie(session: requests.Session, meta: dict) -> dict:
    code = meta["movieCd"]

    r = session.post(DTL_URL, headers=HTML_HEADERS, timeout=20, data={
        "code": code, "sType": "", "titleYN": "Y", "etcParam": "", "isOuterReq": "false",
    })
    r.raise_for_status()
    if "요약정보" not in r.text:
        raise ValueError("상세 페이지 형식이 예상과 다릅니다 (요약정보 없음)")
    detail = parse_detail_html(r.text)

    actors_raw = fetch_json(session, ACTOR_URL, {"movieCd": code})
    staff_raw = fetch_json(session, STAFF_URL, {"movieCd": code, "mgmtMore": "N"})

    directors = [
        {"peopleCd": s.get("peopleCd"), "name": s.get("peopleNm"), "nameEn": s.get("peopleNmEn")}
        for s in staff_raw if s.get("roleCd") == DIRECTOR_ROLE_CD
    ]
    actors = [
        {
            "peopleCd": a.get("peopleCd"),
            "name": (a.get("actorNm") or a.get("peopleNm") or "").strip(),
            "nameEn": a.get("peopleNmEn"),
            "cast": (a.get("cast") or "").strip() or None,
            "actorGb": a.get("actorGb"),  # 출연 구분 (1이 주연)
        }
        for a in actors_raw
    ]

    return {
        "rank": meta.get("rank"),
        "movieCd": code,
        "titleKo": detail["titleKo"] or meta.get("title"),
        "titleEn": detail["titleEn"],
        "summary": detail["summary"],
        "rating": detail["rating"],
        "openDt": detail["openDt"] or meta.get("openDt"),
        "synopsis": detail["synopsis"],
        "stills": detail["stills"],
        "poster": detail["poster"],
        "posters": detail["posters"],
        "directors": directors,
        "actors": actors,
        "audiAcc": meta.get("audiAcc"),  # 누적 관객수 (역대 박스오피스 기준)
        "salesAcc": meta.get("salesAcc"),
        "crawledAt": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
    }


# ────────────────────────────── 메인 ──────────────────────────────
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--delay", type=float, default=1.0, help="영화 간 요청 간격(초)")
    ap.add_argument("--fresh", action="store_true", help="진행 파일을 무시하고 처음부터")
    args = ap.parse_args()

    if not os.path.exists(CODES_PATH):
        log.error("%s 가 없습니다. 먼저 1_fetch_codes.py 를 실행하세요.", CODES_PATH)
        raise SystemExit(1)
    with open(CODES_PATH, encoding="utf-8") as f:
        codes: list[dict] = json.load(f)

    if args.fresh and os.path.exists(PROGRESS_PATH):
        backup = PROGRESS_PATH + time.strftime(".%Y%m%d%H%M%S.bak")
        os.replace(PROGRESS_PATH, backup)
        log.info("--fresh: 기존 진행 파일을 %s 로 백업했습니다.", backup)

    done = load_progress()
    code_set = {m["movieCd"] for m in codes}
    stale = [c for c in done if c not in code_set]
    for c in stale:  # 코드 목록에서 빠진 영화 (titles.txt 에서 삭제 등)
        del done[c]
    if stale:
        log.info("코드 목록에 없는 영화 %d편은 결과에서 제외합니다.", len(stale))
    todo = [m for m in codes if m["movieCd"] not in done]
    total = len(codes)

    if done:
        log.info("진행 중인 파일 발견: %s — 완료 %d편, 남은 %d편부터 이어서 진행합니다.",
                 PROGRESS_PATH, len(done), len(todo))
    else:
        log.info("새로 시작: 총 %d편", total)

    session = requests.Session()
    failed = []
    try:
        for i, meta in enumerate(todo, start=len(done) + 1):
            code, name = meta["movieCd"], meta.get("title")
            rank = f"#{meta['rank']} " if meta.get("rank") else ""
            tag = f"[{i}/{total}] {rank}{name}({code})"
            started = time.time()
            log.info("%s 크롤링 시작", tag)

            for attempt in range(1, MAX_RETRY + 1):
                try:
                    item = crawl_movie(session, meta)
                    append_progress(item)
                    done[code] = item
                    log.info("%s 완료 (%.1fs) — 감독 %s | 배우 %d명 | 포스터 %d장 | 스틸컷 %d장 | 등급 %s | 관객 %s",
                             tag, time.time() - started,
                             ", ".join(d["name"] for d in item["directors"]) or "-",
                             len(item["actors"]), len(item["posters"]), len(item["stills"]), item["rating"],
                             f"{item['audiAcc']:,}" if item["audiAcc"] else "-")
                    break
                except (requests.RequestException, ValueError) as e:
                    wait = attempt * 3
                    if attempt < MAX_RETRY:
                        log.warning("%s 실패 (%d/%d): %s → %ds 후 재시도",
                                    tag, attempt, MAX_RETRY, e, wait)
                        time.sleep(wait)
                        session = requests.Session()  # 세션 문제 대비 새로 생성
                    else:
                        log.error("%s 최종 실패: %s", tag, e)
                        failed.append({**meta, "error": str(e)})
            time.sleep(args.delay)
    except KeyboardInterrupt:
        log.warning("사용자 중단. 지금까지 %d/%d편 저장됨 — 다시 실행하면 이어서 진행합니다.",
                    len(done), total)
        raise SystemExit(130)

    if failed:
        save_json(FAILED_PATH, failed)
        log.warning("실패 %d편 → %s (다시 실행하면 실패한 영화만 재시도합니다)",
                    len(failed), FAILED_PATH)
    elif os.path.exists(FAILED_PATH):
        os.remove(FAILED_PATH)

    if len(done) == total:
        ordered = [done[m["movieCd"]] for m in codes]  # movie_codes.json 순서 (역대 순위 → titles.txt)
        save_json(OUTPUT_PATH, ordered)
        log.info("전체 완료: %d편 → %s", total, OUTPUT_PATH)
    else:
        log.info("완료 %d/%d편. 남은 영화는 다시 실행하면 이어서 진행합니다.", len(done), total)


if __name__ == "__main__":
    main()
