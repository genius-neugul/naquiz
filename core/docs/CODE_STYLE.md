# 코드 스타일 컨벤션

## 목표

코드 스타일 컨벤션은 개인 취향을 줄이고, 팀 전체가 같은 리듬으로 코드를 읽고 수정하기 위한 기준이다.

## Java 기본 규칙

- 클래스명은 역할이 드러나는 명사(구), 메서드명은 동사로 시작하고, boolean 메서드는 `is`·`has`·`can`·`should`를 쓴다. 약어는 대문자 연속을 피한다(`ApiClient`, `UrlParser`).
- `null` 반환보다 빈 컬렉션, `Optional`, 예외 중 의미에 맞는 방식을 고른다.

## Spring Bean 네이밍

| 역할 | 접미사 예시 |
| --- | --- |
| Controller | RoomController |
| Service | RoomService |
| Reader | RoomReader |
| Appender | RoomAppender |
| Updater | RoomUpdater |
| Validator | RoomValidator |
| Calculator | PassThresholdCalculator |
| Repository | RoomRepository |
| Client | SpotifyApiClient |

도메인 모델은 개념 이름을 그대로 쓴다(`Room`, `Participant`). 도메인 모델이 JPA 엔티티를 겸하므로 `~Entity` 접미사는 쓰지 않는다.

## 메서드 작성 규칙

- 한 메서드 안에서 추상화 수준을 섞지 않는다. private 메서드는 복잡한 구현을 숨기되 과도하게 쪼개지 않는다.
- 중첩 if가 깊어지면 early return·정책 객체·validator 분리를, for 반복문이 복잡해지면 컬렉션 처리 책임의 분리를 검토한다.

## Lombok 규칙

허용한다 — `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Slf4j`. 테스트 fixture의 builder는 `@Builder` 없이 직접 작성한다(`TEST.md`).

아래의 "Entity"는 domain 패키지의 도메인 모델을 가리킨다. 도메인 모델이 JPA 엔티티를 겸하므로 두 규칙이 같은 클래스에 적용된다.

금지한다.

- Entity의 `@Setter`, `@Data`. 상태 변경은 의미 있는 도메인 메서드로만 한다.

지양한다.

- 순환 참조 가능성이 있는 객체의 `@ToString`
- equals/hashCode 기준이 불명확한 Entity의 `@EqualsAndHashCode`

## 주석 규칙

코드가 "무엇을 하는지"가 아니라 코드만으로 드러나지 않는 **"왜 그렇게 했는지"**를 남긴다. 외부 정책, 법적 요구, 장애 회피 로직은 근거를 남긴다.

## 상수 규칙

의미 있는 도메인 값은 상수보다 값 객체나 enum을 우선 검토하고, 단순 숫자 리터럴은 의미가 드러나는 이름으로 분리한다. 테스트에서만 쓰는 값은 테스트 fixture에 둔다.

## 리뷰 체크리스트

- 클래스 이름이 역할을 정확히 설명하고, 메서드 추상화 수준이 일정한가?
- Lombok이 객체의 불변성과 캡슐화를 깨지 않는가? Entity에 setter가 없는가?
- 주석이 코드의 이유를 설명하고, 도메인 의미가 원시 타입에 과하게 흩어져 있지 않은가?
