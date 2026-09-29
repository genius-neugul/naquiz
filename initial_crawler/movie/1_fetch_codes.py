"""
[1단계] 크롤링할 영화코드 목록 만들기
  ① KOBIS 역대 박스오피스 순위대로 영화코드 추출 (기본 200편)
  ② titles.txt 의 추가 영화(역대 순위에는 없지만 유명한 영화)를 제목으로 검색해 영화코드 추가
  결과: movie_codes.json
        [{rank, movieCd, title, openDt, salesAcc, audiAcc, scrnCnt, showCnt}, ...   ← ① 역대 순위순
         {rank: null, movieCd, title, query, prdtYear, nation, director, ...}, ...] ← ② titles.txt 순서
  보류: unmatched.json   (titles.txt 중 못 찾았거나 애매한 제목 + 후보 목록)
  로그: logs/fetch_codes.log

  실행: python 1_fetch_codes.py              # 기본
        python 1_fetch_codes.py --refresh    # 역대 박스오피스 목록을 다시 받기
        python 1_fetch_codes.py --limit 50   # 역대 상위 50편만 (--refresh 와 함께)
  다음: python 2_crawl_details.py

참고: 역대 박스오피스는 POST 한 번에 상위 200편을 전부 돌려준다 (페이징 없음).

이어하기
  - movie_codes.json 에 역대 순위 영화가 이미 있으면 ①을 건너뛴다 (--refresh 로 다시 받기).
  - ②는 이미 검색한 제목(query)을 건너뛰고, 1편 찾을 때마다 저장한다.
  - titles.txt 에서 지운 제목은 결과에서도 빠진다.

titles.txt 매칭 규칙
  1. 제작연도가 [연도-2, 연도+1] 범위인 영화만 검색 (개봉연도와 제작연도 차이 보정)
  2. 공백·문장부호를 뺀 한글 제목이 정확히 같은 후보만 인정
  3. 여러 개면 제작상태 '개봉' > 유형 '장편' > 연도 차이가 작은 순
  4. 정확히 같은 제목이 없으면 매칭하지 않고 후보를 unmatched.json 에 남김
     → titles.txt 에 '제목 | 연도 | 영화코드' 로 직접 적어 해결
  5. 역대 순위에 이미 있는 영화는 중복으로 넣지 않음
"""
import argparse
import json
import os
import re
import time

import requests
from bs4 import BeautifulSoup

from common import (BASE, CODES_PATH, HTML_HEADERS, LIST_URL, LOG_DIR, TITLES_PATH,
                    UNMATCHED_PATH, save_json, setup_logger)

log = setup_logger("fetch_codes")

SEARCH_URL = BASE + "/kobis/business/mast/mvie/searchMovieList.do"
CODE_RE = re.compile(r"mstView\(\s*'movie'\s*,\s*'(\w+)'")  # 코드에 영문자가 섞이기도 함 (예: 2024A105)
MAX_PAGES = 5  # 한 제목당 최대 검색 페이지 (페이지당 10건)


def to_int(text):
    digits = re.sub(r"[^\d]", "", text or "")
    return int(digits) if digits else None


def get_csrf_token(session: requests.Session, url: str) -> str:
    r = session.get(url, headers=HTML_HEADERS, timeout=20)
    r.raise_for_status()
    tok = BeautifulSoup(r.text, "html.parser").select_one("#searchForm input[name=CSRFToken]")
    if not tok:
        raise RuntimeError("검색 폼에서 CSRFToken을 찾지 못했습니다.")
    return tok["value"]


# ────────────────────────── ① 역대 박스오피스 ──────────────────────────
def fetch_list_html(session: requests.Session, token: str) -> str:
    data = {
        "CSRFToken": token,
        "loadEnd": "0",
        "searchType": "search",
        "sMultiMovieYn": "",  # 영화구분 (전체)
        "sRepNationCd": "",   # 국적 (전체)
        "sWideAreaCd": "",    # 지역 (전체)
    }
    r = session.post(LIST_URL, data=data, headers=HTML_HEADERS, timeout=30)
    r.raise_for_status()
    return r.text


def parse_rows(html: str) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    rows = []
    for tr in soup.select("table tbody tr"):
        a = tr.select_one("td#td_movie a")
        m = CODE_RE.search(str(a)) if a else None
        if not m:
            continue

        def td(i):
            el = tr.select_one(f"td#{i}")
            return el.get_text(strip=True) if el else None

        rows.append({
            "rank": to_int(td("td_rank")),
            "movieCd": m.group(1),
            "title": a.get("title") or a.get_text(strip=True),
            "openDt": td("td_openDt"),
            "salesAcc": to_int(td("td_salesAcc")),
            "audiAcc": to_int(td("td_audiAcc")),
            "scrnCnt": to_int(td("td_scrnCnt")),
            "showCnt": to_int(td("td_showCnt")),
        })
    return rows


def fetch_ranked(session: requests.Session, limit: int) -> list[dict]:
    log.info("CSRF 토큰 발급: %s", LIST_URL)
    token = get_csrf_token(session, LIST_URL)

    log.info("역대 박스오피스 목록 요청")
    html = fetch_list_html(session, token)
    rows = parse_rows(html)[:limit]

    if not rows:
        os.makedirs(LOG_DIR, exist_ok=True)
        dump = os.path.join(LOG_DIR, "list_debug.html")
        with open(dump, "w", encoding="utf-8") as f:
            f.write(html)
        log.error("영화코드를 추출하지 못했습니다. 응답 HTML을 %s 에 저장했습니다.", dump)
        raise SystemExit(1)

    log.info("역대 박스오피스 %d편 (1위 %s, %d위 %s)",
             len(rows), rows[0]["title"], rows[-1]["rank"], rows[-1]["title"])
    if len(rows) < limit:
        log.warning("요청한 %d편보다 적게 추출되었습니다 (%d편).", limit, len(rows))
    return rows


# ────────────────────────── ② titles.txt 검색 ──────────────────────────
def norm(s: str) -> str:
    return re.sub(r"[\s\-_:：·ㆍ・.,!?'\"’“”()\[\]~]", "", (s or "").lower())


def read_titles(path: str) -> list[dict]:
    items = []
    with open(path, encoding="utf-8") as f:
        for no, line in enumerate(f, 1):
            line = line.split("#", 1)[0].strip()  # 줄 끝 주석 제거
            if not line:
                continue
            parts = [p.strip() for p in line.split("|")]
            if len(parts) < 2 or not parts[1].isdigit():
                log.warning("titles.txt %d번째 줄 형식 오류 (제목 | 연도): %s", no, line)
                continue
            items.append({"title": parts[0], "year": int(parts[1]),
                          "code": parts[2] if len(parts) > 2 and parts[2] else None})
    return items


def search_page(session, token, title, year, page) -> tuple[list[dict], int]:
    data = {
        "CSRFToken": token, "curPage": str(page), "searchType": "search",
        "sMovName": title, "sPrdtYearS": str(year - 2), "sPrdtYearE": str(year + 1),
        "sMultiChk": "YYY", "sNomal": "Y", "sMulti": "Y", "sIndie": "Y", "useYn": "Y",
    }
    r = session.post(SEARCH_URL, data=data, headers=HTML_HEADERS, timeout=20)
    r.raise_for_status()
    soup = BeautifulSoup(r.text, "html.parser")

    total_m = re.search(r"총\s*<em[^>]*>([\d,]+)</em>", r.text)
    total = int(total_m.group(1).replace(",", "")) if total_m else 0

    table = next((t for t in soup.find_all("table") if "영화명(영문)" in t.get_text()), None)
    rows = []
    if table:
        for tr in table.select("tbody tr"):
            m = CODE_RE.search(str(tr))
            tds = [" ".join(td.get_text(" ", strip=True).split()) for td in tr.find_all("td")]
            if not m or len(tds) < 9:
                continue
            rows.append({
                "movieCd": m.group(1), "titleKo": tds[0], "titleEn": tds[1],
                "prdtYear": int(tds[3]) if tds[3].isdigit() else None,
                "nation": tds[4], "type": tds[5], "genre": tds[6],
                "status": tds[7], "director": tds[8],
            })
    return rows, total


def find_code(session, token, title, year) -> tuple[dict | None, list[dict]]:
    """(선택된 후보, 전체 후보) 반환"""
    candidates, target = [], norm(title)
    for page in range(1, MAX_PAGES + 1):
        rows, total = search_page(session, token, title, year, page)
        candidates.extend(rows)
        if any(norm(r["titleKo"]) == target for r in candidates) or len(candidates) >= total or not rows:
            break
        time.sleep(0.3)

    exact = [r for r in candidates if norm(r["titleKo"]) == target]
    if not exact:
        return None, candidates

    def score(r):
        return (r["status"] != "개봉", r["type"] != "장편",
                abs((r["prdtYear"] or year) - year))
    return sorted(exact, key=score)[0], candidates


def search_extras(items: list[dict], ranked: list[dict], extras: list[dict],
                  delay: float) -> tuple[list[dict], list[dict]]:
    """titles.txt 항목 중 아직 검색하지 않은 제목을 찾아 extras 에 추가. (extras, unmatched) 반환"""
    done_titles = {e["query"] for e in extras}
    done_codes = {m["movieCd"] for m in ranked} | {e["movieCd"] for e in extras}
    todo = [it for it in items if it["title"] not in done_titles]
    if not todo:
        log.info("titles.txt %d편 모두 검색 완료 — 건너뜀", len(items))
        return extras, []
    log.info("titles.txt 검색: 완료 %d편, 남은 %d편", len(items) - len(todo), len(todo))

    session = requests.Session()
    token = get_csrf_token(session, SEARCH_URL)
    unmatched = []

    for i, it in enumerate(todo, start=len(items) - len(todo) + 1):
        title, year = it["title"], it["year"]
        tag = f"[{i}/{len(items)}] {title}({year})"
        try:
            if it["code"]:
                pick, cands = {"movieCd": it["code"], "titleKo": title, "prdtYear": year,
                               "nation": None, "status": None, "director": None}, []
                log.info("%s → 지정 코드 사용 [%s]", tag, it["code"])
            else:
                pick, cands = find_code(session, token, title, year)
        except requests.RequestException as e:
            log.error("%s 검색 실패: %s", tag, e)
            unmatched.append({**it, "reason": f"요청 실패: {e}", "candidates": []})
            time.sleep(3)
            try:  # 세션/토큰 새로 발급 후 다음 제목 진행
                session = requests.Session()
                token = get_csrf_token(session, SEARCH_URL)
            except requests.RequestException:
                pass
            continue

        if not pick:
            log.warning("%s → 일치하는 제목 없음 (후보 %d건: %s)", tag, len(cands),
                        ", ".join(f"{c['titleKo']}({c['prdtYear']})" for c in cands[:5]) or "-")
            unmatched.append({**it, "reason": "정확히 일치하는 제목 없음", "candidates": cands[:10]})
        elif pick["movieCd"] in done_codes:
            log.warning("%s → [%s] 이미 목록(역대 순위 또는 추가 목록)에 있는 영화라 건너뜀", tag, pick["movieCd"])
        else:
            extras.append({
                "rank": None,
                "movieCd": pick["movieCd"],
                "title": pick["titleKo"],
                "openDt": None,
                "salesAcc": None,
                "audiAcc": None,  # 검색 결과에는 관객수가 없음
                "query": title,
                "prdtYear": pick.get("prdtYear"),
                "nation": pick.get("nation"),
                "director": pick.get("director"),
            })
            done_codes.add(pick["movieCd"])
            save_json(CODES_PATH, ranked + extras)  # 1편마다 저장 (이어하기)
            if not it["code"]:
                log.info("%s → %s [%s] %s %s %s", tag, pick["titleKo"], pick["movieCd"],
                         pick["prdtYear"], pick["nation"], pick["director"] or "")
        time.sleep(delay)

    return extras, unmatched


# ────────────────────────────── 메인 ──────────────────────────────
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--refresh", action="store_true", help="역대 박스오피스 목록을 다시 받기")
    ap.add_argument("--limit", type=int, default=200, help="역대 박스오피스 상위 몇 편까지")
    ap.add_argument("--titles", default=TITLES_PATH, help="추가 영화 제목 파일 (기본 titles.txt)")
    ap.add_argument("--delay", type=float, default=0.5, help="제목 검색 간 요청 간격(초)")
    args = ap.parse_args()

    existing: list[dict] = []
    if os.path.exists(CODES_PATH):
        with open(CODES_PATH, encoding="utf-8") as f:
            existing = json.load(f)
    ranked = [m for m in existing if m.get("rank") is not None]
    extras = [m for m in existing if m.get("rank") is None]

    # ① 역대 박스오피스
    if args.refresh or not ranked:
        ranked = fetch_ranked(requests.Session(), args.limit)
    else:
        log.info("역대 박스오피스 %d편은 기존 %s 사용 (다시 받으려면 --refresh)", len(ranked), CODES_PATH)
    ranked_codes = {m["movieCd"] for m in ranked}

    # ② titles.txt
    items = read_titles(args.titles) if os.path.exists(args.titles) else []
    if not items:
        log.warning("%s 가 없거나 비어 있어 추가 영화 없이 진행합니다.", args.titles)
    order = {it["title"]: i for i, it in enumerate(items)}
    dropped = [e for e in extras if e["query"] not in order or e["movieCd"] in ranked_codes]
    for e in dropped:
        why = "역대 순위에 포함됨" if e["movieCd"] in ranked_codes else "titles.txt 에서 삭제됨"
        log.info("추가 목록에서 제외: %s [%s] (%s)", e["query"], e["movieCd"], why)
    extras = [e for e in extras if e not in dropped]

    extras, unmatched = search_extras(items, ranked, extras, args.delay)
    extras.sort(key=lambda e: order[e["query"]])  # titles.txt 순서로 정렬

    save_json(CODES_PATH, ranked + extras)
    if unmatched:
        save_json(UNMATCHED_PATH, unmatched)
        log.warning("못 찾은 제목 %d편 → %s (titles.txt 에 '제목 | 연도 | 영화코드' 로 지정 가능)",
                    len(unmatched), UNMATCHED_PATH)
    elif os.path.exists(UNMATCHED_PATH):
        os.remove(UNMATCHED_PATH)
    log.info("완료: 역대 순위 %d편 + 추가 %d편 = %d편 → %s",
             len(ranked), len(extras), len(ranked) + len(extras), CODES_PATH)


if __name__ == "__main__":
    main()
