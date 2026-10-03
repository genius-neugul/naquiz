# initial_crawler

게임에 쓸 초기 데이터(노래 목록, 영화 정보)를 모으는 크롤러다. 게임 서버는 크롤링을 하지 않는다. 여기서 만든 `data/`의 파일로 서버의 초기 데이터 SQL(`core/core-domain/src/main/resources/sql/initial-data.sql`)을 만들고, 서버는 시작할 때 그 SQL을 실행한다.

| 결과 파일 | 내용 | 만드는 곳 |
|---|---|---|
| `data/radio_songs_final.json` | MBC 라디오 선곡표에서 뽑은 노래 목록 | `music/` |
| `data/movies.json` | KOBIS 역대 박스오피스 상위 200편 + `movie/titles.txt`의 추가 영화 | `movie/` |

## 1. 가상환경 준비

Python 3.10 이상이 필요하다. 가상환경 하나(`initial_crawler/.venv`)를 노래 크롤러와 영화 크롤러가 같이 쓴다.

```bash
cd initial_crawler
python3 -m venv .venv            # 처음 한 번만
source .venv/bin/activate        # 터미널을 새로 열 때마다 (끝낼 때: deactivate)
pip install -r requirements.txt  # 처음 한 번, requirements.txt가 바뀌면 다시
```

- 노래 크롤러는 표준 라이브러리만 쓰고, 영화 크롤러는 `requests`, `beautifulsoup4`를 쓴다.
- 아래 명령은 모두 가상환경을 켠 상태에서 실행한다. 프롬프트 앞에 `(.venv)`가 보이면 켜진 것이다.
- `movie/run.sh`는 가상환경을 알아서 만들고 켜므로 따로 준비하지 않아도 된다.

## 2. 디렉터리와 저장 경로

```
initial_crawler/
├── requirements.txt
├── data/                          최종 결과 (커밋)
│   ├── movies.json
│   └── radio_songs_final.json
├── records/                       크롤링 결과 기록 (YYYY-MM-DD-<music|movie>.md, 커밋)
├── music/
│   ├── scripts/radio/             노래 크롤러 (special_artists.csv: 특별 케이스 이름)
│   └── crawl_output/              중간 산출물 (커밋 안 함)
└── movie/
    ├── 1_fetch_codes.py, 2_crawl_details.py, 3_download_images.py, common.py, run.sh
    ├── titles.txt                 추가로 크롤링할 영화 제목 (직접 편집)
    ├── movie_codes.json           1단계 결과: 크롤링할 영화코드 목록
    ├── unmatched.json             1단계에서 코드를 못 찾은 제목 (있을 때만)
    ├── movies.jsonl, failed.json  2단계 진행 파일 / 실패 목록 (커밋 안 함)
    ├── images/                    3단계 포스터·스틸컷 (커밋 안 함)
    └── logs/                      실행 로그 (커밋 안 함)
```

## 3. 노래 크롤링 (`music/`)

MBC 라디오 선곡표 전체 회차를 크롤링해서 정제한다. 파싱 규칙은 [`docs/MUSIC_PARSING_RULE.md`](../docs/MUSIC_PARSING_RULE.md)에 있다.

`music/` 디렉터리에서 순서대로 실행한다.

```bash
cd initial_crawler/music

python3 scripts/radio/crawl.py        # → crawl_output/radio_songs.csv
python3 scripts/radio/make_unique.py  # → crawl_output/radio_songs_unique.csv
python3 scripts/radio/make_final.py   # → crawl_output/radio_songs_final.json

cp crawl_output/radio_songs_final.json ../data/
```

크롤링을 끝내면 단계별 곡 수와 실패 회차를 `records/YYYY-MM-DD-music.md`에 새 파일로 남긴다. 기록에 넣을 통계는 아래 스크립트로 뽑는다.

```bash
python3 scripts/radio/diff_unique.py <이전 radio_songs_unique.csv>  # → crawl_output/radio_songs_added.csv (새로 추가된 곡)
python3 scripts/radio/opening_stats.py                              # 오프닝일 가능성이 높은 곡 표를 출력
```

- `diff_unique.py`는 `make_unique.py`와 같은 기준(제목 키가 같고 가수 키가 하나라도 겹침)으로 같은 곡을 판정한다. `--reset`으로 다시 크롤링하기 전에 이전 `radio_songs_unique.csv`를 따로 복사해 둔다.

| 단계 | 입력 | 출력 |
|---|---|---|
| `crawl.py` | MBC 선곡표 | `radio_songs.csv`, `state.json`(진행 기록) |
| `make_unique.py` | `radio_songs.csv` | `radio_songs_unique.csv`(같은 곡으로 합친 방송 수 `play_count` 포함), `radio_songs_removed.csv`(가사 없는 곡) |
| `make_final.py` | `radio_songs_unique.csv` | `radio_songs_final.json`(`play_count` 포함), `radio_songs_removed.csv`, `radio_songs_unresolved.csv`(규칙으로 처리 못 한 곡) |
| `diff_unique.py` | 이전·새 `radio_songs_unique.csv` | `radio_songs_added.csv`(새로 추가된 곡) |
| `opening_stats.py` | `radio_songs.csv` | 화면 출력(오프닝일 가능성이 높은 곡) |

`crawl.py` 옵션:

- 옵션 없이 실행하면 처음부터 시작한다. 중간에 멈췄으면 같은 명령으로 멈춘 회차부터 이어간다. 진행 위치와 CSV를 어디까지 썼는지를 `state.json`에 기록해 두고, 다시 시작할 때 CSV를 그 위치로 잘라 내므로 중복되거나 깨진 행이 남지 않는다.
- 네트워크 에러(타임아웃, 연결 끊김)는 재시도한 뒤에도 실패하면 그 회차 직전까지 저장하고 멈춘다.
- 재시도해도 HTTP 500을 내는 회차는 `state.json`의 `failed`에 기록하고 넘어간다.
- `--skip-errors`: 네트워크 에러가 난 회차도 `state.json`의 `failed`에 기록하고 넘어간다.
- `--reset`: 기존 결과와 기록을 지우고 처음부터 다시 한다.
- `--out-dir DIR`: 결과 폴더를 바꾼다(기본 `crawl_output`). 이때 `make_unique.py DIR`, `make_final.py DIR`처럼 같은 폴더를 넘긴다.
- `--workers N`: 동시 요청 수(기본 8).

## 4. 영화 크롤링 (`movie/`)

KOBIS에서 역대 박스오피스 상위 200편과 `titles.txt`에 적은 추가 영화를 **한 번에** 크롤링해 `data/movies.json` 하나로 저장한다. 순서는 역대 순위순 200편 다음에 추가 영화가 `titles.txt` 순서로 온다. 추가 영화는 `rank`, `audiAcc`, `salesAcc`가 `null`이다.

### 한 번에 실행

```bash
cd initial_crawler/movie
./run.sh             # 가상환경 준비 → 1단계 → 2단계
./run.sh --refresh   # 역대 박스오피스 목록도 새로 받기
./run.sh --images    # 3단계(이미지 다운로드)까지
```

### 단계별 실행

가상환경을 켠 상태라면 어느 디렉터리에서 실행해도 결과는 같은 위치에 저장된다.

```bash
cd initial_crawler/movie

python 1_fetch_codes.py      # → movie_codes.json
python 2_crawl_details.py    # → ../data/movies.json
python 3_download_images.py  # → images/ (선택)
```

**1단계 `1_fetch_codes.py`**: 크롤링할 영화코드 목록 만들기

- 역대 박스오피스 상위 200편을 받고, `titles.txt`의 제목을 KOBIS에서 검색해 코드를 찾는다.
- 역대 순위는 `movie_codes.json`에 이미 있으면 다시 받지 않는다. 이미 검색한 제목도 건너뛴다.
- `--refresh`: 역대 박스오피스 목록을 다시 받는다. `--limit N`: 상위 N편만 받는다(기본 200). `--delay 초`: 검색 요청 간격.

**2단계 `2_crawl_details.py`**: 영화별 상세정보(감독, 배우, 장르, 등급, 시놉시스, 포스터, 스틸컷 등)

- 1편이 끝날 때마다 `movies.jsonl`에 저장한다. 다시 실행하면 끝난 영화는 건너뛰고 이어서 진행한다.
- 모두 끝나면 `../data/movies.json`을 쓴다. 실패한 영화는 `failed.json`에 남고, 다시 실행하면 그 영화만 재시도한다.
- `--fresh`: 진행 파일을 백업하고 처음부터 한다. `--delay 초`: 요청 간격(기본 1.0).

**3단계 `3_download_images.py`** (선택): 포스터·스틸컷을 로컬에 받는다(개발·오프라인 테스트용).

- 저장 위치: `images/posters/{movieCd}/01.jpg`(01은 대표 포스터), `images/stills/{movieCd}/NN.jpg`, 각 폴더의 `manifest.json`
- `--kind posters|stills`, `--size 640`(썸네일), `--max-per-movie N`, `--delay 초`
- 이미지는 저작권이 있으므로 서비스에서는 원본 URL을 참조한다.

### 추가 영화 넣기 (`titles.txt`)

역대 200위에는 없지만 유명한 영화는 `titles.txt`에 적는다.

```
# 제목 | 연도                (연도는 제작연도 또는 개봉연도)
서편제 | 1993
# 검색으로 못 찾거나 다른 영화가 잡히면 영화코드를 직접 적는다
제목 | 연도 | 영화코드
```

적은 뒤 `./run.sh`를 실행하면 새 제목만 검색하고 크롤링한다. `titles.txt`에서 지운 제목은 결과에서도 빠진다.

1단계에서 코드를 못 찾은 제목은 `unmatched.json`에 후보 목록과 함께 남는다. 후보 중 맞는 영화의 `movieCd`를 골라 `제목 | 연도 | 영화코드` 형식으로 적고 다시 실행한다.
