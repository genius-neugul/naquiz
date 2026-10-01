# .claude — 팀 공용 Claude Code 설정

이 폴더는 두 사람이 같은 방식으로 Claude Code를 쓰기 위한 설정이다. 커밋해서 공유하고, 개인 설정은 `.claude/settings.local.json`에 둔다(Claude Code가 자동으로 git에서 제외한다).

```
.claude/
├── settings.json          훅 등록
├── hooks/
│   ├── session-brief.sh   세션 시작 브리핑
│   ├── pr-gate.sh         PR 생성 게이트
│   └── test-context-guard.py  테스트 context 가드
├── skills/
│   ├── pre-pr/            /pre-pr     PR 전 점검 → PR 생성
│   ├── sync-docs/         /sync-docs  코드와 문서 동기화
│   └── devlog/            /devlog     개발일지·인수인계
└── agents/
    ├── test-runner.md     core/ Gradle 테스트 실행 (Haiku)
    ├── code-reviewer.md   core/ 서버 변경분 리뷰 (Sonnet)
    └── docs-syncer.md     문서 동기화 (Sonnet)
```

## 처음 설정

```bash
brew install jq gh   # jq: 훅이 사용, gh: PR 생성·열린 PR 조회
python3 --version    # 테스트 context 가드 훅이 사용 (macOS 기본 포함)
gh auth login
```

- 저장소 폴더에서 Claude Code를 처음 열면 이 폴더를 신뢰할지 묻는다. 허용해야 훅이 동작한다. 훅만 따로 묻지는 않는다.
- 훅은 `bash`·`python3`로 실행하므로 스크립트에 실행 권한이 없어도 동작한다.
- `jq`가 없으면 PR 게이트가 에러로 끝나면서 PR 생성을 **막지 못한다**. 꼭 설치한다.

### Windows

```bash
winget install jqlang.jq GitHub.cli
python3 --version    # Python 3 버전이 나와야 한다
gh auth login
```

- 훅은 Git Bash(Git for Windows)로 실행된다.
- python.org 설치판은 `python3` 명령을 만들지 않는다. `python3`가 exit 49로 끝나거나 Microsoft Store 설치를 안내하면 Store 판 Python을 설치한다. Store 판은 `python3`를 제공한다.

## 하루 작업 흐름

1. **세션 시작**: 훅이 브랜치, 최근 커밋, 열린 PR, 최근 개발일지의 "다음 할 일"을 Claude에게 알려준다.
2. **작업**: `feat/<기능>` 브랜치에서 작업한다. 테스트는 `test-runner` 에이전트가 돌린다.
3. **PR**: `/pre-pr`를 실행한다. 테스트, 리뷰, 문서 점검을 통과해야 PR이 만들어진다. 개발일지도 이때 같은 PR 브랜치에 커밋된다.
4. **마무리**: 팀원에게 보낼 요약을 받는다. PR 없이 일지만 갱신하려면 `/devlog`를 실행한다(현재 PR 브랜치에 커밋).

## 스킬 (슬래시 명령)

"PR 올려줘"처럼 말로 요청해도 Claude가 알아서 해당 스킬을 쓴다.

### `/pre-pr`: PR 전 점검 후 PR 생성

| 단계 | 내용 |
|---|---|
| 1. 준비 | 이번 작업(대화 context)과 무관한 변경은 묻지 않고 `git stash`로 치워둔다(`pre-pr: 작업 외 변경 임시 보관`). stash 전에 `git restore --staged .`로 staging 영역을 비운다(stash가 staging 전체를 저장해 pop 때 충돌하는 것을 막는다). 작업한 변경은 커밋할지 묻는다. `origin`과 충돌하는지 확인한다. |
| 2. 병렬 점검 | `test-runner`, `code-reviewer`, `docs-syncer`를 동시에 실행한다. `core/` 코드(`src/`, Gradle 설정) 변경이 없으면 `test-runner`를, `core/` 변경이 없으면 `code-reviewer`를 건너뛴다. |
| 3. 결과 처리 | 🔴 Critical이나 테스트 실패가 있으면 고친 뒤 2단계를 다시 한다. 🟡 Warning은 PR 본문 "리뷰어에게"에 적는다. 문서를 고쳤으면 `docs: sync with <기능>`으로 커밋한다. |
| 4. 개발일지 | `/devlog` 절차로 오늘 일지를 쓰거나 이어서 쓰고, `docs: devlog YYYY-MM-DD`로 **같은 PR 브랜치에** 커밋한다. |
| 5. 통과 기록 | HEAD 커밋 해시를 `.git/claude-pr-ready`에 적는다. |
| 6. PR 생성 | push한 뒤 템플릿(무엇을 / 왜 / 확인한 것 / 리뷰어에게)으로 `gh pr create`를 실행하고 팀원을 리뷰어로 지정한다. 만든 PR 링크를 일지에 채워 다시 커밋·push한다. |
| 7. stash 복원 | 1단계에서 stash 했으면 `git stash pop`으로 되돌린다. 중간에 멈추거나 PR 생성이 실패해도 끝내기 전에 복원한다. |

### `/sync-docs [범위]`: 코드와 문서 동기화

- `docs-syncer`에게 맡기고, 고친 문서는 `git diff docs/`로 보여준 뒤 확인을 받고 `docs:`로 커밋한다.
- 인자를 주면 그 문서나 기능만 본다(예: `/sync-docs 크롤러`).
- "기획과 구현 불일치"가 나오면 코드가 틀린 건지 기획이 바뀐 건지 사용자에게 묻는다. 기획이 바뀐 것이면 `docs/기획.md`나 `docs/DOMAIN.md`를 고치고, 커밋 메시지와 PR 본문에 `(기획 변경)`을 적는다.

### `/devlog [week]`: 개발일지·인수인계

- 오늘 커밋, 변경 규모, 열린 PR, 대화에서 내린 결정을 모아 `docs/devlog/YYYY-MM-DD-<작성자>.md`를 쓴다. 같은 날 파일이 있으면 이어서 쓴다.
- 섹션: 한 일 / 결정한 것 / 막힌 것·팀원 확인 필요 / 다음 할 일. 세션 시작 훅은 "다음 할 일"을 읽어 가므로 섹션 이름을 바꾸지 않는다.
- "한 일"·"결정한 것" 항목(과 문서·코드에 걸린 "막힌 것" 항목)에는 내용이 반영된 곳(문서 절 앵커, 코드 파일·줄, 커밋, PR)의 링크를 건다. 반영된 곳이 없으면 "(미반영)"이라고 적는다.
- `docs: devlog YYYY-MM-DD`로 현재 PR 브랜치에 커밋하고(main에는 올리지 않는다), 채팅에 붙여넣을 3~5줄 요약을 출력한다.
- 같은 날 여러 PR 브랜치에 같은 일지 파일이 올라가면 머지할 때 충돌할 수 있다. 나중에 머지하는 PR에서 두 내용을 합친다.
- `/devlog week`: 최근 7일 일지를 합쳐 `docs/devlog/weekly/YYYY-Www.md`로 주간 회고를 쓴다.

## 에이전트

스킬이 호출하는 하위 에이전트다. 직접 부를 수도 있다(예: "test-runner로 테스트 돌려줘").

| 에이전트 | 모델 | 수정 권한 | 하는 일 |
|---|---|---|---|
| `test-runner` | Haiku (effort low) | 없음 | `core/` Spring 서버 테스트(Gradle)를 실행하고 **실패만** 요약한다. `core/` 코드 변경이 없으면 실행하지 않는다. 메인 대화 토큰을 아끼기 위해 테스트는 항상 이 에이전트로 돌린다. |
| `code-reviewer` | Sonnet | 코드 수정 안 함 | `core/` Spring 게임 서버 변경분만 리뷰한다(크롤러는 대상 아님). 게임 규칙 준수, 서버 단일 진실 원천, 서버 타이머, 동시 정답 race condition, 유비쿼터스 언어, `core/docs/` 컨벤션(레이어·예외·로그)을 보고, 신뢰도 70 이상만 🔴/🟡/💡/❓로 보고한다. 반복되는 패턴은 프로젝트 메모리에 쌓는다. |
| `docs-syncer` | Sonnet | 문서만 | 변경분이 영향을 주는 문서를 찾는다. 규칙 문서(기획, DOMAIN, MUSIC_PARSING_RULE, MUSIC_SELECTION_RULE, `core/docs/`)는 **보고만** 하고, 사실 기록 문서(CLAUDE.md, `core/CLAUDE.md`, `front/CLAUDE.md`, `front/README.md`, `initial_crawler/README.md`, `initial_crawler/CLAUDE.md`)는 코드에 맞춰 고친다. |

## 훅

`settings.json`에 등록돼 있다.

### SessionStart → `session-brief.sh`

- 세션을 새로 열거나 이어서 열 때 실행된다(`startup|resume`). 출력이 Claude 컨텍스트에 들어간다.
- 알려주는 것: 현재 브랜치, `origin` 기본 브랜치 대비 앞섬·뒤처짐, 최근 커밋 5개, 열린 PR 5개(`gh`가 있을 때), 최근 개발일지 2개의 "다음 할 일".
- `git fetch`를 하므로 네트워크가 느리면 시작이 조금 늦어질 수 있다(제한 시간 15초).

### PreToolUse → `pr-gate.sh`

- `gh pr create ...` Bash 명령과 GitHub MCP의 `create_pull_request` 호출을 가로챈다.
- Bash 명령은 내용에 `gh pr create`가 있을 때만 검사하고, 없으면 통과시킨다. MCP 호출은 항상 검사한다. ([트러블슈팅](../docs/troubleshooting/2026-09-29-pr-gate-관계없는-명령-차단.md))
- 다음 경우에는 PR 생성을 막고 이유를 Claude에게 돌려준다.
  - 커밋이 하나도 없다.
  - 추적 중인 파일에 커밋 안 된 변경이 있다.
  - `/pre-pr` 통과 기록(`.git/claude-pr-ready`)이 없다.
  - 기록된 커밋과 현재 HEAD가 다르다. 점검 후 새 커밋이 생긴 경우라 `/pre-pr`을 다시 해야 한다.
- 통과 기록은 `.git/` 안에 있어서 커밋되지 않는다. 각자의 로컬에서만 유효하다.
- 이 게이트는 Claude Code 안에서만 동작한다. 터미널에서 직접 `gh pr create`를 치면 막지 않는다.

### PreToolUse → `test-context-guard.py`

- Claude가 `Write`·`Edit`로 `core/src/test/**/*.java`를 쓰기 전에 검사한다. 공통 상위 클래스를 두는 `core/src/test/java/geniusneugul/project/core/support/` 아래 파일과 `core/src/test` 밖의 파일은 통과시킨다.
- Bash(heredoc, sed 등)로 쓴 파일은 검사하지 못한다. 그래서 `core/CLAUDE.md`에 테스트 파일은 Write/Edit로만 쓴다는 규칙을 두었다.
- 이번 쓰기로 다음 패턴이 **새로 생기면** 막고 이유를 Claude에게 돌려준다. 기존 파일에 이미 있던 것은 막지 않는다.
  - `@DirtiesContext`
  - 테스트 클래스에 직접 붙인 `@SpringBootTest`
  - 통합 테스트 클래스(`@SpringBootTest`, `IntegrationTestSupport`·`WebSocketTestSupport` 상속) 안의 `@MockitoBean`·`@MockitoSpyBean`·`@MockBean`
- 클래스별 `@TestPropertySource`·`@ActiveProfiles`는 필요할 수 있어 검사하지 않고 리뷰에서 본다.
- 규칙 근거는 `core/docs/TEST.md` 「Spring Context 재사용」이다. 사람이 IDE에서 직접 고치는 것은 막지 않는다.

## 문제 해결

| 증상 | 원인 / 해결 |
|---|---|
| "PR 전 점검이 아직 안 됐습니다" | `/pre-pr`을 실행한다. |
| "마지막 /pre-pr 점검 이후 새 커밋이 생겼습니다" | 점검 뒤에 커밋했다. `/pre-pr`을 다시 실행한다(고친 부분만 다시 봐도 된다). |
| 세션 시작 브리핑이 안 나옴 | 저장소 폴더를 신뢰했는지 확인한다. 훅은 `bash`로 실행하므로 실행 권한은 상관없다. `/hooks`로 등록 상태를 볼 수 있다. |
| "테스트 context 가드: ..." | 통합 테스트가 context를 새로 띄우는 패턴을 쓰려 했다. `IntegrationTestSupport`를 상속하고 mock은 그 클래스에 선언한다. |
| `/pre-pr` 뒤 작업 외 변경이 사라짐 / stash pop 충돌 | `git stash list`에서 `pre-pr: 작업 외 변경 임시 보관`을 찾아 `git stash pop stash@{n}`으로 복원한다. 충돌이면 충돌 파일을 직접 정리한 뒤 `git stash drop`한다. |
| 열린 PR이 브리핑에 안 나옴 | `gh auth status`로 로그인 상태를 확인한다. |
| 훅이 `EFTYPE: inappropriate file type or format, uv_spawn`으로 실패 | 훅에 `args`가 있어 스크립트 파일을 직접 실행하려 했다. Windows는 shebang을 읽지 못한다. `args`를 빼고 `bash "..."`·`python3 "..."`로 등록한다. ([트러블슈팅](../docs/troubleshooting/2026-10-01-windows-훅-실행-실패.md)) |
| 테스트 context 가드가 동작하지 않음 (Windows) | `python3`가 Microsoft Store 리다이렉터다. 위 "처음 설정 > Windows"대로 Store 판 Python을 설치한다. |

## 수정할 때

- 스킬: `skills/<이름>/SKILL.md`. frontmatter의 `description`이 자동 호출 조건이므로 트리거 문구를 함께 적는다.
- 에이전트: `agents/<이름>.md`. `model`, `tools`, `effort`를 frontmatter에서 정한다.
- 훅: `settings.json`에 등록하고 스크립트는 `hooks/`에 둔다. 스크립트는 stdin으로 JSON을 받으므로 쓰지 않더라도 읽어서 비워야 한다.
- 훅 명령은 `args` 없이 `bash "$CLAUDE_PROJECT_DIR/.claude/hooks/x.sh"`, `python3 "$CLAUDE_PROJECT_DIR/.claude/hooks/x.py"`처럼 인터프리터를 앞에 적는다. `args`를 쓰면 셸 없이 파일을 직접 실행해서(exec form) Windows에서 `.sh`·`.py`를 실행하지 못한다. `args`가 없으면 macOS는 `sh -c`, Windows는 Git Bash로 실행된다.
- 바꾼 뒤에는 이 문서와 CLAUDE.md의 "협업 흐름"도 함께 고친다.
