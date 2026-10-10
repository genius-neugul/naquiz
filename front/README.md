# front

퀴즈방 프론트엔드. npm workspaces 모노레포로 게임 앱과 백오피스 앱, 둘이 함께 쓰는 패키지를 담는다.

| 워크스페이스 | 경로 | 설명 |
| --- | --- | --- |
| `@naquiz/game` | `apps/game` | 게임 앱. 홈(방 만들기·참가), 대기실, 게임 진행(노래·영화 스무고개·스틸컷), 채팅, 결과 |
| `@naquiz/admin` | `apps/admin` | 백오피스 앱. 문제별 통계, 문제 오류 신고 처리 |
| `@naquiz/ui` | `packages/ui` | 디자인 토큰, 라이트/다크 테마, 공용 컴포넌트 |
| `@naquiz/shared` | `packages/shared` | 도메인 타입·상수, 정답 판정·힌트 마스킹·투표 통과 판정 순수 함수 |

## 실행

Node 22.12 이상이 필요하다.

```bash
cd front
npm install
npm run dev:game     # http://localhost:5173
npm run dev:admin    # http://localhost:5174
```

빌드·검사:

```bash
npm run build        # 두 앱 타입 검사 + 빌드 (apps/*/dist)
npm run typecheck
npm run lint
npm test
```

## Mock 클라이언트

게임 서버의 실시간 API가 아직 없어서 두 앱 모두 Mock 클라이언트로 동작한다.

- 게임 앱 `MockGameClient`: 방을 만들면 봇 7명이 들어온다. 봇은 잡담, 오답·정답 입력, 힌트·스킵 투표, 스무고개 단서 선택을 한다. 스무고개에서는 차례인 참가자가 정답 힌트를 열고, 스틸컷은 정답 일부 공개(`ANSWER_PARTIAL`) 힌트를 쓴다. 참가하기로 들어가면 방장 봇이 게임을 골라 시작한다. 초대 코드는 숫자·영문 6자리면 아무 값이나 된다.
- 백오피스 앱 `MockAdminClient`: 고정 시드로 만든 문제 통계와 샘플 신고를 보여준다. 게임 앱과 데이터를 공유하지 않는다.

서버 API가 생기면 `apps/game/src/game/GameClient.ts`, `apps/admin/src/admin/AdminClient.ts` 인터페이스의 실제 구현을 만들어 `main.tsx`에서 바꿔 끼운다.
