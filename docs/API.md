**API Convention**

## **목표**

이 문서는 백엔드가 제공하는 HTTP API와 실시간 메시지(STOMP)의 **요청/응답 규격**을 기록한다. 프론트엔드 연동, 신규 입사자 온보딩, API 변경 리뷰의 기준 문서로 사용한다.


API는 `core/docs/ARCHITECTURE.md`의 레이어 규칙을 따른다. 요청/응답 DTO는 presentation 레이어에서만 정의하고, service는 command/result 모델로 소통한다. 에러 응답 형식은 `core/docs/EXCEPTION.md`를 따른다. 도메인 모델이 JPA 엔티티를 겸하므로, 엔티티가 응답으로 새어 나가는 것을 레이어 규칙이 막아 주지 않는다. 아래 공통 규칙으로 명시적으로 금지한다.

## **공통 규칙**

- 기본 경로 접두사는 `/api`다.
- 요청/응답 본문은 모두 `application/json`이다.
- 성공 응답의 HTTP 상태 코드는 유스케이스 의미에 맞춘다. 리소스 생성은 `201 Created`, 조회는 `200 OK`를 사용한다.
- 요청 형식 검증 실패는 `400 Bad Request`와 **`COMMON_INVALID_REQUEST`** 코드로 내려간다. 필드별 상세는 `errors` 배열에 담긴다.
- 비즈니스 규칙 위반은 공통 `ErrorCode`에 정의된 상태 코드와 코드로 내려간다.
- 인증은 없다. 참가자는 비회원 게스트이며, 실시간 메시지에서는 연결 단위로 참가자를 식별한다(「실시간 메시지(STOMP) 규격」). HTTP 요청에서 참가자를 식별하는 방식은 아직 없다.
- 도메인 모델을 응답 본문으로 직접 직렬화하지 않는다. presentation은 service가 반환한 result 모델만 응답 DTO로 변환한다.
- service의 result 모델에도 도메인 모델을 담지 않는다. 필요한 값만 옮긴 record로 만든다.
- 요청 본문을 도메인 모델에 바인딩하지 않는다. 요청 DTO → command로만 들어온다.

## **API 버저닝**

경로에 버전 세그먼트를 두지 않는다. 접두사는 `/api`뿐이고 `/api/v1` 같은 형태를 쓰지 않는다. 호환이 깨지는 변경이 필요해지면 그때 방침을 정한다.

## **페이징·정렬 규격**

목록 조회는 페이징을 쓴다. 배열을 그대로 반환하지 않는다. (개수가 구조적으로 작은 목록 — 한 리뷰 요청의 리뷰어 목록 등 — 은 예외로 배열을 반환할 수 있다)

**요청 파라미터**

| 파라미터 | 기본값 | 규칙 |
| --- | --- | --- |
| `page` | `0` | **0-base**. 음수는 `400` |
| `size` | `20` | 최대 `100`. 초과·0 이하는 `400` |
| `sort` | 엔드포인트별 지정 | `필드,방향` 형식(예: `occurrenceCount,desc`). **엔드포인트가 허용하는 필드 화이트리스트 밖이면 `400`** — 임의 필드 정렬을 허용하면 인덱스 없는 컬럼으로 전체 스캔이 난다 |

**응답 본문**

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

- 키 이름은 위 5개로 고정한다. Spring Data `Page`의 직렬화 형태(`pageable`, `first`, `last`, `numberOfElements` 등)를 그대로 내보내지 않는다 — 버전에 따라 필드가 바뀌고 클라이언트가 그것에 의존하게 된다.
- **service는 Spring `Page`를 반환하지 않는다.** infra 타입이 상위 레이어로 새는 것이다. `common/service/PageResult`로 옮겨 반환하고, presentation이 `common/presentation/PageResponse`로 변환한다.

## **에러 응답 형식**

모든 에러는 다음 형식을 사용한다. (상세 규칙은 `core/docs/EXCEPTION.md` 참고)

```json
{
  "code": "COMMON_RESOURCE_NOT_FOUND",
  "message": "요청한 리소스를 찾을 수 없습니다."
}
```

검증 실패는 `errors` 배열이 붙는다. 비어 있으면 직렬화에서 빠진다.

```json
{
  "code": "COMMON_INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [{ "field": "title", "message": "must not be blank" }]
}
```

## **실시간 메시지(STOMP) 규격**

방·게임 진행은 STOMP over WebSocket으로 주고받는다. 본문은 모두 JSON이다.

**연결과 참가자 식별**

- 엔드포인트는 `/ws`다(순수 WebSocket, SockJS 없음).
- 서버는 연결마다 새 Principal을 붙이고, 그 이름을 참가자 토큰으로 쓴다. 클라이언트는 토큰을 보내지 않는다. 연결이 끊기면 참가자는 퇴장한다(`docs/DOMAIN.md` 참가자).
- 한 연결은 한 방에만 들어갈 수 있다.
- 한 연결이 보낸 메시지는 보낸 순서대로 처리한다.
- 한 연결로 가는 메시지는 서버가 보낸 순서대로 도착한다. 한 참가자가 연달아 보낸 채팅은 모두에게 보낸 순서대로 보인다.

**destination**

| 방향 | destination | 본문 | 설명 |
| --- | --- | --- | --- |
| SEND | `/app/rooms/create` | `{ "nickname": "방장" }` | 방을 만들고 초대 코드를 발급한다. 닉네임은 앞뒤 공백을 빼고 1~10자 |
| SEND | `/app/rooms/join` | `{ "inviteCode": "A1B2C3", "nickname": "감자" }` | 초대 코드로 방에 들어간다. 초대 코드는 앞뒤 공백을 빼고 대문자로 바꿔 받는다. 닉네임 규칙은 방 만들기와 같고, 같은 방에서 겹쳐도 된다 |
| SEND | `/app/rooms/leave` | 없음 | 방을 나간다. 들어간 방이 없으면 무시한다 |
| SEND | `/app/rooms/chat` | `{ "text": "안녕하세요" }` | 들어가 있는 방에 채팅을 보낸다. 앞뒤 공백을 빼고 1~100자. 라운드 진행 중이면 정답 제출을 겸한다(「게임」) |
| SEND | `/app/games/start` | `{ "gameType": "MOVIE_STILL_CUT", "targetScore": 5 }` | 방장이 게임을 시작한다. `gameType`은 `SONG`, `MOVIE_TWENTY_QUESTIONS`, `MOVIE_STILL_CUT`, `targetScore`는 1~50. 혼자 있어도 시작할 수 있다 |
| SUBSCRIBE | `/user/queue/room` | 방 상태 | 방 만들기·참가하기 응답. 보낸 연결에만 온다 |
| SUBSCRIBE | `/topic/rooms/{roomId}` | 방 이벤트 | 방에 있는 모든 참가자에게 온다. 채팅도 이 토픽으로 온다(보낸 사람 포함) |
| SUBSCRIBE | `/user/queue/errors` | 에러 응답 | 메시지 처리 실패. 보낸 연결에만 온다 |

**방 상태**

```json
{
  "roomId": 1,
  "inviteCode": "A1B2C3",
  "status": "WAITING",
  "meId": 1,
  "participants": [{ "participantId": 1, "nickname": "방장", "tag": 1, "role": "HOST" }]
}
```

- `meId`는 이 메시지를 받는 참가자의 ID다.
- `inviteCode`는 숫자·영문 대문자 6자리다.
- `tag`는 방 안에서 유일한 입장 순서 번호다(방장 1, 나간 사람 번호는 다시 쓰지 않음). 화면에는 `nickname#tag`로 보여준다.

**방 이벤트**

| type | 의미 | 본문 |
| --- | --- | --- |
| `PARTICIPANT_JOINED` | 참가자가 들어왔다 | `{ "type": "PARTICIPANT_JOINED", "participantId": 3, "participants": [...] }` (들어온 사람 포함 전체) |
| `PARTICIPANT_LEFT` | 게스트가 나갔다 | `{ "type": "PARTICIPANT_LEFT", "participantId": 2, "participants": [...] }` (남은 참가자) |
| `ROOM_CLOSED` | 방장이 나가 방이 끝났다 | `{ "type": "ROOM_CLOSED", "participantId": 1 }` |
| `CHAT` | 참가자가 채팅을 보냈다 | 아래 「채팅」 |
| `GAME_STARTED` | 방장이 게임을 시작했다 | 아래 「게임」 |
| `ROUND_STARTED` | 라운드가 열렸다 | 아래 「게임」 |
| `ROUND_SOLVED` | 정답자가 확정됐다 | 아래 「게임」 |
| `GAME_FINISHED` | 승자가 목표 점수에 도달해 게임이 끝났다 | 아래 「게임」 |

**채팅**

```json
{ "type": "CHAT", "participantId": 2, "nickname": "감자", "tag": 2, "text": "안녕하세요", "sentAt": "2026-10-03T21:00:00" }
```

- 채팅은 저장하지 않는다. 방에 들어온 뒤 구독한 채팅만 받는다.
- `sentAt`은 서버가 받은 시각이다.

**게임**

```json
{ "type": "GAME_STARTED", "gameId": 1, "gameType": "MOVIE_STILL_CUT", "targetScore": 5, "scores": [{ "participantId": 1, "score": 0 }] }
{ "type": "ROUND_STARTED", "gameId": 1, "roundNo": 1, "startedAt": "2026-10-03T21:00:00" }
{
  "type": "ROUND_SOLVED", "gameId": 1, "roundNo": 1,
  "solverId": 2, "nickname": "감자", "tag": 2, "text": "기생충",
  "answer": "기생충", "subAnswer": "Parasite", "solvedAt": "2026-10-03T21:00:12",
  "scores": [{ "participantId": 1, "score": 0 }, { "participantId": 2, "score": 1 }],
  "nextRoundAt": "2026-10-03T21:00:15"
}
{ "type": "GAME_FINISHED", "gameId": 1, "winnerId": 2, "scores": [...] }
```

- 게임을 시작하면 `GAME_STARTED` 다음에 1라운드 `ROUND_STARTED`가 온다. 방 상태는 PLAYING이 되고 모든 점수는 0이다.
- `ROUND_STARTED`에는 정답이 없다. 정답은 라운드가 끝날 때 `ROUND_SOLVED`로 공개한다.
- 라운드 진행 중 채팅은 모두 정답 판정 대상이다. 가장 먼저 맞힌 채팅은 `CHAT` 대신 `ROUND_SOLVED`로 모두에게 간다(`text`가 그 채팅이다). 오답과 라운드 사이 채팅은 `CHAT`으로 간다.
- `scores`는 방에 있는 참가자 모두의 현재 게임 점수다.
- 게임이 계속되면 `nextRoundAt`(정답자 확정 3초 뒤)에 다음 라운드 `ROUND_STARTED`가 온다. 정답자가 목표 점수에 도달하면 `nextRoundAt`은 `null`이고 바로 `GAME_FINISHED`가 오며, 방은 WAITING으로 돌아간다.
- 방장이 나가 방이 끝나면 게임도 승자 없이 끝난다. 게임 이벤트는 따로 없고 `ROOM_CLOSED`만 온다.
- 시각(`startedAt`, `solvedAt`, `nextRoundAt`)은 서버 시각이다.

**에러**

본문은 HTTP 에러 응답과 같은 형식(`code`, `message`, 검증 실패면 `errors`)이다. 방·채팅·게임 관련 코드:

| code | 상황 |
| --- | --- |
| `COMMON_INVALID_REQUEST` | 닉네임이 비었거나 10자를 넘는다. 채팅이 비었거나 100자를 넘는다. 게임 종류가 없거나 목표 점수가 1~50이 아니다 |
| `ROOM_ALREADY_JOINED` | 이미 방에 들어가 있는 연결이 방을 또 만들거나 다른 방에 들어간다 |
| `ROOM_INVITE_CODE_EXHAUSTED` | 겹치지 않는 초대 코드를 만들지 못했다 |
| `ROOM_INVALID_INVITE_CODE` | 초대 코드가 숫자·영문 6자리가 아니다 |
| `ROOM_NOT_FOUND` | 그 초대 코드의 방이 없다(방장이 나가 닫힌 방 포함) |
| `ROOM_FULL` | 방 인원(10명)이 가득 찼다 |
| `ROOM_ALREADY_PLAYING` | 게임이 진행 중인 방이다 |
| `ROOM_NOT_JOINED` | 방에 들어가 있지 않은 연결이 채팅을 보내거나 게임을 시작한다 |
| `GAME_INVALID_TYPE` | 게임 종류 코드가 올바르지 않다 |
| `GAME_NOT_HOST` | 방장이 아닌 참가자가 게임을 시작한다 |
| `GAME_ALREADY_PLAYING` | 이미 게임이 진행 중인 방에서 게임을 시작한다 |
| `GAME_QUESTION_NOT_FOUND` | 그 게임 종류로 출제할 수 있는(승인되고 활성인) 문제가 없다 |

---
