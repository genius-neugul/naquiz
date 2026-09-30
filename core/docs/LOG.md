# 로그 컨벤션

## 목표

로그는 운영 중인 서버에서 무슨 일이 일어났는지 추적하기 위한 기록이다. 많이 남기는 것이 목적이 아니라 **필요한 맥락을 안전하게 남기는 것**이 목적이다.

## 기본 사용법

SLF4J Logger를 사용하고(Lombok `@Slf4j` 허용), 메시지는 문자열 더하기가 아니라 **placeholder**로 작성한다.

## 로그 메시지 작성 규칙

### 로그 위치 Prefix 규칙

로그 메시지 맨 앞에 `[{ClassName}.{methodName}]` 형식의 prefix를 붙인다.

```java
log.warn("[RoomService.join] Room join rejected. roomId={}, currentStatus={}", roomId, status);
```

- 클래스명은 패키지를 제외한 simple class name, 메서드명은 실제 로그가 작성된 메서드명을 쓴다.
- prefix 뒤에는 공백 한 칸을 둔 뒤 이벤트 메시지를 작성한다.
- 호출 스택을 읽어 클래스명·메서드명을 자동 생성하지 않는다. 성능 비용이 있고 람다·프록시 환경에서 기대와 다른 값이 나온다.
- 공통 로깅 유틸을 쓰더라도 최종 메시지에는 같은 prefix 형식을 유지한다. prefix만 보고 코드 위치를 검색할 수 있어야 한다.

메시지는 이벤트 중심으로 짧게 쓴다. 이벤트 이름은 과거형·결과 중심(`Round finished`, `Room join rejected`)으로 쓰고 식별자를 반드시 포함하며, 객체 전체 대신 필요한 필드만 남긴다. 메시지에 줄바꿈을 넣지 않는다.

## 로그 레벨

| Level | 사용 기준 | 규칙 |
| --- | --- | --- |
| ERROR | 즉시 확인이 필요한 시스템 장애 — DB·외부 API 장애, 메시지 발행 실패 | 예상하지 못한 예외에 쓰고, exception 객체를 **마지막 인자**로 전달해 stack trace를 남긴다 |
| WARN | 요청은 실패했지만 시스템 장애는 아닌 경우 — 비즈니스 규칙 위반, 재시도 가능한 외부 API 실패 | 사용자 실수나 도메인 실패를 ERROR로 남기지 않는다 |
| INFO | 운영 흐름상 의미 있는 주요 상태 변경 — 게임 시작, 정답자 확정, 게임 종료 | 모든 메서드 시작/종료를 INFO로 남기지 않는다 |
| DEBUG | 개발·장애 분석용 상세 정보 — 분기 조건, 중간 계산값 | 운영 기본 레벨에서 보이지 않아도 되는 정보. 분석이 끝난 임시 로그는 제거한다 |
| TRACE | 매우 상세한 흐름 추적 — 반복문 내부 | |

## 어디에 로그를 남길까?

로그는 책임이 있는 위치에 남긴다.

- **Controller** — API 계약과 관련된 실패(잘못된 요청 값)만. 비즈니스 성공/실패를 중복으로 남기지 않는다.
- **Service** — 유스케이스의 주요 업무 이벤트를 **비즈니스적으로 중요한 상태 변경이 끝난 시점**에 남긴다. service가 상세 구현을 알면 안 되므로 하위 기술 세부 로그는 남기지 않는다.
- **Implement** — service 흐름에서는 보이지 않는 도메인 판단과 분기 결과. 같은 실패를 service와 중복으로 남기지 않는다.
- **Infra** — 외부 API 실패, timeout, 재시도, circuit breaker open. SQL 전체나 요청/응답 전문은 남기지 않는다.

## 무엇을 남겨야 할까?

식별자(traceId, participantId, 도메인 id)와 실패 원인을 남긴다. 외부 API는 provider·statusCode·elapsedMs를, 메시지 발행은 eventType·aggregateId를 함께 남긴다. 장애 원인 추적에 도움이 되는가, 같은 요청의 다른 로그와 연결되는가, 민감 정보가 아닌가, 값이 너무 크지 않은가를 기준으로 고른다.

## 실시간 메시지 로그

- **채팅·정답 제출 메시지 원문은 로그에 남기지 않는다.** 메시지 수가 많아 로그가 묻히고, 참가자가 쓴 내용이다.
- 정답자 확정 같은 결과만 INFO로 남긴다(roomId, roundId, participantId).

## 민감 정보 규칙

다음은 로그에 남기지 않는다 — 참가자 토큰, session id, 휴대폰 번호·이메일·주소 전체, 외부 API secret. 필요하면 마스킹한다(`h***@example.com`, `010-****-5678`).

## Trace ID와 MDC

Trace ID는 한 요청의 로그를 이어 붙이기 위한 식별자다. **지금은 tracing 의존성이 없어 trace id가 없다.** 아래 규칙은 tracing 도입 시 적용한다.

- trace id는 **직접 만들지 않는다** — tracing 계측이 요청마다 trace를 시작하고 그 값을 MDC의 `traceId`·`spanId`로 넣어 준다.
- **직접 `TraceIdFilter`를 만들지 않는다.** 따로 만들면 ID가 둘 공존해 로그와 트레이스가 오히려 끊긴다. MDC 정리도 계측이 맡으므로 손으로 `MDC.clear()`를 부르지 않는다.
- **API 에러 응답에는 trace id를 포함한다.** `ErrorResponse`가 팩토리에서 `MDC.get("traceId")`를 읽는다 — 핸들러가 따로 챙기지 않는다.
- 외부 API 요청 시 trace id를 헤더로 전달하고, 비동기 메시지에는 correlation id를 포함한다.
- MDC에 **직접 넣은** 값은 요청 종료 시 반드시 제거한다.

## 예외 로그 규칙

예외 로그는 중복으로 남기지 않는다.

- 예상 가능한 비즈니스 예외는 GlobalExceptionHandler에서 WARN으로 **한 번만** 남긴다.
- 예상하지 못한 예외는 GlobalExceptionHandler에서 ERROR로 **한 번만** 남긴다.
- 외부 시스템 호출 실패처럼 원인 위치가 중요하면 infra에서 맥락과 exception을 함께 남긴다. `log.error(exception.getMessage())`처럼 stack trace와 맥락이 빠진 형태는 쓰지 않는다.

## 외부 연동 로그

요청 시작, 응답 성공, 응답 실패, 타임아웃, 재시도, 최종 실패를 남긴다.

- 외부 시스템 이름을 남기고, 내부 식별자와 외부 요청 식별자, 처리 시간(elapsedMs)을 함께 남긴다.
- 요청/응답 전문은 기본적으로 남기지 않는다. 필요하면 마스킹·보관 기간·접근 권한을 먼저 합의한다.

## 요청/응답 로그

Controller마다 직접 남기지 않고, 필요하면 Filter 또는 Interceptor에서 공통으로 남긴다. 권장 필드는 traceId, method, path, status code, elapsed time, participant id, client ip다.

- **트레이스와 중복되지 않는지 먼저 본다.** tracing을 도입하면 method·path·status·elapsed는 HTTP 서버 스팬이 이미 갖는다. 그때 요청/응답 로그는 **스팬에 없는 것**(도메인 식별자)을 남길 때 값이 있다.
- **request body와 response body는 기본적으로 남기지 않는다.**
- 2xx 응답을 너무 많이 남기면 로그 비용이 커진다.

## 로그 저장과 롤링 전략

**컨테이너로 운영하면 파일 appender를 두지 않고 stdout으로 남긴다.** 보존·검색·롤링은 수집기가 맡는다.

운영 기본 로그 레벨은 INFO이며, DEBUG가 필요하면 특정 패키지만 일시적으로 올리고 분석이 끝나면 되돌린다. 감사·보안 이벤트처럼 장기 보관이 필요한 기록은 로그가 아니라 별도 테이블이나 이벤트 저장소에 남긴다.

파일 로그를 쓰는 환경이 생긴다면 일자+파일 크기 기준으로 롤링한다 — maxFileSize 100MB, maxHistory 14일, totalSizeCap 2GB, 지난 로그는 `.gz` 압축.

## 흔한 실수

`System.out.println` 사용(레벨 제어와 trace id 같은 공통 문맥이 붙지 않는다), 객체 전체 출력(`log.info("request={}", request)` — 민감 정보가 섞이고 `toString()` 변경에 휘둘린다), 모든 곳에 로그 남기기(start / step1 / end — 중요한 로그가 묻힌다).

## 리뷰 체크리스트

- 로그 메시지 맨 앞에 [클래스명.메서드명] prefix를 붙였는가?
- 로그 레벨이 상황에 맞는가? 예상 가능한 비즈니스 실패를 ERROR로 남기지 않았는가?
- 장애 원인을 추적할 식별자가 있고, 객체 전체 출력이나 민감 정보가 섞이지 않았는가?
- 채팅·정답 제출 원문을 로그에 남기지 않았는가?
- 예외 로그에 stack trace와 맥락이 함께 남고, 같은 예외를 여러 레이어에서 중복으로 남기지 않았는가?
- 외부 연동 실패에 provider, statusCode, elapsedMs 같은 정보가 있는가?
