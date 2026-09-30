# .claude — 팀 공용 Claude Code 설정

이 폴더는 두 사람이 같은 방식으로 Claude Code를 쓰기 위한 설정이다. 커밋해서 공유하고, 개인 설정은 `.claude/settings.local.json`에 둔다(Claude Code가 자동으로 git에서 제외한다).

```
.claude/
├── settings.json          훅 등록
├── hooks/
│   ├── session-brief.sh   세션 시작 브리핑
│   └── pr-gate.sh         PR 생성 게이트
├── skills/
│   ├── pre-pr/            /pre-pr     PR 전 점검 → PR 생성
│   ├── sync-docs/         /sync-docs  코드와 문서 동기화
│   └── devlog/            /devlog     개발일지·인수인계
└── agents/
    ├── test-runner.md     테스트 실행 (Haiku)
    ├── code-reviewer.md   변경분 리뷰 (Sonnet)
    └── docs-syncer.md     문서 동기화 (Sonnet)
```

## 처음 설정

```bash
brew install jq gh   # jq: 훅이 사용, gh: PR 생성·열린 PR 조회
gh auth login
```

- 저장소를 처음 열면 Claude Code가 프로젝트 훅을 믿을지 묻는다. 허용해야 훅이 동작한다.
- 훅 스크립트에는 실행 권한이 필요하다. 권한이 빠졌으면 `chmod +x .claude/hooks/*.sh`로 준다.
- `jq`가 없으면 PR 게이트가 에러로 끝나면서 PR 생성을 **막지 못한다**. 꼭 설치한다.

## 하루 작업 흐름

1. **세션 시작**: 훅이 브랜치, 최근 커밋, 열린 PR, 최근 개발일지의 "다음 할 일"을 Claude에게 알려준다.
2. **작업**: `feat/<기능>` 브랜치에서 작업한다. 테스트는 `test-runner` 에이전트가 돌린다.
3. **PR**: `/pre-pr`를 실행한다. 테스트, 리뷰, 문서 점검을 통과해야 PR이 만들어진다.
4. **마무리**: `/devlog`로 `docs/devlog/`에 일지를 남기고, 팀원에게 보낼 요약을 받는다.

## 스킬 (슬래시 명령)

"PR 올려줘"처럼 말로 요청해도 Claude가 알아서 해당 스킬을 쓴다.

### `/pre-pr`: PR 전 점검 후 PR 생성

| 단계 | 내용 |
|---|---|
| 1. 준비 | 커밋 안 된 변경이 있으면 커밋할지 묻는다. `origin`과 충돌하는지 확인한다. |
| 2. 병렬 점검 | `test-runner`, `code-reviewer`, `docs-syncer`를 동시에 실행한다. |
| 3. 결과 처리 | 🔴 Critical이나 테스트 실패가 있으면 고친 뒤 2단계를 다시 한다. 🟡 Warning은 PR 본문 "리뷰어에게"에 적는다. 문서를 고쳤으면 `docs: sync with <기능>`으로 커밋한다. |
| 4. 통과 기록 | HEAD 커밋 해시를 `.git/claude-pr-ready`에 적는다. |
| 5. PR 생성 | push한 뒤 템플릿(무엇을 / 왜 / 확인한 것 / 리뷰어에게)으로 `gh pr create`를 실행하고 팀원을 리뷰어로 지정한다. |

### `/sync-docs [범위]`: 코드와 문서 동기화

- `docs-syncer`에게 맡기고, 고친 문서는 `git diff docs/`로 보여준 뒤 확인을 받고 `docs:`로 커밋한다.
- 인자를 주면 그 문서나 기능만 본다(예: `/sync-docs 크롤러`).
- "기획과 구현 불일치"가 나오면 코드가 틀린 건지 기획이 바뀐 건지 사용자에게 묻는다. 기획이 바뀐 것이면 `docs/기획.md`나 `docs/DOMAIN.md`를 고치고, 커밋 메시지와 PR 본문에 `(기획 변경)`을 적는다.

### `/devlog [week]`: 개발일지·인수인계

- 오늘 커밋, 변경 규모, 열린 PR, 대화에서 내린 결정을 모아 `docs/devlog/YYYY-MM-DD-<작성자>.md`를 쓴다. 같은 날 파일이 있으면 이어서 쓴다.
- 섹션: 한 일 / 결정한 것 / 막힌 것·팀원 확인 필요 / 다음 할 일. 세션 시작 훅은 "다음 할 일"을 읽어 가므로 섹션 이름을 바꾸지 않는다.
- "한 일"·"결정한 것" 항목(과 문서·코드에 걸린 "막힌 것" 항목)에는 내용이 반영된 곳(문서 절 앵커, 코드 파일·줄, 커밋, PR)의 링크를 건다. 반영된 곳이 없으면 "(미반영)"이라고 적는다.
- `docs: devlog YYYY-MM-DD`로 커밋하고, 채팅에 붙여넣을 3~5줄 요약을 출력한다.
- `/devlog week`: 최근 7일 일지를 합쳐 `docs/devlog/weekly/YYYY-Www.md`로 주간 회고를 쓴다.

## 에이전트

스킬이 호출하는 하위 에이전트다. 직접 부를 수도 있다(예: "test-runner로 테스트 돌려줘").

| 에이전트 | 모델 | 수정 권한 | 하는 일 |
|---|---|---|---|
| `test-runner` | Haiku (effort low) | 없음 | 변경된 모듈의 테스트를 빌드 도구(Gradle, Maven, npm, pytest)를 감지해 실행하고 **실패만** 요약한다. 메인 대화 토큰을 아끼기 위해 테스트는 항상 이 에이전트로 돌린다. |
| `code-reviewer` | Sonnet | 코드 수정 안 함 | 변경분을 리뷰한다. 게임 규칙 준수, 서버 단일 진실 원천, 서버 타이머, 동시 정답 race condition, 유비쿼터스 언어, 크롤러 결정성을 보고, 신뢰도 70 이상만 🔴/🟡/💡/❓로 보고한다. 반복되는 패턴은 프로젝트 메모리에 쌓는다. |
| `docs-syncer` | Sonnet | 문서만 | 변경분이 영향을 주는 문서를 찾는다. 규칙 문서(기획, DOMAIN, MUSIC_PARSING_RULE, MUSIC_SELECTION_RULE)는 **보고만** 하고, 사실 기록 문서(CLAUDE.md, `initial_crawler/README.md`, `initial_crawler/CLAUDE.md`)는 코드에 맞춰 고친다. |

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

## 문제 해결

| 증상 | 원인 / 해결 |
|---|---|
| "PR 전 점검이 아직 안 됐습니다" | `/pre-pr`을 실행한다. |
| "마지막 /pre-pr 점검 이후 새 커밋이 생겼습니다" | 점검 뒤에 커밋했다. `/pre-pr`을 다시 실행한다(고친 부분만 다시 봐도 된다). |
| 세션 시작 브리핑이 안 나옴 | 프로젝트 훅을 허용했는지, 스크립트에 실행 권한이 있는지 확인한다. `/hooks`로 등록 상태를 볼 수 있다. |
| 열린 PR이 브리핑에 안 나옴 | `gh auth status`로 로그인 상태를 확인한다. |

## 수정할 때

- 스킬: `skills/<이름>/SKILL.md`. frontmatter의 `description`이 자동 호출 조건이므로 트리거 문구를 함께 적는다.
- 에이전트: `agents/<이름>.md`. `model`, `tools`, `effort`를 frontmatter에서 정한다.
- 훅: `settings.json`에 등록하고 스크립트는 `hooks/`에 둔다. 스크립트는 stdin으로 JSON을 받으므로 쓰지 않더라도 읽어서 비워야 한다.
- 바꾼 뒤에는 이 문서와 CLAUDE.md의 "협업 흐름"도 함께 고친다.
