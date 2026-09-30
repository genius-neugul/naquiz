---
name: pre-pr
description: PR을 올리기 전에 테스트·코드리뷰·문서 동기화를 한 번에 돌리고, 개발일지를 같은 브랜치에 커밋한 뒤 PR을 생성한다. "PR 올려줘", "PR 만들어줘", /pre-pr 요청 시 사용.
---

# PR 전 점검 후 PR 생성

`gh pr create` 는 PR 게이트 훅이 막고 있다. 이 스킬을 끝까지 통과해야 PR을 만들 수 있다.

## 1. 준비
1. `git status --porcelain` 으로 변경 파일을 보고, 이번 대화(작업 context)에서 만들거나 고친 파일과 그 외 파일로 나눈다.
2. 그 외 파일이 있으면 사용자에게 묻지 않고 stash 한다. **stash 전에 staging 영역을 먼저 비운다.**
   ```bash
   git restore --staged .
   git stash push --include-untracked -m "pre-pr: 작업 외 변경 임시 보관" -- <작업 외 파일들>
   ```
   - 경로를 지정해도 stash 는 staging 영역 전체를 함께 저장한다. staging 에 옛 버전이 올라간 작업 파일이 있으면 pop 할 때 커밋된 새 버전과 충돌한다. 그래서 먼저 비운다(내용은 워킹트리에 그대로 남는다).
   - 이 때문에 작업 외 파일의 staged 상태는 pop 뒤 unstaged 로 돌아온다. 내용은 바뀌지 않는다.
   - `-a` 는 쓰지 않는다(무시 파일까지 stash 되지 않게).
   - stash 한 파일 목록을 한 줄로 알린다(허락은 구하지 않는다).
3. 작업 context 변경은 사용자에게 커밋할지 물어본다(메시지는 Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`). `main` 이면 규칙대로 브랜치를 먼저 만든다.
4. `git fetch origin` 후 base 브랜치(`origin/HEAD`, 보통 main/develop)와 충돌 여부만 확인한다.

## 2. 병렬 점검 — 해당하는 에이전트를 한 번에 띄운다
- `test-runner` (Haiku, 낮은 effort): `core/` Spring 서버 테스트. `git diff --name-only origin/HEAD...HEAD -- core/src core/build.gradle core/settings.gradle core/gradle` 가 비어 있으면 띄우지 않는다(문서·크롤러·`.claude/`만 바뀐 PR은 테스트 없음).
- `code-reviewer` (Sonnet): `core/` Spring 서버 변경분 리뷰. `git diff --name-only origin/HEAD...HEAD -- core/` 가 비어 있으면 띄우지 않는다(크롤러·문서만 바뀐 PR은 자동 리뷰 없음).
- `docs-syncer` (Sonnet): 문서 동기화

메인 대화에서 테스트 로그를 직접 읽지 않는다. 결과 요약만 받는다.

## 3. 결과 처리
- 🔴 Critical 리뷰 이슈나 테스트 실패가 있으면: 사용자에게 요약을 보여주고, 고칠지 물어본 뒤 고친다 → 커밋 → **2단계를 다시** 실행한다(고친 부분만 다시 봐도 됨).
- 🟡 Warning 은 PR 본문 "리뷰어에게" 섹션에 옮겨 적고 진행해도 된다.
- docs-syncer 가 문서를 고쳤으면 `docs: sync with <기능>` 으로 별도 커밋한다.
- "기획과 구현 불일치" 보고는 사용자에게 꼭 알린다.

## 4. 개발일지
PR에는 항상 개발일지를 함께 올린다.
- `/devlog` 스킬 절차(1~2단계)대로 `docs/devlog/YYYY-MM-DD-<작성자>.md`를 쓰거나 이어서 쓴다. 이번 PR의 한 일·결정을 담고, PR이 아직 없으므로 PR 링크 자리는 5단계에서 채운다.
- `docs: devlog YYYY-MM-DD` 로 **이 PR 브랜치에** 커밋한다. main에 따로 올리지 않는다.
- 이미 만든 PR에 커밋을 더하는 경우에도 같은 날 일지를 갱신해 같은 브랜치에 커밋한다.

## 5. 통과 기록
모든 점검이 통과하고 워킹트리가 깨끗하면:
```bash
git rev-parse HEAD > "$(git rev-parse --git-dir)/claude-pr-ready"
```

## 6. PR 생성
`git push -u origin HEAD` 후 `gh pr create` 로 만든다. PR을 만든 뒤 개발일지의 PR 링크를 채워 `docs: devlog YYYY-MM-DD` 로 커밋·push 한다(PR 본문과 일지만 바뀐 커밋이라 점검을 다시 돌리지 않는다). 본문 템플릿:

```markdown
## 무엇을
- (변경 요약 2~4줄)

## 왜
- (관련 이슈 #번호, 기획 문서 항목)

## 확인한 것
- 테스트: ✅ N개 통과 (core/ 코드 변경이 없으면 "해당 없음")
- 자동 리뷰: Critical 0 / Warning N (core/ 변경이 없으면 "해당 없음")
- 문서: (수정한 문서 목록 또는 "변경 없음")
- 개발일지: docs/devlog/YYYY-MM-DD-<작성자>.md

## 리뷰어에게
- (특히 봐줬으면 하는 부분, Warning 항목, 기획과 다르게 구현한 부분)
```
팀원을 리뷰어로 지정한다(`--reviewer`; 아이디를 모르면 물어본다).

## 7. stash 복원
- 1단계에서 stash 했으면 `git stash list` 에서 `pre-pr: 작업 외 변경 임시 보관` 항목을 찾아 `git stash pop stash@{n}` 한다.
- PR 생성이 실패하거나 3단계에서 중단하더라도 **스킬을 끝내기 전에 반드시 pop** 한다.
- pop 이 충돌하면(docs-syncer 가 같은 문서를 고친 경우 등) 억지로 해결하지 않고 멈춘 뒤, 충돌 파일과 stash 가 남아 있다는 것을 알린다.
