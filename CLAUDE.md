# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 안내서다. 기획과 규칙의 상세 내용은 `docs/`에 있다. 이 파일과 문서가 다르면 **문서가 우선**이다.

## 프로젝트 개요

지인끼리 초대 코드로 방에 모여 **실시간 채팅으로 먼저 정답을 치는** 멀티플레이 퀴즈 게임(노래 맞추기, 영화 스무고개, 스틸컷 영화 맞추기).

## 참고 문서

- `docs/기획.md`: 1차 MVP 기획. 게임 종류, 게임 룰, 힌트·투표 규칙, 백오피스
- `docs/DOMAIN.md`: 도메인 설계. 유비쿼터스 언어, 애그리거트, 정답 판정·마스킹·투표 규칙, 미정 사항
- `docs/MUSIC_PARSING_RULE.md`: 노래 크롤링과 제목·가수 파싱 규칙
- `docs/MUSIC_SELECTION_RULE.md`: 게임에서 Spotify 곡 정보 조회와 YouTube 영상 선택 규칙
- `docs/API.md`: HTTP API 요청/응답 규격
- `core/CLAUDE.md`: 게임 서버 기술 스택, 빌드·테스트 명령, 서버 작업 규칙
- `core/docs/`: 게임 서버 코드 컨벤션(아키텍처, 코드 스타일, 예외, 로그, 테스트)
- `front/CLAUDE.md`: 프론트엔드(게임 앱·백오피스 앱) 기술 스택, 명령, 작업 규칙
- `front/README.md`: 프론트엔드 워크스페이스 구성과 실행 방법, Mock 클라이언트 설명
- `initial_crawler/README.md`: 크롤러 실행 방법(가상환경 포함)과 저장 경로
- `initial_crawler/CLAUDE.md`: 크롤러 자주 쓰는 명령, 크롤링 결과 기록 규칙
- `initial_crawler/records/`: 크롤링 결과 기록(`YYYY-MM-DD-<music|movie>.md`). 단계별 곡 수, 실패 회차, 통계
- `.claude/README.md` (`docs/CLAUDE_CODE.md`는 이 파일의 심볼릭 링크): 팀 공용 스킬(/pre-pr, /sync-docs, /devlog, /explain), 에이전트, 훅 설명과 처음 설정·문제 해결
- `docs/devlog/`: 날짜별 개발일지(`YYYY-MM-DD-<작성자>.md`). 무엇을 했고 왜 그렇게 정했는지, 다음 할 일
- `docs/troubleshooting/`: 문제별 기록(`YYYY-MM-DD-<주제>.md`). 증상, 원인, 해결, 확인

## 디렉터리 구조

```
.claude/               Claude Code 설정 (팀 공유, 커밋한다)
├── README.md          스킬·에이전트·훅 사용 설명
├── settings.json      훅 등록 (세션 시작 브리핑, PR 게이트, 테스트 context 가드)
├── hooks/             pr-gate.sh, session-brief.sh, test-context-guard.py
├── agents/            test-runner(Haiku), code-reviewer, docs-syncer, explain-drafter
└── skills/            /pre-pr, /sync-docs, /devlog, /explain
core/                  게임 서버 (Spring Boot, Gradle 멀티모듈)
├── CLAUDE.md          서버 기술 스택, 명령, 작업 규칙
├── docs/              서버 코드 컨벤션
├── core-domain/       라이브러리. 엔티티·저장소·implement, 공통 예외·이벤트
├── game-api/          실행 앱. 게임·방·투표 API
├── admin-api/         실행 앱. 백오피스 API
└── crawler-batch/     실행 앱. 데일리 크롤링
front/                 프론트엔드 (npm workspaces, Vite + React + TypeScript)
├── CLAUDE.md          프론트 기술 스택, 명령, 작업 규칙
├── apps/game/         게임 앱
├── apps/admin/        백오피스 앱
├── packages/ui/       디자인 토큰·테마·공용 컴포넌트
└── packages/shared/   도메인 타입·상수, 정답 판정·마스킹·투표 순수 함수
docs/                  기획·도메인·파싱 규칙·API 문서
├── CLAUDE_CODE.md     .claude/README.md 심볼릭 링크
├── devlog/            개발일지
└── troubleshooting/   트러블슈팅 기록
initial_crawler/       초기 데이터를 확보하기 위한 크롤러 (Python 3.10+, venv: initial_crawler/.venv)
├── CLAUDE.md          크롤러 명령, 기록 규칙
├── records/           크롤링 결과 기록
├── data/              초기 데이터. 게임 서버의 초기 데이터 SQL(core-domain sql/initial-data.sql)을 이 파일로 만든다
│   ├── movies.json            KOBIS 역대 박스오피스 200편 + titles.txt 추가 영화
│   └── radio_songs_final.json MBC 라디오 선곡표 기반 노래 목록
├── music/             노래 크롤러 (scripts/radio/, 중간 산출물 crawl_output/)
└── movie/             영화 크롤러 (1_fetch_codes → 2_crawl_details → 3_download_images, titles.txt)
```

## 기술 스택

- 백엔드: Java 25, Spring Boot (`core/`). 상세는 `core/CLAUDE.md`
- 프론트엔드: React 19, TypeScript, Vite, CSS Modules, npm workspaces (`front/`). 상세는 `front/CLAUDE.md`
- 실시간 통신:
- DB / 캐시: H2(로컬·테스트), MySQL(`local-dev` 프로필, docker-compose). 진행 중인 방·참가자는 서버 메모리. 상세는 `core/CLAUDE.md`
- 데이터 수집: Python 3 (`initial_crawler/`)

## 자주 쓰는 명령

- 게임 서버 빌드·테스트·로컬 실행: `core/CLAUDE.md`
- 프론트엔드 개발 서버·빌드·테스트: `front/CLAUDE.md`
- 크롤러(노래·영화 목록): `initial_crawler/CLAUDE.md`

## 작업 규칙

- 게임 서버 작업 규칙(게임 규칙 순수 함수·테스트, 서버 단일 진실 원천, 서버 타이머)은 `core/CLAUDE.md`에 있다.
- 프론트엔드 작업 규칙(공용 패키지, 디자인 토큰, GameClient 경유 상태 접근)은 `front/CLAUDE.md`에 있다.
- 크롤링 중간 산출물(`crawl_output/`, `movies.jsonl`, `logs/`, `images/`)과 API 키는 커밋하지 않는다(`.gitignore`). 키는 환경 변수로 받는다. 최종 결과 `initial_crawler/data/`는 커밋한다.
- 파싱 코드를 고칠 때는 `docs/MUSIC_PARSING_RULE.md`의 "구현할 때 주의할 점"을 먼저 읽는다.
- 문서에 없는 규칙을 새로 정해야 하면 임의로 정하지 말고 먼저 물어본다.

## 문서 관리

- 문서에는 최종 결과(현재 상태)만 쓴다. "~ 때문에 ~로 바꿨다" 같은 변경 과정·이력은 `docs/devlog/`, `docs/troubleshooting/`, 회고 문서에 따로 쓰고, 본문에는 그 문서 링크만 건다. 규칙이 왜 그런지 설명하는 문장은 규칙의 일부로 남긴다.
- 문서는 제목이 말하는 범위만 담는다. 범위 밖 내용은 맞는 문서로 옮기고 링크를 건다.
- 크롤링 실행 결과(단계별 곡 수, 최신 회차, 실패 회차, 데이터 통계)는 `initial_crawler/records/`에 둔다. 규칙 문서에는 수치를 넣지 않는다.
- 크롤러 실행 방법·옵션·중간 파일 설명은 `initial_crawler/README.md`에 둔다.
- `docs/CLAUDE_CODE.md`는 `.claude/README.md`의 심볼릭 링크다. 내용은 `.claude/README.md`에서 고친다.

## 협업 흐름 (2인)

- 브랜치: `feat/<기능>`, `fix/<내용>`, `docs/<내용>` → PR → 팀원 1명 승인 후 머지. `main`에 직접 커밋하지 않는다(개발일지 포함).
- 커밋 메시지: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:` 접두어 + 한국어 설명.
- PR은 `/pre-pr` 로 만든다. 테스트(test-runner, `core/` 코드 변경 시) · 코드 리뷰(code-reviewer, `core/` 변경 시) · 문서 동기화(docs-syncer)와, 원하면 변경을 그림으로 설명한 아티팩트 초안(explain-drafter, 매번 물어봄)을 한 번에 돌리고, 통과해야 `gh pr create` 가 허용된다(PR 게이트 훅). 점검 후 새 커밋이 생기면 다시 돌려야 한다.
- `/pre-pr` 은 이번 작업과 무관한 워킹트리 변경을 묻지 않고 `git stash` 해두고, PR을 만든 뒤 `git stash pop` 으로 되돌린다.
- 테스트는 메인 대화에서 직접 돌리지 않고 `test-runner` 에이전트에 맡긴다(Haiku, 실패만 요약).
- Claude는 코드·명령·설정을 바꾸는 작업을 할 때마다 끝내기 전에 문서 동기화를 한다. 이 파일, 하위 디렉터리의 `CLAUDE.md`, `initial_crawler/README.md`, `front/README.md`, `.claude/README.md`, `docs/`, `core/docs/`에서 바뀐 내용과 어긋나는 곳을 찾는다. 사실을 기록한 부분은 바로 고치고, `docs/` 규칙과 어긋나면 아래 규칙대로 먼저 알린다.
- 코드를 바꿨는데 `docs/` 규칙과 달라지면 문서를 고치지 말고 먼저 알린다. 실행 방법·구조·명령처럼 사실을 기록한 부분(이 파일, `core/CLAUDE.md`, `front/CLAUDE.md`, `front/README.md`, `initial_crawler/CLAUDE.md`, `initial_crawler/README.md`)만 코드에 맞춰 고친다. `/sync-docs` 로 따로 점검할 수 있다.
- PR에는 항상 개발일지를 함께 올린다. `/pre-pr` 이 `/devlog` 절차로 `docs/devlog/`에 일지를 쓰고 같은 PR 브랜치에 커밋한다. 따로 `/devlog` 를 실행해도 현재 PR 브랜치에 커밋하고, 팀원에게 보낼 요약을 만든다. 세션 시작 시 훅이 최근 일지의 "다음 할 일"과 열린 PR을 알려준다.
- 필요한 도구: `jq`(훅), `python3`(테스트 context 가드 훅), `gh`(PR·리뷰어 지정, `gh auth login`), Node 22.12 이상(`front/`). Windows는 훅을 Git Bash로 실행하고, `python3` 명령이 실제 Python을 가리켜야 한다(`.claude/README.md` 「처음 설정」).
