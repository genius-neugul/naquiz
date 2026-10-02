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

기본 실행은 두 앱 모두 Mock 클라이언트로 동작한다. 게임 서버의 실시간 API는 방 만들기·참가하기·나가기만 있어서 나머지 게임 진행은 Mock이 대신한다(서버에 붙이려면 아래 「게임 서버에 붙여 실행」).

- 게임 앱 `MockGameClient`: 방을 만들면 봇 7명이 들어온다. 봇은 잡담, 오답·정답 입력, 힌트·스킵 투표, 스무고개 단서 선택을 한다. 참가하기로 들어가면 방장 봇이 게임을 골라 시작한다. 초대 코드는 숫자·영문 6자리면 아무 값이나 된다.
- 백오피스 앱 `MockAdminClient`: 고정 시드로 만든 문제 통계와 샘플 신고를 보여준다. 게임 앱과 데이터를 공유하지 않는다.

게임 앱의 실제 구현은 `StompGameClient`(`VITE_GAME_CLIENT=stomp`)다. 백오피스 앱은 서버 API가 생기면 `apps/admin/src/admin/AdminClient.ts` 인터페이스의 실제 구현을 만들어 `main.tsx`에서 바꿔 끼운다.

## 게임 서버에 붙여 실행

게임 앱은 `StompGameClient`로 게임 서버(`core/game-api`)에 STOMP로 붙을 수 있다. 지금은 방 만들기·참가하기·나가기만 서버로 처리하고, 게임 진행은 아직 동작하지 않는다.

```bash
# core/ 에서
./gradlew :game-api:bootRun                 # http://localhost:8080
# front/ 에서
VITE_GAME_CLIENT=stomp npm run dev:game     # /ws 를 localhost:8080 으로 프록시
```

game-api를 IntelliJ나 `bootRun`으로 실행하면 게임 웹 컨테이너(`compose.game-web.yml`)가 자동으로 함께 뜬다(`../core/CLAUDE.md`). 이때는 위의 `npm run dev:game`을 따로 실행하지 않는다(5173 포트가 겹친다).

Docker로 서버와 게임 웹을 함께 띄울 수도 있다. 저장소 루트의 `compose.yml`이 game-api 이미지와 게임 웹 개발 서버(`front/Dockerfile.dev`)를 띄운다. `front/` 소스는 컨테이너에 마운트돼 고치면 바로 반영되고, 의존성은 이미지 안에 따로 설치된다.

```bash
# 저장소 루트에서
docker compose up --build                    # 웹 http://localhost:5173, 서버 http://localhost:8080
GAME_API_PORT=18080 docker compose up --build  # 8080을 이미 쓰고 있을 때
```

`package.json`이나 `package-lock.json`을 바꾸면 이미지를 다시 빌드한다(`--build`). Vite 프록시 대상은 `GAME_API_URL` 환경 변수로 정하고, compose는 `ws://game-api:8080`을 넣는다.
