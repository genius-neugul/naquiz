#!/usr/bin/env bash
# 세션 시작 시 Claude에게 현재 작업 맥락을 알려준다: 브랜치, 최근 커밋, 팀원의 열린 PR, 최근 개발일지.
# 출력(plain text)은 SessionStart에서 Claude 컨텍스트로 들어간다.
cat >/dev/null
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
git rev-parse --git-dir >/dev/null 2>&1 || exit 0

echo "## 현재 작업 맥락"
echo "- 브랜치: $(git branch --show-current 2>/dev/null)"
git fetch --quiet origin 2>/dev/null || true
BASE=$(git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null || echo origin/main)
AHEAD=$(git rev-list --count "$BASE"..HEAD 2>/dev/null || echo "?")
BEHIND=$(git rev-list --count HEAD.."$BASE" 2>/dev/null || echo "?")
echo "- $BASE 대비: ${AHEAD}개 앞섬, ${BEHIND}개 뒤처짐"

echo "- 최근 커밋:"
git log --oneline -5 2>/dev/null | sed 's/^/  - /'

if command -v gh >/dev/null 2>&1; then
  PRS=$(gh pr list --limit 5 --json number,title,author,headRefName \
        --jq '.[] | "  - #\(.number) \(.title) (@\(.author.login), \(.headRefName))"' 2>/dev/null || true)
  if [ -n "$PRS" ]; then
    echo "- 열린 PR (리뷰 필요할 수 있음):"
    echo "$PRS"
  fi
fi

LATEST=$(ls -1t docs/devlog/*.md 2>/dev/null | head -2)
if [ -n "$LATEST" ]; then
  echo "- 최근 개발일지:"
  for f in $LATEST; do
    echo "  - $f"
    # '다음 할 일' 섹션만 짧게
    awk '/^## 다음 할 일/{f=1;next} /^## /{f=0} f' "$f" | head -5 | sed 's/^/    /'
  done
fi
exit 0
