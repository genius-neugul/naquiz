---
name: devlog
description: 오늘 한 작업을 개발일지로 정리해 docs/devlog/ 에 남기고 팀원에게 보낼 요약을 만든다. "개발일지", "오늘 작업 정리", "인수인계", "팀원한테 공유", /devlog 요청 시 사용.
---

# 개발일지 / 인수인계

두 명이 같은 레포에서 작업하므로, 일지는 레포 안(`docs/devlog/`)에 남겨 서로 pull 해서 보게 한다.
세션 시작 훅이 최근 일지의 "다음 할 일"을 자동으로 읽어준다.

## 1. 재료 모으기 (직접 추측하지 말 것)
- 작성자: `git config user.name`
- 오늘 커밋: `git log --since=midnight --author="$(git config user.name)" --oneline --all`
- 변경 규모: `git diff --stat <오늘 첫 커밋>^..HEAD`
- 열린 PR / 리뷰 요청: `gh pr list --author @me`, `gh pr list --search "review-requested:@me"`
- 이 대화에서 내린 결정, 막힌 부분

## 2. 파일 작성: `docs/devlog/YYYY-MM-DD-<작성자>.md`
같은 날 파일이 있으면 이어서 쓴다.

```markdown
# YYYY-MM-DD <작성자>

## 한 일
- 기능 단위로 2~5개, 관련 PR/커밋 링크

## 결정한 것
- 무엇을, 왜 (대안이 있었다면 왜 버렸는지) — 나중에 면접/회고 때 쓸 수 있게

## 막힌 것 / 팀원 확인 필요
- @팀원 에게 묻거나 부탁할 것

## 다음 할 일
- [ ] 우선순위 순으로
```

## 3. 공유
- `docs: devlog YYYY-MM-DD` 로 커밋한다(PR 없이 main에 바로 올릴지 사용자에게 물어본다).
- 채팅(카톡/디스코드/슬랙)에 붙여넣을 3~5줄 요약을 대화에 출력한다: 한 일 한 줄, 팀원 확인 필요 사항, 내일 할 일.

## 주간 회고 (인자로 `week` 가 주어지면)
최근 7일 `docs/devlog/*.md` 를 모두 읽고 `docs/devlog/weekly/YYYY-Www.md` 로 두 사람 작업을 합쳐 정리한다: 완료 기능, 주요 결정, 남은 리스크, 다음 주 목표.
