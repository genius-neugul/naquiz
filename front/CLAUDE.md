# front/CLAUDE.md

프론트엔드(`front/`) 작업 시 참고한다. 게임 규칙과 용어는 `../docs/기획.md`, `../docs/DOMAIN.md`가 기준이고, 이 파일·디자인과 `docs/`가 다르면 **문서가 우선**이다.

## 참고 문서

- `../docs/기획.md`: 게임 종류, 게임 룰, 힌트·투표 규칙, 백오피스
- `../docs/DOMAIN.md`: 유비쿼터스 언어, 정답 판정·마스킹·투표 규칙, 오류 신고 유형
- `../docs/API.md`: HTTP API 요청/응답 규격
- 디자인 원본: [퀴즈방 · 시안 C](https://claude.ai/artifact/Hq4ZJQ85CgvRpYhMpanhqy) (Claude Design 캔버스). 화면 레이아웃·스타일은 이 디자인을, 규칙·문구는 `docs/`를 따른다

## 기술 스택

- npm workspaces 모노레포, Node 22.12 이상
- Vite 8, React 19, TypeScript 6 (strict), CSS Modules, react-router 7
- 테스트: Vitest 4 (`packages/shared` 순수 함수)
- 린트: ESLint 10 + typescript-eslint + react-hooks
- 실시간 통신: `@stomp/stompjs`로 게임 서버(`core/game-api`)의 `/ws`에 붙는다(`StompGameClient`). 지금은 방 만들기·참가하기·나가기만 서버로 처리하고, 기본 실행은 Mock 클라이언트(`MockGameClient`, `MockAdminClient`)다. 규격은 `../docs/API.md` 「실시간 메시지(STOMP) 규격」
- `.npmrc`의 `legacy-peer-deps=true`는 npm 10.9의 peer 해석 버그(vite 8 → `@vitejs/devtools` → vitest 순환에서 `edgesOut` 오류)를 피하려고 둔다. 필요한 peer는 `package.json`에 직접 적는다

## 자주 쓰는 명령

`front/`에서 실행한다.

```bash
npm install          # 처음 한 번, 의존성 설치 (워크스페이스 전체)
npm run dev:game     # 게임 앱 개발 서버 http://localhost:5173
VITE_GAME_CLIENT=stomp npm run dev:game   # 게임 서버(localhost:8080)에 붙어 실행. /ws는 Vite가 프록시한다(대상은 GAME_API_URL로 바꿀 수 있다)
npm run dev:admin    # 백오피스 앱 개발 서버 http://localhost:5174
npm run build        # 두 앱 타입 검사 + 빌드
npm run typecheck    # 모든 워크스페이스 타입 검사
npm run lint         # ESLint
npm test             # Vitest
```

## 디렉터리 구조

```
front/
├── package.json         워크스페이스 루트, 공통 devDependencies와 스크립트
├── tsconfig.base.json   strict 공통 설정
├── eslint.config.js
├── vitest.config.ts
├── apps/
│   ├── game/            @naquiz/game 게임 앱
│   │   └── src/
│   │       ├── game/        GameClient 인터페이스, 상태 타입, GameProvider(useRoom), selectors
│   │       │   ├── mock/    MockGameClient(봇 시뮬레이션), fixtures
│   │       │   └── stomp/   StompGameClient(게임 서버 STOMP 연결)
│   │       ├── components/  Header, ReportPanel(문제 오류 신고)
│   │       ├── hooks/       useNow(남은 시간 표시)
│   │       └── pages/       Home(방 만들기·참가), Room(대기실·게임 진행·채팅·결과)
│   └── admin/           @naquiz/admin 백오피스 앱
│       └── src/
│           ├── admin/       AdminClient 인터페이스, 타입, mock/
│           └── pages/       StatsPage(문제 통계·오류 신고 처리)
└── packages/
    ├── ui/              @naquiz/ui 디자인 토큰(tokens.css), 테마(useTheme), 공용 컴포넌트
    └── shared/          @naquiz/shared 게임 종류·힌트·단서·신고 타입과 상수, 정답 판정·마스킹·투표 순수 함수
```

패키지는 빌드 단계 없이 TS 소스를 그대로 export하고, 앱의 Vite가 직접 트랜스파일한다.

## 작업 규칙

- 두 앱이 함께 쓰는 코드는 앱에 복사하지 않고 `packages/ui`(시각 요소) 또는 `packages/shared`(타입·상수·순수 함수)에 둔다. 앱끼리 서로 import하지 않는다.
- 색·반경·간격·폰트는 `packages/ui/src/tokens.css`의 CSS 변수만 쓴다. 색을 하드코딩하지 않는다. 다크 모드는 토큰 재정의로만 처리한다.
- 스타일은 컴포넌트 옆 `*.module.css`에 둔다. inline style은 진행률 width처럼 런타임에 계산되는 값에만 쓴다.
- 게임 상태는 `GameClient` 인터페이스로만 읽고 바꾼다. 컴포넌트에서 게임 타이머·봇 로직을 돌리지 않는다(남은 시간 표시용 `useNow`는 예외). 서버가 생기면 게임 상태의 단일 진실 원천은 서버이고(`../core/CLAUDE.md`), 프론트는 표시만 한다. Mock 구현은 서버가 붙을 때까지의 임시 구현이다.
- 열거형 값(게임 종류, 힌트·단서 종류, 신고 유형·상태)은 `../docs/DOMAIN.md`의 코드명(`SONG`, `ANSWER_MASK` 등)을 그대로 쓴다.
- `packages/shared`의 정답 판정·마스킹·투표 함수는 `../docs/DOMAIN.md` 규칙을 따른다. 규칙을 바꾸면 테스트도 함께 바꾼다.
- 실제 `<button>`, `<label>`+`<input>`을 쓰고, 아이콘만 있는 버튼에는 `aria-label`을 단다. 아이콘은 `@naquiz/ui`의 `Icon`(인라인 stroke SVG)을 쓰고 이모지는 쓰지 않는다.
- 화면은 휴대폰 폭에서도 가로 스크롤 없이 동작해야 한다.
- 기술 스택·명령·구조가 바뀌면 이 파일을 함께 고친다.
