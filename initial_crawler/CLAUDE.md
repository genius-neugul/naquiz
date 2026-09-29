# initial_crawler/CLAUDE.md

초기 데이터 크롤러 작업 시 참고한다. 실행 방법 상세는 `README.md`, 노래 크롤링·파싱 규칙은 `../docs/MUSIC_PARSING_RULE.md`를 본다.

## 자주 쓰는 명령

```bash
# 크롤러 가상환경 (initial_crawler/ 에서, 처음 한 번)
python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt

# 노래 목록 (initial_crawler/music/ 에서 순서대로)
python3 scripts/radio/crawl.py          # → crawl_output/radio_songs.csv (이어하기 지원, --reset / --skip-errors)
python3 scripts/radio/make_unique.py    # → crawl_output/radio_songs_unique.csv
python3 scripts/radio/make_final.py     # → crawl_output/radio_songs_final.json (+ removed.csv, unresolved.csv)
cp crawl_output/radio_songs_final.json ../data/
python3 scripts/radio/diff_unique.py <이전 unique.csv>  # → crawl_output/radio_songs_added.csv (records용)
python3 scripts/radio/opening_stats.py                 # 오프닝일 가능성이 높은 곡 표 (records용)

# 영화 목록 (역대 200편 + titles.txt 추가 영화를 한 번에 → initial_crawler/data/movies.json)
movie/run.sh [--refresh] [--images]
```

## 크롤링 결과 기록 (`records/`)

- 크롤링을 돌린 뒤 결과 수치는 `records/YYYY-MM-DD-<music|movie>.md`에 **새 파일**로 남긴다. 이전 기록은 고치지 않는다.
- 남길 것: 크롤링한 범위(최신 회차), 단계별 곡 수, 삭제·미해결 사유별 수, 실패 회차, 데이터에서 뽑은 통계(예: 오프닝일 가능성이 높은 곡).
- 규칙 문서(`docs/MUSIC_PARSING_RULE.md`)에는 수치를 넣지 않는다.
