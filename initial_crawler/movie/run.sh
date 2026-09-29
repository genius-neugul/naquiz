#!/usr/bin/env bash
# KOBIS 영화 크롤러 실행 스크립트
#   ./run.sh            가상환경 준비 → 1단계(영화코드: 역대 박스오피스 + titles.txt) → 2단계(상세 크롤링)
#   ./run.sh --refresh  movie_codes.json 에 역대 순위가 있어도 역대 박스오피스 목록을 다시 받기
#   ./run.sh --images   2단계 후 포스터·스틸컷 이미지도 내려받기 (images/posters/, images/stills/)
#   (옵션은 함께 사용 가능: ./run.sh --refresh --images)
# 결과: ../data/movies.json
# 모든 단계는 이어하기를 지원한다. 중간에 멈추면 같은 명령을 다시 실행하면 된다.
set -euo pipefail
cd "$(dirname "$0")"

REFRESH=()
IMAGES=0
for arg in "$@"; do
  case "$arg" in
    --refresh) REFRESH=(--refresh) ;;
    --images|--stills) IMAGES=1 ;;
    *) echo "알 수 없는 옵션: $arg (사용 가능: --refresh, --images)" >&2; exit 1 ;;
  esac
done

PY=${PYTHON:-python3}
command -v "$PY" >/dev/null || { echo "python3 를 찾을 수 없습니다." >&2; exit 1; }

# 0) 가상환경 + 의존성 (initial_crawler/.venv 를 노래 크롤러와 함께 사용)
VENV=../.venv
if [[ ! -d $VENV ]]; then
  echo "▶ 가상환경 생성 ($VENV)"
  "$PY" -m venv "$VENV"
fi
source "$VENV/bin/activate"
echo "▶ 의존성 설치"
python -m pip install -q --upgrade pip
python -m pip install -q -r ../requirements.txt

# 1) 영화코드: 역대 박스오피스 + titles.txt 검색 (이어하기 지원)
echo "▶ [1] 영화코드 목록 만들기 (역대 박스오피스 + titles.txt)"
python 1_fetch_codes.py ${REFRESH[@]+"${REFRESH[@]}"}

# 2) 상세 크롤링 (이어하기 지원)
echo "▶ [2] 영화 상세정보 크롤링 (movie_codes.json → ../data/movies.json)"
python 2_crawl_details.py

# 3) 포스터·스틸컷 다운로드 (선택, 이어하기 지원)
if [[ $IMAGES -eq 1 ]]; then
  echo "▶ [3] 포스터·스틸컷 이미지 다운로드"
  python 3_download_images.py
fi

echo "✔ 완료 — 결과: ../data/movies.json, 로그: logs/"
