# 테스트 컨벤션

## 목표

테스트는 변경을 안전하게 만들기 위한 문서이자 자동 검증 수단이다. 이름만 읽어도 검증 대상을 알 수 있고, 실패 원인을 빠르게 좁힐 수 있으며, 구현 세부사항이 아니라 외부로 드러나는 행위를 검증해야 한다.

## 테스트 계층

| 테스트 | 대상 | 목적 |
| --- | --- | --- |
| Unit Test | Domain, Implement 단위 클래스 | 빠른 피드백, 정책/계산/검증 확인 |
| Service Test | Service 유스케이스 | 비즈니스 흐름과 트랜잭션 경계 확인 |
| Repository Test | Infra(Repository) + Domain(엔티티 매핑) | 쿼리, 매핑, 영속성 확인 |
| Controller Test | Presentation | 요청/응답, 검증, 상태 코드 확인 |
| WebSocket Test | 실시간 메시지 경로 | 구독·전송 destination과 메시지 경로 확인 |
| Integration Test | 여러 레이어 결합 | 주요 시나리오 회귀 방지. 도메인을 가로지르는 것은 `scenario/` 아래 둔다 |

> 도메인 모델이 JPA 엔티티를 겸하므로 단위 테스트는 도메인 객체를 `new`로 만들어 검증하며, 이때 식별자는 `null`이다. 식별자가 필요한 검증은 Repository·Service 테스트에서 하거나 Fixture Builder로 주입한다.

## 무엇을 테스트할까

테스트는 깨졌을 때 의미 있는 것만 쓴다. 단순 CRUD까지 계층마다 테스트하면 가치는 없고 유지 비용과 빌드 시간만 는다. **테스트를 쓰기 전에 검증할 행위 목록과 각각을 테스트하는 이유를 먼저 정리해 확인받는다.**

**반드시 테스트한다.**

- 게임 규칙 순수 함수 — 정답 판정·정규화, 마스킹, 투표 통과 기준, 점수·승자 확정
- 도메인 상태 전이와 정책 분기 — 성공 케이스와 실패 케이스를 함께
- 동시성 — 선착순 정답자 확정처럼 동시 요청에서 깨질 수 있는 규칙
- 서버 타이머 — 10초 패스, 스틸컷 교체, 시간 경과 힌트
- 직접 작성한 쿼리 — 조건, 정렬, fetch join
- 엔드포인트의 HTTP 계약 — 상태 코드, 검증 실패 응답 형식
- 여러 도메인을 관통하는 핵심 시나리오

**테스트하지 않는다.**

- 분기 없이 Repository로 위임만 하는 조회·저장(Reader, Appender, 단순 Service). 이 경로는 시나리오 테스트가 지나가며 검증한다.
- Spring Data 기본·파생 메서드(`save`, `findById`, `findByX`)
- getter, 생성자, record, Lombok, 필드 복사뿐인 DTO 변환
- 프레임워크 동작. Bean Validation 애노테이션을 필드마다 확인하지 않고 대표 케이스 하나로 응답 형식만 본다.
- 같은 규칙을 여러 계층에서 반복 검증하는 것. 규칙을 소유한 가장 낮은 계층에서 한 번만 검증한다.
- 실제 결과 없이 `verify()` 호출 횟수만 확인하는 테스트

입력 변형은 테스트 메서드를 늘리지 않고 `@ParameterizedTest`로 묶으며, 동치 분할의 대표값과 경계값만 쓴다. 도메인 규칙은 Spring 없이 단위 테스트로 검증하고, context를 띄우는 테스트는 흐름 검증에만 쓴다.

## 네이밍 규칙

```java
@DisplayName("방 인원이 가득 차면 입장에 실패한다.")
@Test
void join_roomIsFull() {}
```

- `@DisplayName`은 사용자 행위 또는 비즈니스 규칙 중심으로 쓰고, 어투는 `~다.` 형태로 맞춘다.
- 메서드명은 영어를 기본으로 한다.
  - 성공 케이스는 `{메서드명}` 또는 `{메서드명}_{조건}` 형식으로 쓴다(`join`, `join_asFirstParticipant`).
  - 실패 케이스는 `{메서드명}_{실패이유}` 형식으로 쓴다(`join_roomIsFull`).
- 하나의 테스트는 하나의 이유로 실패해야 한다.

## Given-When-Then

테스트 본문은 given(조건), when(검증 대상 행위 1회), then(결과 검증) 구역으로 주석을 달아 나눈다. **테스트를 이해하는 데 필요 없는 값은 fixture나 builder로 숨긴다.**

## Fixture 규칙

Fixture는 테스트의 의도를 가리는 중복을 줄이기 위해 사용한다.

- 모든 필드를 받는 거대한 fixture 메서드(`create(id, name, age, status, ...)`)는 만들지 않는다.
- 테스트에서 중요한 값은 테스트 본문에 드러내고, 중요하지 않은 기본값은 fixture 내부에 둔다.
- fixture는 `src/test` 아래에만 두고, 운영 코드(`src/main`)에서 참조하지 않는다. fixture가 운영 코드를 호출해 객체를 만드는 것은 허용한다.

### 중요한 값을 주입하는 Fixture Builder 규칙

특정 값이 검증 의도에 중요하다면 fixture 메서드 인자를 늘리지 말고 **테스트 전용 builder**를 쓴다. `{도메인}/fixture/` 아래에 정적 팩토리 + 중첩 builder 형태로 두고, 체이닝을 위해 자기 자신을 반환한다. **builder는 직접 작성하고 Lombok `@Builder`를 쓰지 않는다.**

권장 형식:

```java
public class ParticipantFixture {

    public static ParticipantBuilder participant() {
        return new ParticipantBuilder();
    }

    public static class ParticipantBuilder {

        private String nickname = "참가자1";
        private ParticipantRole role = ParticipantRole.GUEST;
        private int roundScore = 0;

        public ParticipantBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public ParticipantBuilder role(ParticipantRole role) {
            this.role = role;
            return this;
        }

        public ParticipantBuilder roundScore(int roundScore) {
            this.roundScore = roundScore;
            return this;
        }

        public Participant build() {
            return Participant.create(nickname, role, roundScore);
        }
    }
}
```

사용 예시:

```java
Participant host = ParticipantFixture.participant().role(ParticipantRole.HOST).build();
```

- builder의 기본값은 정상 케이스를 만들 수 있는 값으로 두고, `build()`는 항상 유효한 객체를 반환한다.
- 테스트마다 값이 자주 바뀌는 필드만 builder 메서드로 노출한다.
- 식별자가 필요하면 builder에서 주입한다. 저장 전 id가 `null`이므로 테스트 코드에서의 리플렉션 주입을 허용한다.

## Assertion 규칙

JUnit 기본 assertion보다 AssertJ를 우선 사용한다.

```java
assertThat(result.status()).isEqualTo(RoomStatus.PLAYING);
assertThatThrownBy(() -> roomService.join(command))
    .isInstanceOf(BusinessException.class)
    .hasMessage(ROOM_FULL.getMessage());
```

- 예외 검증은 `assertThatThrownBy` 또는 `assertThatExceptionOfType`을, 컬렉션 검증은 `containsExactly`·`extracting`·`filteredOn`을 쓴다.
- JUnit assertion은 테스트 생명주기와 관련된 특수 상황이 아니면 사용하지 않는다.

## Mock 사용 규칙

Mock은 외부 협력 객체의 결과를 통제해야 할 때만 쓴다 — 외부 API 호출, 메시지 발행, UUID·랜덤 같은 비결정 요소, 실패 상황 강제.

- **예외:** Controller 슬라이스 테스트(`@WebMvcTest`)는 HTTP 계약에 집중하기 위해 Service를 mock한다.
- 도메인 모델·값 객체·같은 모듈의 작은 객체까지 mock으로 덮거나, 실제 검증 없이 `verify()` 호출 수에만 집중한 테스트는 지양한다.

### 시간 제어

시간은 mock하지 않고 **`Clock` 빈으로 제어한다.**

- 운영 코드는 현재 시각을 `Clock`(또는 스케줄러 포트) 빈에서 읽는다. `LocalDateTime.now()`처럼 시스템 시계를 직접 읽지 않는다.
- 테스트는 `support/`의 공통 설정에 조작 가능한 가짜 시계를 **한 번만** 등록한다. 테스트 클래스마다 시계를 따로 등록하면 context가 갈린다.
- 타이머 테스트는 가짜 시계를 앞으로 돌려 검증하고, 실제 시간(10초 패스 등)을 기다리지 않는다.

## DB 테스트 독립 환경 설정

각 테스트의 독립적인 환경은 **매 테스트 전에 테이블을 비우는 방식**으로 만든다. **개별 테스트에 `@Transactional`을 붙이지 않는다.** 각 앱 모듈의 공통 상위 클래스 `support/IntegrationTestSupport`가 매 테스트 전에 모든 테이블을 비운다(H2 참조 무결성을 잠시 끄고 `TRUNCATE ... RESTART IDENTITY`).

**`@Transactional` 롤백에 기대지 않는 이유가 셋이다.**

- **서비스가 선언한 트랜잭션 경계를 테스트 트랜잭션이 덮어써** 경계 자체를 검증하지 못하게 된다. 「방장은 정확히 1명」처럼 트랜잭션 경계로만 보장되는 불변식이 통과해 버린다.
- 커밋이 일어나지 않으므로 `@TransactionalEventListener(AFTER_COMMIT)` 리스너가 실행되지 않는다.
- 명시적 flush로 순서를 만드는 코드(`saveAndFlush`)가 무엇을 막는지 드러나지 않는다.

> **테스트 전용 엔티티를 만들지 않는다.** 스키마는 운영과 같은 엔티티 매핑에서 만들고, 그 형태 위에서 매핑을 검증한다.

## Spring Context 재사용

Spring은 설정이 같은 테스트끼리 context를 캐시해 재사용한다. 테스트 클래스마다 설정이 조금씩 다르면 context가 그 수만큼 새로 떠서 빌드가 느려진다. **모든 통합 테스트가 같은 설정을 공유하게 만든다.**

- `@SpringBootTest` 테스트는 모두 `support/`의 공통 상위 클래스를 상속한다. 테스트 클래스에 `@SpringBootTest`를 직접 붙이지 않는다.
- `@DirtiesContext`를 쓰지 않는다. 상태는 테이블·캐시 정리로 되돌린다.
- `@MockitoBean`·`@MockitoSpyBean`은 공통 상위 클래스(또는 `support/`의 공통 설정)에만 선언한다. 각 테스트는 given 절에서 동작만 정한다. 선언한 mock은 테스트마다 초기화된다.
- 클래스별 `@TestPropertySource`·`@ActiveProfiles`·`@Import`도 context를 새로 만든다. 꼭 필요할 때만 쓰고 이유를 주석으로 남긴다.
- `@WebMvcTest` 같은 슬라이스 테스트는 이 절의 대상이 아니다.

**통합 테스트(`@SpringBootTest`)는 앱 모듈(game-api, admin-api, crawler-batch)에만 둔다.** core-domain은 `@SpringBootApplication`이 없는 라이브러리라 단위 테스트(domain, implement)만 둔다. Repository·매핑 테스트는 그 Repository를 쓰는 앱 모듈에서 한다. `support/`는 앱 모듈마다 `src/test/java/geniusneugul/project/core/support/`에 따로 두고, 세 앱의 `IntegrationTestSupport`는 같은 내용을 유지한다.

`support/`에 두는 공통 상위 클래스는 아래 목록뿐이다. **목록 밖의 상위 클래스가 필요하면 만들기 전에 먼저 묻는다.**

| 클래스 | 설정 | 용도 |
| --- | --- | --- |
| `IntegrationTestSupport` | `@SpringBootTest` (MOCK 환경) | Service·Repository·Integration 테스트 |
| `WebSocketTestSupport` | `@SpringBootTest(webEnvironment = RANDOM_PORT)` | WebSocket 테스트. 지금은 game-api에만 있다 |

> 앞의 세 규칙(`@SpringBootTest` 직접 선언, `@DirtiesContext`, 테스트 클래스의 `@MockitoBean`)은 Claude가 Write/Edit로 테스트 코드를 쓸 때 `.claude/hooks/test-context-guard.py`가 쓰기 전에 막는다. 각 모듈의 `src/test/java/geniusneugul/project/core/support/` 아래 파일만 검사하지 않는다. Bash로 파일을 쓰면 훅을 거치지 않으므로 테스트 파일은 Write/Edit로만 쓴다.

## 계층별 테스트

### Controller 테스트

Controller 테스트는 HTTP 계약(URL, method, header, query parameter, request/response body, status code)을 검증한다. **RestAssuredMockMvc를 표준으로 사용하고, 단순 `MockMvc.perform()` 방식은 새 테스트에서 쓰지 않는다.**

```java
// @WebMvcTest(RoomController.class) + Service는 @MockitoBean
// @BeforeEach에서 RestAssuredMockMvc.mockMvc(mockMvc)로 초기화한다
RestAssuredMockMvc.given().contentType(ContentType.JSON).body(request)
    .when().post("/api/rooms")
    .then().statusCode(HttpStatus.CREATED.value()).body("status", equalTo("WAITING"));
```

- Service는 mock 처리하고 Controller의 HTTP 계약에 집중한다. 복잡한 비즈니스 성공/실패 조합은 Service 테스트에서 검증한다.

### Service 테스트

Service 테스트는 가능하면 실제와 가까운 환경에서 검증한다. `support/IntegrationTestSupport`를 상속해 쓴다.

### Repository 테스트

Repository 테스트도 `support/IntegrationTestSupport`를 상속해 같은 context를 재사용한다. **`@DataJpaTest`는 쓰지 않는다** — 기본 트랜잭션 롤백이 «DB 테스트 독립 환경 설정»과 맞지 않고, context가 따로 뜬다.

- 쿼리 조건과 정렬, 엔티티 매핑(domain 모델 ↔ 테이블), N+1이나 fetch join이 중요한 조회를 검증한다.
- H2와 운영 DB의 문법 차이를 무시한 테스트, 단순 Spring Data 메서드에 대한 과도한 테스트는 지양한다.

### WebSocket 테스트

실시간 메시지 경로(구독, 정답 제출, 브로드캐스트)는 `support/WebSocketTestSupport`를 상속해 실제 포트로 서버를 띄우고 STOMP 클라이언트로 검증한다.

- 정답 판정·투표 같은 규칙은 단위 테스트가 보므로, 소켓 테스트는 메시지 경로와 destination만 확인한다.
- 소켓 테스트는 context가 하나 더 뜨므로 핵심 경로만 둔다.
- 메시지 도착은 Awaitility로 상한을 걸고 기다린다.

### Integration 테스트

Integration 테스트는 **여러 도메인과 여러 레이어가 실제로 물려 있는지**를 본다. Service 테스트와 같은 상위 클래스를 상속하며 HTTP 계층은 검증 대상이 아니다 — 컨트롤러의 계약은 Controller 테스트가 본다.

- **한 도메인에 속하는 시나리오는 그 도메인 패키지에 둔다.** 라운드 진행처럼 주인이 분명한 것이 여기 해당한다.
- **주인이 없는 시나리오는 `geniusneugul.project.core.scenario` 패키지에 둔다.** 방 생성 → 입장 → 게임 시작 → 라운드 → 정답자 확정처럼 여러 도메인을 관통하는 흐름은 어느 도메인의 것도 아니다.
- **서비스 진입점만으로 시나리오를 엮는다.** 리포지토리에 직접 seeding하면 그 지점의 정책 검증과 이벤트 발행을 건너뛰어, 배선이 끊겨 있어도 초록이 된다.
- **비동기 경로를 기다릴 때는 Awaitility로 상한을 걸고 기다린다.** `Thread.sleep` 폴링 루프를 새로 만들지 않는다 — 손으로 쓴 루프는 타임아웃 시 마지막 상태를 조용히 반환해 무엇을 기다리다 실패했는지가 남지 않는다. **대기 상한은 한 상수에 둔다** — 복제해 두면 한 번에 조정할 수 없다.

## 테스트 데이터 정리

테스트는 서로 독립적이어야 하며 실행 순서에 의존하지 않는다. DB 정리는 위 «DB 테스트 독립 환경 설정»을 따른다.

## 리뷰 체크리스트

- 테스트 이름이 비즈니스 행위를 설명하는가?
- 실패 케이스가 함께 검증되는가?
- 테스트가 구현 세부사항에 과하게 결합되어 있지 않은가?
- Fixture가 테스트 의도를 가리지 않는가?
- 통합 테스트와 단위 테스트의 역할이 분리되어 있는가?
- 단순 CRUD·위임·프레임워크 동작을 테스트하거나 같은 규칙을 여러 계층에서 반복하지 않는가?
- 통합 테스트가 `support/`의 공통 상위 클래스를 상속하고, 클래스별 설정으로 context를 갈라놓지 않는가?
- 시간에 의존하는 코드가 `Clock` 빈을 쓰고, 테스트가 실제 시간을 기다리지 않는가?
