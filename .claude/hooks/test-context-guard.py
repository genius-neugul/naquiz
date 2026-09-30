#!/usr/bin/env python3
# 테스트 context 가드: Claude가 core/ 테스트 코드에 Spring context를 새로 띄우게 만드는 패턴을 쓰려 하면 쓰기 전에 막는다.
# 규칙: core/docs/TEST.md 「Spring Context 재사용」. SUPPORT_DIR 아래 공통 설정만 검사하지 않는다.
# 기존 파일에 이미 있던 위반은 막지 않고, 이번 쓰기로 새로 생긴 위반만 막는다.
import json
import re
import sys
from pathlib import Path

DOC = "core/docs/TEST.md 「Spring Context 재사용」"
# 검사하지 않는 공통 상위 클래스·설정 경로. 이 경로 하나만 예외다.
SUPPORT_DIR = "/core/src/test/java/geniusneugul/project/core/support/"

MOCK_BEAN = re.compile(r"@(MockitoBean|MockitoSpyBean|MockBean|SpyBean)\b")
INTEGRATION = re.compile(r"@SpringBootTest\b|\bextends\s+(IntegrationTestSupport|WebSocketTestSupport)\b")

RULES = [
    ("dirties",
     lambda s: re.search(r"@DirtiesContext\b", s) is not None,
     "@DirtiesContext 는 context를 버리고 다시 띄운다. 상태는 테이블·캐시 정리로 되돌린다."),
    ("springboottest",
     lambda s: re.search(r"@SpringBootTest\b", s) is not None,
     "테스트 클래스에 @SpringBootTest 를 직접 붙이지 않는다. support/ 의 공통 상위 클래스(IntegrationTestSupport, WebSocketTestSupport)를 상속한다."),
    ("mockbean",
     lambda s: INTEGRATION.search(s) is not None and MOCK_BEAN.search(s) is not None,
     "통합 테스트 클래스에 @MockitoBean·@MockitoSpyBean 을 선언하면 context가 갈린다. support/IntegrationTestSupport 에 한 번만 선언하고 테스트에서는 동작만 정한다."),
]


def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def violations(src: str) -> set:
    code = strip_comments(src)
    return {rid for rid, check, _ in RULES if check(code)}


def main() -> None:
    data = json.load(sys.stdin)
    tool = data.get("tool_name", "")
    inp = data.get("tool_input", {}) or {}
    path = inp.get("file_path", "")

    norm = path.replace("\\", "/")
    if "/core/src/test/" not in norm or not norm.endswith(".java") or SUPPORT_DIR in norm:
        return

    p = Path(path)
    before = p.read_text(encoding="utf-8") if p.is_file() else ""

    if tool == "Write":
        after = inp.get("content", "")
    elif tool == "Edit":
        old, new = inp.get("old_string", ""), inp.get("new_string", "")
        if not old:
            after = before + new
        elif inp.get("replace_all"):
            after = before.replace(old, new)
        else:
            after = before.replace(old, new, 1)
    else:
        return

    added = violations(after) - violations(before)
    if not added:
        return

    reasons = [msg for rid, _, msg in RULES if rid in added]
    reason = "테스트 context 가드: " + " / ".join(reasons) + f" (규칙: {DOC})"
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
