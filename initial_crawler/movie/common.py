"""KOBIS 크롤러 공통 설정/유틸"""
import json
import logging
import os

BASE = "https://www.kobis.or.kr"
LIST_URL = BASE + "/kobis/business/stat/boxs/findFormerBoxOfficeList.do"
DTL_URL = BASE + "/kobis/business/mast/mvie/searchMovieDtl.do"
ACTOR_URL = BASE + "/kobis/business/mast/mvie/searchMovActorLists.do"
STAFF_URL = BASE + "/kobis/business/mast/mvie/searchMovStaffLists.do"

# 경로는 이 파일 위치 기준 (어디서 실행해도 같은 곳에 저장)
HERE = os.path.dirname(os.path.abspath(__file__))           # initial_crawler/movie
LOG_DIR = os.path.join(HERE, "logs")
IMAGES_DIR = os.path.join(HERE, "images")
CODES_PATH = os.path.join(HERE, "movie_codes.json")        # 1단계 결과 (역대 순위 + titles.txt)
TITLES_PATH = os.path.join(HERE, "titles.txt")             # 추가로 크롤링할 영화 제목
UNMATCHED_PATH = os.path.join(HERE, "unmatched.json")      # titles.txt 중 코드를 못 찾은 제목
PROGRESS_PATH = os.path.join(HERE, "movies.jsonl")         # 2단계 진행 파일 (이어하기)
FAILED_PATH = os.path.join(HERE, "failed.json")            # 2단계 최종 실패 목록
OUTPUT_PATH = os.path.normpath(os.path.join(HERE, "..", "data", "movies.json"))  # 최종 결과

UA = ("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/128.0 Safari/537.36")

HTML_HEADERS = {
    "User-Agent": UA,
    "Referer": LIST_URL,
    "Content-Type": "application/x-www-form-urlencoded",
}

# 배우/스태프 API는 이 두 헤더가 없으면 JSON 대신 에러 HTML을 돌려준다
JSON_HEADERS = {
    **HTML_HEADERS,
    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
    "Accept": "application/json, text/javascript, */*; q=0.01",
    "X-Requested-With": "XMLHttpRequest",
}


def setup_logger(name: str) -> logging.Logger:
    os.makedirs(LOG_DIR, exist_ok=True)
    logger = logging.getLogger(name)
    if logger.handlers:
        return logger
    logger.setLevel(logging.INFO)
    fmt = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s", "%Y-%m-%d %H:%M:%S")
    for h in (logging.StreamHandler(),
              logging.FileHandler(os.path.join(LOG_DIR, f"{name}.log"), encoding="utf-8")):
        h.setFormatter(fmt)
        logger.addHandler(h)
    return logger


def save_json(path: str, data):
    """임시 파일에 쓴 뒤 교체한다. 중간에 죽어도 기존 파일이 깨지지 않는다."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.replace(tmp, path)
