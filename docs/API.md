**API Convention**

## **목표**

이 문서는 백엔드가 제공하는 HTTP API의 **요청/응답 규격**을 기록한다. 프론트엔드 연동, 신규 입사자 온보딩, API 변경 리뷰의 기준 문서로 사용한다.

API는 `core/docs/ARCHITECTURE.md`의 레이어 규칙을 따른다. 요청/응답 DTO는 presentation 레이어에서만 정의하고, service는 command/result 모델로 소통한다. 에러 응답 형식은 `core/docs/EXCEPTION.md`를 따른다. 도메인 모델이 JPA 엔티티를 겸하므로, 엔티티가 응답으로 새어 나가는 것을 레이어 규칙이 막아 주지 않는다. 아래 공통 규칙으로 명시적으로 금지한다.

## **공통 규칙**

- 기본 경로 접두사는 `/api`다.
- 요청/응답 본문은 모두 `application/json`이다.
- 성공 응답의 HTTP 상태 코드는 유스케이스 의미에 맞춘다. 리소스 생성은 `201 Created`, 조회는 `200 OK`를 사용한다.
- 요청 형식 검증 실패는 `400 Bad Request`와 **`COMMON_INVALID_REQUEST`** 코드로 내려간다. 필드별 상세는 `errors` 배열에 담긴다.
- 비즈니스 규칙 위반은 공통 `ErrorCode`에 정의된 상태 코드와 코드로 내려간다.
- 인증은 없다. 참가자는 비회원 게스트이며, 요청에서 참가자를 식별하는 방식은 미정이다.
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

---
