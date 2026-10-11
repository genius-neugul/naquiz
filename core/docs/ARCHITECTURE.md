# 아키텍처 컨벤션

## 목표

프로젝트는 **도메인 중심의 레이어드 아키텍처를 사용**한다. **핵심 목표는 service가 상세 구현을 알지 않고도 비즈니스 흐름을 설명할 수 있게 만드는 것**이다. 신규 입사자, 기획자, 운영 담당자가 service 메서드를 읽었을 때 대략적인 업무 흐름을 이해할 수 있어야 한다.

이 문서는 Gemini Kim의 글 **지속 성장 가능한 소프트웨어를 만들어가는 방법**의 방향성을 프로젝트 컨벤션으로 구체화한 것이다.

## 모듈 구조

`core/`는 Gradle 멀티모듈로 나눈다.

```
core/
├── core-domain    라이브러리. 모든 앱이 의존한다
│   ├── common        ErrorCode, BusinessException, DomainEvent, EventPublisher
│   └── song, question, report, admin, crawl …   domain / implement / infra
│         (Spotify·YouTube 클라이언트는 song/infra, 곡 정제 규칙은 song/domain)
├── game-api       실행 앱. 방·게임·투표 presentation·service, WebSocket(STOMP), 웹 공통
├── admin-api      실행 앱. 관리자 로그인, 검수, 오류 신고 처리, 통계, 크롤링 현황
└── crawler-batch  실행 앱. 웹 없음. 데일리 크롤링(정제 → 중복 제외 → Spotify → YouTube → 검수 대기로 저장)
```

| 모듈 | 종류 | 의존 | 담는 것 |
| --- | --- | --- | --- |
| core-domain | 라이브러리 | — | 도메인별 domain·implement·infra(엔티티, 저장소, 외부 API 클라이언트), 공통 예외·이벤트 |
| game-api | 실행 앱 | core-domain | 게임 유스케이스(service), Controller·STOMP 핸들러, 에러 응답·예외 핸들러·요청 로그 필터 |
| admin-api | 실행 앱 | core-domain | 관리자 유스케이스(service), Controller, 자체 에러 응답·예외 핸들러 |
| crawler-batch | 실행 앱 | core-domain | 데일리 크롤링 스케줄과 유스케이스 |

- **세 앱은 따로 배포한다.** 크롤링이 실패하거나 배포해도 게임이 멈추지 않고, 관리자 API는 게임과 다른 인증·접근 제한을 둘 수 있다.
- **공통 코드는 core-domain에 둔다.** 게임 서버도 초기 데이터 곡을 출제할 때 Spotify·YouTube를 호출하므로 외부 API 클라이언트를 core-domain에서 함께 쓴다.
- **global 모듈은 두지 않는다.** 모든 앱이 core-domain에 의존하므로 공통 예외·이벤트는 core-domain `common`으로 충분하다.
- **웹 공통은 game-api에 둔다.** 별도 웹 모듈로 분리하지 않는다. admin-api는 같은 [에러 응답 형식](EXCEPTION.md#에러-응답-형식)을 따르는 에러 응답·예외 핸들러를 따로 둔다.
- **presentation·service는 앱 모듈에, implement·infra·domain은 core-domain에 둔다.** 앱끼리는 서로 의존하지 않는다. 두 앱이 같은 흐름을 쓰면 implement로 내려 core-domain에서 공유한다.
- **멀티모듈에서는 implement 패키지를 `<도메인>.implement`로 둔다.** implement는 여러 앱의 service가 함께 쓰는 core-domain의 레이어이고, core-domain에는 service가 없으므로 `service` 아래에 두지 않는다. 의존성 방향(presentation → service → implement → infra)은 그대로다.
- **외부 API 클라이언트는 해당 도메인의 infra에 둔다.** 별도 `external` 패키지를 만들지 않는다.
- `ErrorCode`가 `HttpStatus`를 가지므로 core-domain은 `spring-web`에 의존한다. 웹 서버(`spring-boot-starter-webmvc`)는 game-api·admin-api만 넣는다.
- **모든 모듈의 기본 패키지는 `geniusneugul.project.core`로 같다.** 앱 클래스가 이 패키지에 있어 core-domain의 엔티티·Repository·빈을 별도 스캔 설정 없이 찾는다.
- **DB·JPA 설정은 core-domain의 `domain.yml`(프로필별 `domain-<프로필>.yml`) 한 벌이다.** 각 앱은 `spring.config.import: classpath:domain.yml`로 가져오고, 앱 설정에는 앱 이름·포트 같은 앱 고유 값만 둔다.

## 기본 패키지 구조

도메인을 최상위 기준으로 나누고, 도메인 내부에서 레이어를 나눈다. 같은 도메인 패키지가 여러 모듈에 걸쳐 있고, 레이어마다 놓이는 모듈이 정해져 있다([모듈 구조](#모듈-구조)).

```java
// core-domain
geniusneugul.project.core
├── common
│   ├── exception        // ErrorCode, BusinessException
│   ├── domain           // 여러 도메인이 함께 쓰는 값(GameType)
│   ├── domain/event     // DomainEvent
│   └── infra/event      // EventPublisher
└── room
    ├── implement
    ├── infra
    └── domain

// game-api (admin-api도 같은 형태)
geniusneugul.project.core
├── GameApiApplication
├── common
│   └── exception        // ErrorResponse, GlobalExceptionHandler
└── room
    ├── presentation
    └── service
```

| 패키지 | 역할 |
| --- | --- |
| common | 도메인 공통 요소. core-domain: 에러 코드·예외(`exception`), 여러 도메인이 함께 쓰는 값(`domain`), 이벤트(`domain/event`, `infra/event`). 앱 모듈: 에러 응답·예외 핸들러(`exception`) |
| presentation | HTTP·실시간 메시지 요청/응답, Controller, API DTO, 참가자 식별 |
| service | 비즈니스 흐름 조립, 유스케이스 단위 트랜잭션 경계 |
| implement | 비즈니스 흐름을 구성하는 상세 구현 도구. core-domain의 `<도메인>.implement`에 둔다([모듈 구조](#모듈-구조)) |
| infra | 저장소 접근(Repository), 외부 API·캐시·메시징 기술 격리 |
| domain | 도메인 모델(JPA 엔티티 겸용), 값 객체, 정책, 상태 전이 규칙 |

## 의존성 방향

```java
presentation -> service -> implement -> infra

domain: presentation을 제외한 모든 레이어가 참조하는 핵심 모델
```

1. 상위 레이어는 하위 레이어만 참조한다.
2. 하위 레이어는 상위 레이어를 참조하지 않는다.
3. 레이어를 건너뛰지 않는다. 예를 들어 service가 infra를 직접 참조하지 않는다.
4. 동일 레이어 간 참조는 피한다. 단, implement는 협력 도구 성격이 강하므로 다른 implement를 참조할 수 있다.
5. 다른 도메인이 필요하면 그 도메인의 implement(Reader 등)를 직접 주입받는다. 다른 도메인의 service·infra는 참조하지 않으며, 도메인 간 순환 참조는 금지한다.
6. domain은 레이어 사이의 단계가 아니라 핵심 모델이다. service·implement·infra가 참조할 수 있고, **presentation은 참조하지 않는다**(service의 result 모델만 받는다). domain은 다른 레이어를 참조하지 않으며(domain -> infra 금지), 공통 매핑 상위 클래스도 domain에 둔다.

## Service 작성 규칙

service는 비즈니스 로직을 "직접 구현"하는 곳이 아니라 비즈니스 흐름을 "표현"하는 곳이다.

**허용한다.**

- 유스케이스를 나타내는 public 메서드
- 트랜잭션 경계
- 입력 커맨드 검증 중 비즈니스 흐름에 가까운 검증
- implement 객체를 조합한 업무 흐름
- 도메인 객체의 정책 호출

예시

```java
@Transactional
public JoinResult join(JoinCommand command) {
    Room room = roomReader.readByInviteCode(command.inviteCode());
    roomEntryValidator.validate(room);
    Participant participant = participantAppender.append(room, command.nickname());

    return JoinResult.from(participant);
}
```

**금지한다.**

- Repository 직접 주입
- JPA, QueryDSL, Redis, 메시징 클라이언트, HTTP Client 같은 기술 객체 직접 사용
- 요청 DTO를 그대로 서비스 인자로 받기, 응답 DTO를 서비스에서 직접 조립하기
- 복잡한 if, for, switch가 누적되어 구현 상세가 드러나는 코드
- 외부 API 응답 모델이나 DB Entity에 강하게 결합된 코드

## Implement 작성 규칙

implement는 서비스가 사용하는 협력 도구다. 하나의 클래스는 하나의 명확한 역할을 가지며, 이름만 봐도 서비스 흐름에서 맡는 역할이 드러나야 한다.

네이밍은 역할 중심으로 한다 — 조회 `RoomReader`, 생성 `ParticipantAppender`·`InviteCodeIssuer`, 수정 `ScoreUpdater`, 검증 `RoomEntryValidator`, 계산 `PassThresholdCalculator`, 외부 연동 조율 `SongInfoRequester`·`MessageSender`.

- implement는 상세 구현 로직을 가지며, infra가 제공하는 인터페이스나 저장소 접근 객체를 사용할 수 있다.
- 다른 implement와 협력할 수 있지만 순환 참조는 금지하고, 재사용 가능한 단위로 작게 유지한다.

## Infra 작성 규칙

infra는 기술 의존성을 격리한다.

- Spring Data Repository, QueryDSL, Redis, 외부 API Client 구현체는 이 레이어에 둔다.
- **JPA 엔티티는 domain에 둔다.** 도메인 모델 클래스에 JPA 애노테이션을 붙여 쓰므로 별도 엔티티 클래스와 변환 코드를 만들지 않는다. Repository는 도메인 모델을 그대로 다룬다.
- 상위 레이어에는 기술 세부사항을 노출하지 않는다. 외부 API 응답 DTO를 그대로 올리지 않고, 필요하면 순수 인터페이스와 조회 결과 모델을 제공한다.
- 쿼리 방식 변경(Spring Data ↔ QueryDSL, 페이징·정렬 전략)은 service나 implement로 번지지 않아야 한다.

> 도메인 모델이 JPA 엔티티를 겸하므로 **JPA 자체를 벗어나는 전환은 domain 수정을 수반한다.** 변환 코드와 클래스 중복을 없애는 대신 이 비용을 받아들인 선택이다.

> **저장 위치는 통계 필요 여부로 나눈다.** 백오피스 통계(문제·힌트·투표)에 쓰이거나 원래 영속 데이터인 모델은 JPA 엔티티를 겸한다. 통계에 쓰이지 않는 **방·참가자는 JPA 애노테이션 없는 순수 도메인 객체로 서버 메모리에 둔다.** 방 Repository는 infra의 인터페이스로 두고 메모리 구현체를 쓴다.
>
> - 참가자를 저장하지 않으므로 다른 엔티티의 참가자 ID(승자, 정답자, 단서를 연 참가자, 투표 찬성)는 FK 없는 값으로만 남긴다.
> - 라운드의 차례 필드(차례 순서, 현재 차례 인덱스, 차례 마감 시각)는 진행 중에만 쓰므로 `@Transient`로 둔다.

## 이벤트 발행 규약

외부 메시지 큐가 생기기 전까지는 **인프로세스 발행기 하나만** 둔다.

### 배치

```java
common
├── domain
│   └── event
│       └── DomainEvent          // 마커 인터페이스, 프레임워크 의존 없음
└── infra
    └── event
        └── EventPublisher       // 인프로세스 발행기. 내부적으로 ApplicationEventPublisher를 쓴다
```

- implement는 `EventPublisher`만 참조한다. `ApplicationEventPublisher` 같은 기술 객체를 직접 주입받지 않는다.

### 이벤트 페이로드

이벤트는 **직렬화 가능한 불변 record**로 정의한다. 식별자·원시값·값 객체·시각 타입만 담는다. 지금은 인프로세스로만 전달되지만, 외부 큐로 옮길 때 페이로드를 다시 만들지 않기 위한 규칙이다.

금지한다.

- JPA Entity, 지연 로딩 프록시, 연관 컬렉션
- 영속성 컨텍스트나 트랜잭션이 살아 있어야 읽을 수 있는 값
- **도메인 모델.** 상태가 필요한 리스너는 식별자로 다시 조회한다.

```java
// 금지 — 도메인 모델을 담는다
public record RoundStartedEvent(Round round) implements DomainEvent {}

// 허용
public record RoundStartedEvent(Long roomId, Long roundId, OffsetDateTime occurredAt)
        implements DomainEvent {}
```

**이 규약은 인프로세스에서는 어겨도 동작하므로 리뷰에서 확인한다.**

### 실패 처리

`@TransactionalEventListener(AFTER_COMMIT)`에서 발생한 예외는 호출자에게 전파되지 않고 사라지며, `@Async`가 붙으면 더 확실히 묻힌다. 비동기 수신 실패는 `AsyncUncaughtExceptionHandler`로 ERROR 로그를 남긴다. **리스너에서 예외를 삼키지 않는다.**

### 외부 큐 도입 시

외부 메시지 큐를 도입하면 이 절을 구체화한다. 그때 정할 것: `EventPublisher`를 포트로 두고 기술별 어댑터를 둘지, 어댑터 선택 방식(프로퍼티), 핸들러 멱등성(at-least-once 대비), 이벤트 순서 비의존, 같은 프로세스 안에서만 이어져야 하는 신호의 분리.

## Domain 작성 규칙

domain은 프로젝트의 핵심 개념과 정책을 담는다.

- 값 객체는 불변으로 설계하고, 상태 전이 규칙은 도메인 객체 내부에 둔다. 단순 데이터 컨테이너가 아니라 의미 있는 행위를 제공한다.
- **JPA 매핑 애노테이션은 허용한다.** `@Entity`, `@Table`, `@Id`, `@Column`, `@Embedded`, `@Embeddable`, `@Enumerated`, `@MappedSuperclass`, 연관 매핑, 그리고 Lombok `@Getter`·`@NoArgsConstructor(access = PROTECTED)`까지다.
- **Spring과 Web 의존은 두지 않는다.** `@Component`, `@Transactional`, `ResponseEntity`, `HttpStatus`는 domain에 들어오지 않는다. 비즈니스 규칙 위반은 `common/exception`의 `BusinessException`과 `ErrorCode`로 던진다(`EXCEPTION.md`). **`ErrorCode`가 `HttpStatus`를 가지므로 domain이 `ErrorCode`를 통해 간접적으로 Web에 의존하는 것은 허용한다.** domain 코드에서 `HttpStatus`를 직접 쓰지는 않는다.
- `@Entity` 클래스는 record로 만들 수 없다. 기본 생성자와 가변 필드가 필요하므로 `@NoArgsConstructor(access = PROTECTED)` 일반 클래스로 둔다. **setter는 두지 않는다.** 상태 변경은 의미 있는 도메인 메서드로만 한다.

## 동시성

방·참가자는 서버 메모리에 있고, 같은 방을 여러 스레드가 동시에 건드린다. 서로 다른 참가자의 메시지는 STOMP 처리 스레드 풀에서 병렬로 실행되고, 연결 끊김은 웹소켓 I/O 스레드에서, 서버 타이머는 스케줄러 스레드에서 온다. **같은 방의 상태를 읽고 바꾸는 흐름은 방 단위 락(`room/implement/RoomLock`) 안에서 실행한다.**

```java
// RoomJoiner: 인원·상태 확인과 참가자 추가가 한 락 안에서 일어난다
return roomLock.withLock(room.getId(), () -> {
    Participant participant = room.join(nickname, participantToken, LocalDateTime.now(clock)); // 확인 + 변경
    roomRepository.save(room);                                                                 // 색인 갱신
    return RoomJoin.of(room, participant);                                                     // 스냅샷
});
```

- **확인과 변경을 한 락 안에 둔다.** 인원 상한·닉네임 중복·선착순 정답자처럼 "확인한 뒤 바꾸는" 규칙은 락 밖에서 확인하면 동시 요청이 함께 통과한다.
- **락은 implement에서 잡는다.** service는 흐름만 표현하고 락을 직접 다루지 않는다.
- **도메인 객체(`Room`)는 스스로 동기화하지 않는다.** 락 밖으로 넘기는 결과는 락 안에서 만든 스냅샷(record)으로 넘긴다.
- **읽은 값으로 방 상태를 바꾸지 않는 단순 조회는 락 없이 읽는다.** 채팅 보낸 사람 확인(`RoomMemberReader.read`)처럼 입장·퇴장과 겹쳐도 결과가 틀려지지 않는 경우다. 읽은 값으로 확인하고 바꾸는 흐름(정답 제출 등)은 락 안에서 읽는다(`RoomMemberReader.readWithLock`).
- **락 안에서는 메모리 상태만 다룬다.** DB 조회·저장, 외부 API 호출, STOMP 발송은 락 밖에서 한다. 락 안에서 I/O를 하면 그 방의 모든 요청이 밀리고, 기다리는 스레드가 STOMP 처리 스레드 풀을 차지해 다른 방까지 느려진다.
- **락 안에서 다시 확인한다.** 방을 찾은 뒤 락을 잡기까지 다른 스레드가 먼저 상태를 바꿨을 수 있다.
- **닫힌 방은 다시 바뀌지 않는다.** 지워진 방의 락을 정리할 때 같은 방 ID로 락이 둘 생길 수 있어도, 닫힌 방에 대한 변경은 아무것도 하지 않으므로 상태가 깨지지 않는다.
- 공정 락(`new ReentrantLock(true)`)은 쓰지 않는다. 처리 순서가 중요한 규칙(선착순 정답)은 락 안의 상태 확인으로 지킨다.

## 트랜잭션 경계

- 기본 트랜잭션 경계는 service public 메서드에 둔다.
- 조회 전용 유스케이스는 `@Transactional(readOnly = true)`를 사용한다.
- implement에는 원칙적으로 트랜잭션을 선언하지 않는다.
- 하위 도구 클래스에서 독립 트랜잭션이 필요하면 이유를 PR에 명시한다.

## 리뷰 체크리스트

- service 메서드가 비즈니스 흐름으로 읽히며, Repository나 외부 기술 객체를 직접 참조하지 않는가?
- implement 클래스가 하나의 명확한 역할을 갖는가?
- infra가 Repository·외부 클라이언트 기술을 상위 레이어에 전파하지 않는가? (엔티티는 domain이므로 여기 해당하지 않는다)
- presentation이 domain을 참조하지 않고, 응답이 result → 응답 DTO로만 나가는가?
- domain에 Spring·Web 의존이 들어오지 않았는가? (`ErrorCode`를 통한 간접 의존만 허용)
- 레이어를 건너뛰는 참조가 없고, 다른 도메인은 implement로만 참조하며 순환 참조가 없는가?
- 이벤트 페이로드가 직렬화 가능한 불변 record이며, 리스너가 예외를 삼키지 않는가?
- 같은 방의 상태를 읽고 바꾸는 흐름이 방 락 안에 있고, 락 안에서 I/O를 하지 않는가?
