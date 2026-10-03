# core/CLAUDE.md

게임 서버(`core/`) 작업 시 참고한다. 게임 규칙과 용어는 `../docs/기획.md`, `../docs/DOMAIN.md`가 기준이고, 이 파일과 `docs/`가 다르면 **문서가 우선**이다.

## 참고 문서

- `docs/ARCHITECTURE.md`: 도메인 중심 레이어드 아키텍처. 모듈 구조, 패키지 구조, 의존성 방향, 레이어별 작성 규칙, 트랜잭션 경계, 이벤트 발행 규약
- `docs/CODE_STYLE.md`: 네이밍, Spring Bean 접미사, Lombok, 주석·상수 규칙
- `docs/EXCEPTION.md`: 에러 응답 형식, ErrorCode, BusinessException, 예외를 던지는 위치
- `docs/LOG.md`: 로그 prefix, 레벨, 로그를 남기는 위치, 민감 정보, Trace ID
- `docs/TEST.md`: 테스트 계층, 네이밍, Given-When-Then, Fixture, Assertion, Mock
- `docs/review/`: 컨벤션 검토 기록. 사용자가 고른 선택지와 메모가 남아 있다
- `../docs/API.md`: HTTP API와 실시간 메시지(STOMP) 요청/응답 규격
- `../docs/DOMAIN.md`: 유비쿼터스 언어, 애그리거트, 정답 판정·마스킹·투표 규칙
- `../docs/MUSIC_SELECTION_RULE.md`: 게임에서 Spotify 곡 정보 조회와 YouTube 영상 선택 규칙

## 기술 스택

- Java 25, Spring Boot 4.1.1, Gradle (wrapper 포함)
- 테스트: JUnit 5 (`spring-boot-starter-test`)
- 실시간 통신: Spring WebSocket + STOMP(simple broker), `game-api`의 `/ws`. 규격은 `../docs/API.md` 「실시간 메시지(STOMP) 규격」
- Gradle 멀티모듈: `core-domain`(라이브러리), `game-api`, `admin-api`, `crawler-batch`(실행 앱). 역할은 `docs/ARCHITECTURE.md` 「모듈 구조」
- 영속성: Spring Data JPA. 기본(로컬)·테스트는 H2 인메모리, `local-dev` 프로필은 MySQL 8(`docker-compose.yml`). 설정은 `core-domain`의 `domain.yml` 한 벌
- 저장 위치: 통계에 쓰이거나 원래 영속 데이터인 모델(게임·라운드·공개된 힌트·공개된 단서·투표·문제·콘텐츠·오류 신고·관리자·크롤링 실행)은 DB, 방·참가자는 서버 메모리(`docs/ARCHITECTURE.md` 「Infra 작성 규칙」)
- 캐시: 없음
- Lombok
- 인증: 없음. 참가자는 비회원 게스트이고, 실시간 연결마다 서버가 붙이는 Principal 이름을 참가자 토큰으로 쓴다
- 기본 패키지: `geniusneugul.project.core`

### Spring Boot 4 주의사항

- **Java 25 문법 범위를 넘지 않는다.** 상위 버전 문법을 쓰지 않는다.
- Spring Boot 3.x 기준의 블로그·예제 코드를 그대로 복사하지 않는다.

## 자주 쓰는 명령

`core/` 에서 실행한다. 테스트는 메인 대화에서 직접 돌리지 않고 `test-runner` 에이전트에 맡긴다.

```bash
./gradlew build                      # 전 모듈 컴파일 + 테스트
./gradlew test                       # 전 모듈 테스트
./gradlew :game-api:test             # 한 모듈만
./gradlew :game-api:bootRun          # 게임 서버 로컬 실행 (H2 인메모리, 8080). 게임 웹 컨테이너도 함께 뜬다(아래)
./gradlew :admin-api:bootRun         # 관리자 API (8081)
./gradlew :crawler-batch:bootRun     # 데일리 크롤링 (웹 없음)

# MySQL로 실행 (local-dev). .env.example 을 .env 로 복사해 계정을 채운다
docker compose up -d
set -a; source .env; set +a
./gradlew :game-api:bootRun --args='--spring.profiles.active=local-dev'
```

game-api를 IntelliJ나 `bootRun`으로 실행하면 Spring Boot Docker Compose 지원(`spring-boot-docker-compose`, developmentOnly)이 루트의 `compose.game-web.yml`로 게임 웹(Vite 개발 서버, http://localhost:5173)을 함께 띄우고, 앱을 끄면 멈춘다. 게임 웹은 호스트의 game-api로 `/ws`를 프록시한다.

- Docker가 실행 중이어야 앱이 뜬다. 게임 웹 없이 서버만 띄우려면 환경 변수 `SPRING_DOCKER_COMPOSE_ENABLED=false`를 준다.
- 작업 디렉터리에서 위로 올라가며 `compose.game-web.yml`을 찾는다(`common/config/GameWebComposeFileEnvironmentPostProcessor`). 저장소 안 어디서 실행해도 되고, 못 찾으면 Docker Compose 지원을 끈다. 다른 파일을 쓰려면 `SPRING_DOCKER_COMPOSE_FILE`로 지정한다.
- 8080이 아닌 포트로 실행하면 `GAME_API_PORT`도 같은 값으로 준다(예: `GAME_API_PORT=18081`, `--server.port=18081`).
- 테스트와 실행 jar(컨테이너)에서는 이 기능이 동작하지 않는다.

## 디렉터리 구조

```
core/
├── docs/              서버 코드 컨벤션 (아키텍처, 스타일, 예외, 로그, 테스트)
├── settings.gradle    모듈 목록
├── build.gradle       모듈 공통 설정 (Java 25, Boot BOM, Lombok, 테스트)
├── docker-compose.yml local-dev 프로필용 MySQL
├── .env.example       docker-compose·local-dev 계정 예시 (.env 는 커밋하지 않는다)
├── core-domain/       라이브러리. 도메인별 domain·implement·infra, common(ErrorCode, 이벤트)
│   └── src/main/resources/   domain.yml(H2), domain-local-dev.yml(MySQL)
├── game-api/          실행 앱. room·chat·game·vote·report presentation·service, 에러 응답·예외 핸들러, STOMP 설정, Dockerfile(루트 compose.yml에서 씀)
├── admin-api/         실행 앱. admin·question·report·song·movie·statistics·crawl presentation·service
└── crawler-batch/     실행 앱. crawl service
```

- 모든 모듈의 Java 패키지는 `geniusneugul.project.core` 아래 도메인별(room, chat, game, question, vote, song, movie, report, admin, crawl, statistics)이다.
- 앱 모듈의 `src/test/java/geniusneugul/project/core/support/`에 통합 테스트 공통 상위 클래스·설정을 둔다(목록은 docs/TEST.md 「Spring Context 재사용」).

## 작업 규칙

- 게임 규칙(정답 판정, 마스킹, 투표)은 순수 함수로 분리하고 단위 테스트를 먼저 작성한다. 규칙을 바꾸면 테스트도 함께 바꾼다.
- 게임 상태(점수, 현재 문제, 투표 현황, 타이머)는 서버가 단일 진실 원천이다. 클라이언트는 표시만 한다.
- 타이머(다음 라운드 3초, 10초 패스, 스틸컷 10초 교체, 시간 경과 힌트)는 서버 기준으로 돌린다. 게임 타이머는 game-api의 `gameTaskScheduler`(`common/config/GameSchedulerConfig`)로 돌리고, 다음 라운드 간격은 `app.game.next-round-delay`(테스트는 `src/test/resources/config/application.yml`에서 짧게 덮는다)다. 현재 시각은 주입받은 `Clock` 빈에서 읽는다(`docs/TEST.md` 「시간 제어」).
- 테스트는 `docs/TEST.md` 「무엇을 테스트할까」 기준으로 필수만 쓴다. 단순 CRUD·위임·프레임워크 동작은 테스트하지 않는다.
- **테스트를 쓰기 전에 검증할 행위 목록과 각각을 테스트하는 이유를 사용자에게 먼저 제시하고 확인받는다.**
- 통합 테스트는 `support/`의 공통 상위 클래스를 상속해 Spring context를 재사용한다(`docs/TEST.md` 「Spring Context 재사용」). `@DirtiesContext`, 직접 붙인 `@SpringBootTest`, 테스트 클래스의 `@MockitoBean`은 훅이 쓰기 전에 막는다.
- `support/`에는 `docs/TEST.md`에 나열된 상위 클래스만 둔다. 목록 밖의 상위 클래스가 필요하면 만들기 전에 먼저 묻는다.
- `core/<모듈>/src/test`의 Java 파일은 Write/Edit 도구로만 쓴다. Bash(heredoc, sed, python 등)로 쓰면 테스트 context 가드 훅을 거치지 않는다.
- Spring 설정 파일은 `.properties`가 아니라 `.yml`로 쓴다.
- 클래스·메서드 이름은 `../docs/DOMAIN.md`의 유비쿼터스 언어를 따른다.
- 초기 데이터는 `../initial_crawler/data/`의 JSON만 읽는다. 크롤러 중간 산출물에 의존하지 않는다.
- 코드를 바꿔 기술 스택·명령·구조가 달라지면 이 파일을 함께 고친다. `docs/` 컨벤션과 달라지면 문서를 고치지 말고 먼저 알린다.
