#!/usr/bin/env bash
# PR 게이트: /pre-pr 스킬이 현재 HEAD에 대해 리뷰·테스트·문서 동기화를 끝냈을 때만 PR 생성을 허용한다.
# /pre-pr이 통과하면 .git/claude-pr-ready 에 HEAD 커밋 해시를 기록한다.
set -euo pipefail

# settings.json 의 if 조건이 복잡한 명령(반복문·파이프)을 못 거르고 훅을 부를 때가 있어서 직접 한 번 더 거른다.
# MCP create_pull_request 는 command 필드가 없으므로 항상 검사한다.
INPUT=$(cat)
TOOL=$(jq -r '.tool_name // ""' <<<"$INPUT")
if [ "$TOOL" = "Bash" ]; then
  CMD=$(jq -r '.tool_input.command // ""' <<<"$INPUT")
  grep -Eq 'gh[[:space:]]+pr[[:space:]]+create' <<<"$CMD" || exit 0
fi

deny() {
  jq -n --arg r "$1" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: $r
    }
  }'
  exit 0
}

cd "${CLAUDE_PROJECT_DIR:-.}"
git rev-parse --git-dir >/dev/null 2>&1 || exit 0   # git 저장소가 아니면 관여 안 함

GIT_DIR=$(git rev-parse --git-dir)
MARKER="$GIT_DIR/claude-pr-ready"
HEAD_SHA=$(git rev-parse --verify -q HEAD) || deny "아직 커밋이 없습니다. 먼저 커밋한 뒤 /pre-pr 을 실행하세요."

# 커밋 안 된 변경이 있으면 리뷰한 코드와 PR 코드가 달라질 수 있음
if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  deny "커밋되지 않은 변경사항이 있습니다. 커밋한 뒤 /pre-pr 을 실행하세요."
fi

if [ ! -f "$MARKER" ]; then
  deny "PR 전 점검이 아직 안 됐습니다. 먼저 /pre-pr 스킬을 실행해 테스트·코드리뷰·문서 동기화를 끝내세요."
fi

if [ "$(cat "$MARKER")" != "$HEAD_SHA" ]; then
  deny "마지막 /pre-pr 점검 이후 새 커밋이 생겼습니다. /pre-pr 을 다시 실행하세요."
fi

exit 0  # 통과: 일반 권한 흐름으로 진행
