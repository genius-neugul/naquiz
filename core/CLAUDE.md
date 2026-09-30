# core/CLAUDE.md

게임 서버(`core/`) 작업 시 참고한다. 게임 규칙과 용어는 `../docs/기획.md`, `../docs/DOMAIN.md`가 기준이고, 이 파일과 `docs/`가 다르면 **문서가 우선**이다.

## 참고 문서

- `docs/ARCHITECTURE.md`: 도메인 중심 레이어드 아키텍처. 패키지 구조, 의존성 방향, 레이어별 작성 규칙, 트랜잭션 경계, 이벤트 발행 규약
- `docs/CODE_STYLE.md`: 네이밍, Spring Bean 접미사, Lombok, 주석·상수 규칙
- `docs/EXCEPTION.md`: 에러 응답 형식, ErrorCode, BusinessException, 예외를 던지는 위치
- `docs/LOG.md`: 로그 prefix, 레벨, 로그를 남기는 위치, 민감 정보, Trace ID
- `docs/TEST.md`: 테스트 계층, 네이밍, Given-When-Then, Fixture, Assertion, Mock
- `docs/review/`: 컨벤션 검토 기록. 사용자가 고른 선택지와 메모가 남아 있다
- `../docs/API.md`: HTTP API 요청/응답 규격
- `../docs/DOMAIN.md`: 유비쿼터스 언어, 애그리거트, 정답 판정·마스킹·투표 규칙
- `../docs/MUSIC_SELECTION_RULE.md`: 게임에서 Spotify 곡 정보 조회와 YouTube 영상 선택 규칙

## 기술 스택

- Java 25, Spring Boot 4.1.1, Gradle (wrapper 포함)
- 테스트: JUnit 5 (`spring-boot-starter-test`)
- 실시간 통신:
- DB / 캐시: 미정 (진행 중인 게임 상태를 DB·메모리·Redis 중 어디에 둘지 포함)
- 인증: 없음. 참가자는 비회원 게스트이고, 참가자 식별 방식(토큰 등)은 미정
- 기본 패키지: `geniusneugul.project.core`

### Spring Boot 4 주의사항

- **Java 25 문법 범위를 넘지 않는다.** 상위 버전 문법을 쓰지 않는다.
- Spring Boot 3.x 기준의 블로그·예제 코드를 그대로 복사하지 않는다.

## 자주 쓰는 명령

`core/` 에서 실행한다. 테스트는 메인 대화에서 직접 돌리지 않고 `test-runner` 에이전트에 맡긴다.

```bash
./gradlew build      # 컴파일 + 테스트
./gradlew test       # 테스트만
./gradlew bootRun    # 로컬 실행
```

## 디렉터리 구조

지금은 단일 모듈이다. 멀티모듈(core-domain, game-api, admin-api, crawler-batch)로 나누는 목표 구조는 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md#모듈-구조)에 있다.

```
core/
├── docs/              서버 코드 컨벤션 (아키텍처, 스타일, 예외, 로그, 테스트)
├── build.gradle
└── src/
    ├── main/java/geniusneugul/project/core/   도메인별 패키지 (docs/ARCHITECTURE.md)
    └── test/java/geniusneugul/project/core/
        └── support/   통합 테스트 공통 상위 클래스·설정 (목록은 docs/TEST.md 「Spring Context 재사용」)
```

## 작업 규칙

- 게임 규칙(정답 판정, 마스킹, 투표)은 순수 함수로 분리하고 단위 테스트를 먼저 작성한다. 규칙을 바꾸면 테스트도 함께 바꾼다.
- 게임 상태(점수, 현재 문제, 투표 현황, 타이머)는 서버가 단일 진실 원천이다. 클라이언트는 표시만 한다.
- 타이머(10초 패스, 스틸컷 10초 교체, 시간 경과 힌트)는 서버 기준으로 돌린다. 현재 시각은 주입받은 `Clock` 빈에서 읽는다(`docs/TEST.md` 「시간 제어」).
- 테스트는 `docs/TEST.md` 「무엇을 테스트할까」 기준으로 필수만 쓴다. 단순 CRUD·위임·프레임워크 동작은 테스트하지 않는다.
- **테스트를 쓰기 전에 검증할 행위 목록과 각각을 테스트하는 이유를 사용자에게 먼저 제시하고 확인받는다.**
- 통합 테스트는 `support/`의 공통 상위 클래스를 상속해 Spring context를 재사용한다(`docs/TEST.md` 「Spring Context 재사용」). `@DirtiesContext`, 직접 붙인 `@SpringBootTest`, 테스트 클래스의 `@MockitoBean`은 훅이 쓰기 전에 막는다.
- `support/`에는 `docs/TEST.md`에 나열된 상위 클래스만 둔다. 목록 밖의 상위 클래스가 필요하면 만들기 전에 먼저 묻는다.
- `core/src/test`의 Java 파일은 Write/Edit 도구로만 쓴다. Bash(heredoc, sed, python 등)로 쓰면 테스트 context 가드 훅을 거치지 않는다.
- 클래스·메서드 이름은 `../docs/DOMAIN.md`의 유비쿼터스 언어를 따른다.
- 초기 데이터는 `../initial_crawler/data/`의 JSON만 읽는다. 크롤러 중간 산출물에 의존하지 않는다.
- 코드를 바꿔 기술 스택·명령·구조가 달라지면 이 파일을 함께 고친다. `docs/` 컨벤션과 달라지면 문서를 고치지 말고 먼저 알린다.
