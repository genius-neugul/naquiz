---
name: pre-pr
description: PR을 올리기 전에 테스트·코드리뷰·문서 동기화를 한 번에 돌리고, 통과하면 PR을 생성한다. "PR 올려줘", "PR 만들어줘", /pre-pr 요청 시 사용.
---

# PR 전 점검 후 PR 생성

`gh pr create` 는 PR 게이트 훅이 막고 있다. 이 스킬을 끝까지 통과해야 PR을 만들 수 있다.

## 1. 준비
- 커밋 안 된 변경이 있으면 사용자에게 커밋할지 물어본다(메시지는 Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`).
- `git fetch origin` 후 base 브랜치(`origin/HEAD`, 보통 main/develop)와 충돌 여부만 확인한다.

## 2. 병렬 점검 — 세 에이전트를 한 번에 띄운다
- `test-runner` (Haiku, 낮은 effort): 변경된 모듈 테스트
- `code-reviewer` (Sonnet): 변경분 리뷰
- `docs-syncer` (Sonnet): 문서 동기화

메인 대화에서 테스트 로그를 직접 읽지 않는다. 결과 요약만 받는다.

## 3. 결과 처리
- 🔴 Critical 리뷰 이슈나 테스트 실패가 있으면: 사용자에게 요약을 보여주고, 고칠지 물어본 뒤 고친다 → 커밋 → **2단계를 다시** 실행한다(고친 부분만 다시 봐도 됨).
- 🟡 Warning 은 PR 본문 "리뷰어에게" 섹션에 옮겨 적고 진행해도 된다.
- docs-syncer 가 문서를 고쳤으면 `docs: sync with <기능>` 으로 별도 커밋한다.
- "기획과 구현 불일치" 보고는 사용자에게 꼭 알린다.

## 4. 통과 기록
모든 점검이 통과하고 워킹트리가 깨끗하면:
```bash
git rev-parse HEAD > "$(git rev-parse --git-dir)/claude-pr-ready"
```

## 5. PR 생성
`git push -u origin HEAD` 후 `gh pr create` 로 만든다. 본문 템플릿:

```markdown
## 무엇을
- (변경 요약 2~4줄)

## 왜
- (관련 이슈 #번호, 기획 문서 항목)

## 확인한 것
- 테스트: ✅ N개 통과
- 자동 리뷰: Critical 0 / Warning N
- 문서: (수정한 문서 목록 또는 "변경 없음")

## 리뷰어에게
- (특히 봐줬으면 하는 부분, Warning 항목, 기획과 다르게 구현한 부분)
```
팀원을 리뷰어로 지정한다(`--reviewer`; 아이디를 모르면 물어본다).
