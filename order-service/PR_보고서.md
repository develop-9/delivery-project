# [Feat] Order Service 주문·재고·스냅샷 CRUD + Search

## 작업 내용

order-service의 **주문 / 재고 / 주문 이력(스냅샷)** 도메인에 대한 CRUD와 검색을 구현했습니다.
다른 서비스 연동 없이 **order-service 단독으로 기동·검증**됩니다.

- 공통 코드(`global`) 구축
- 주문 CRUD + Search (8개 API)
- 재고 CRUD + Search (7개 API)
- 주문 이력(스냅샷) 자동 기록 + 조회
- 팀 컨벤션(DDD 패키지 구조 · CQRS · 로깅 · Swagger) 적용

## 관련 이슈

- Closes #13

---

## 1. API 목록 (11개)

### 주문 `/api/v1/orders`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/` | 주문 접수 (201) |
| GET | `/{orderId}` | 단건 조회 |
| GET | `/` | 검색 + 페이징 |
| PATCH | `/{orderId}` | 수정 (요청사항 · 납품기한 · 상품 구성) |


### 주문 이력 `/api/v1/orders/{orderId}/snapshots`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/` | 이력 타임라인 (eventType 필터) |
| GET | `/{snapshotId}` | 이력 단건 |

**검색 조건**: 상태, 공급/수령업체, 출발/도착허브, 요청자, 배송ID, 상품ID, 키워드(상품명·요청사항), 총액 범위, 생성일 범위

### 재고 `/api/v1/inventories`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/` | 등록 (201) |
| GET | `/{inventoryId}` | 단건 조회 |
| GET | `/` | 검색 + 페이징 |
| POST | `/{inventoryId}/inbound` | 입고 (누적) |
| PATCH | `/{inventoryId}/adjust` | 실사 보정 (덮어쓰기) |
| DELETE | `/{inventoryId}` | 논리 삭제 |

**검색 조건**: 상품, 허브, 업체, 보유수량 범위, 가용수량 범위, `onlyAvailable`, `onlyReserved`

> 재고에 일반 `PATCH`를 두지 않았습니다. 수량이 바뀌는 경로를 **입고 · 보정** 으로만 제한해
> 각각 사유와 이력이 남도록 했습니다.

### 스냅샷

조회 API만 있습니다. 이력은 주문 생성·수정·취소 시 **서버가 같은 트랜잭션에서 자동 기록**하고,
주문이 논리 삭제되면 함께 감춰집니다. 손으로 만들거나 고칠 수 있으면 이력이 아니기 때문입니다.

---

## 2. global 공통 코드

| 패키지 | 구성 | 비고 |
| --- | --- | --- |
| `common` | `BaseEntity`, `BaseDeletableEntity` | 감사 필드 + 논리 삭제 |
| `exception` | `ErrorCode`, `BusinessException`, `ErrorDto`, `GlobalExceptionHandler` | 예외 → 공통 응답 변환 |
| `response` | `SuccessResponse`, `ErrorResponse`, `PageResponse` | 응답 봉투 |
| `config` | `JpaConfig`, `SecurityConfig`, `SecurityAuditorAware`, `SystemUser`, `OpenApiConfig` | 인증 주체는 SecurityContext 에서 읽는다 |
| `util` | `PageableUtil` | 페이지 크기 · 정렬 화이트리스트 |

**설계 판단**

- `PageResponse` — `Page<T>`를 그대로 내리면 Jackson이 `pageable`·`sort` 내부 구조까지 직렬화합니다. 실제 쓰는 5개 필드만 담았습니다.
- `SecurityAuditorAware` — hub-service 와 동일하게 `SecurityContextHolder` 에서 인증 주체를 읽어 `created_by`/`updated_by` 를 채웁니다. JWT 파싱 필터가 들어오기 전까지는 `SystemUser.ID` 로 대체합니다(TODO).
- `PageableUtil` — size는 10/30/50, sort는 도메인별 화이트리스트만 허용합니다. 임의 컬럼 정렬을 열어두면 인덱스 없는 컬럼 풀스캔이 됩니다.
- `GlobalExceptionHandler` — 낙관적 락 충돌은 409(`CONCURRENT_UPDATE_CONFLICT`)로 매핑했습니다. 500으로 흘리면 클라이언트가 재시도 여부를 판단할 수 없습니다.

---

## 3. 도메인 규칙 (엔티티가 스스로 방어)

검증을 서비스에 두면 배치나 내부 API가 생길 때마다 규칙이 새어 나가므로, 불변식은 전부 엔티티 안에 두었습니다.

**주문**
- 상태 기계 — `canTransitTo()`에 없는 전이는 전부 차단. `COMPLETED`/`CANCELED`/`FAILED`는 종착역
- 집계 컬럼(`itemCount`/`totalQuantity`/`totalPrice`) 자동 재계산 (`private`이라 외부 조작 불가)
- 같은 상품 중복 줄 차단
- 배송 생성 후에는 상품 구성 변경 차단
- 진행 중(`PENDING`/`CONFIRMED`) 주문은 삭제 불가 — 취소를 먼저 거쳐야 함
- `@Version` 낙관적 락

**재고** — 핵심은 `quantity`(실물)와 `reservedQuantity`(선점) 분리, **가용 = quantity − reserved**
- 선점은 실물을 건드리지 않고 예약만 증가, 가용 초과 시 거절
- **실물이 줄어드는 지점은 `confirm()` 하나뿐** (배송 완료 시점)
- 이관은 가용 범위 안에서만 — 선점된 물량까지 빼가면 진행 중 주문이 차감할 재고를 잃음
- 보정은 선점 수량 아래로 불가, 사유 필수
- 선점된 재고는 삭제 불가

**스냅샷**
- 바뀐 값만이 아니라 **주문 전체와 모든 줄을 통째로 복사** — 이력 한 건만 봐도 그때 구성을 알 수 있음
- `(order_id, sequence)` 유니크, sequence는 1부터
- 업체·허브 **ID는 항상 저장**하고 이름은 nullable — company/hub 연동 후 `fillNames()`로 채우되 기존 값은 덮어쓰지 않음

---

## 4. 주요 구현 판단

**주문 수정은 전체 삭제 후 재삽입이 아니라 병합**
`(order_id, product_id)` 유니크 제약이 있어, 같은 상품을 지웠다 다시 넣으면 Hibernate의 INSERT가 DELETE보다 먼저 나가 제약 위반이 납니다. "있으면 수정 / 없으면 추가 / 빠진 건 제거"로 처리했습니다.

**검색은 QueryDSL 대신 JPA Specification**
Boot 4 / Hibernate 7 조합에서 querydsl-apt 호환이 아직 불안정합니다. 포트 시그니처(`OrderQueryRepository.search`)는 기술 중립이라, QueryDSL로 통일하기로 하면 `infrastructure.persistence`만 교체하면 됩니다.

**상품 조건은 join이 아니라 EXISTS 서브쿼리**
join으로 걸면 줄 수만큼 주문이 중복돼 페이징의 `totalElements`가 부풀어 오릅니다.

**DB 스키마 분리**
DB는 `delivery` 하나를 공유하고 order-service는 `order_db` 스키마만 소유합니다. 스키마명은 `.env`(전 서비스 공유)가 아니라 `application.yaml`에 고정했습니다.
> ⚠️ 스키마명을 `order`로 하면 SQL 예약어라 `from order.p_orders`가 문법 오류로 깨집니다.

**`db/schema.sql`** — `ddl-auto`가 만들지 못하는 것만 담았습니다(`local` 프로파일에서 실행).
- `uk_inventory_alive` — 살아있는 행에만 걸리는 **부분 유니크 인덱스**. 일반 UNIQUE면 삭제 후 재등록이 영구 불가
- 수량·금액 CHECK 제약 (`reserved <= quantity` 등)

---

## 5. 팀 컨벤션 적용

| 항목 | 적용 |
| --- | --- |
| 패키지 구조 | `order/{domain, application, infrastructure, presentation}` + `global` |
| Application | `command` / `result` / `command_service` / `query_service` |
| Repository | 포트는 `domain.repository`, 어댑터는 `infrastructure.persistence` |
| CQRS | `OrderRepository`(커맨드) / `OrderQueryRepository`(쿼리) 분리 |
| DTO 명명 | `OrderCreateRequest` / `OrderCreateCommand` / `OrderResult` / `OrderResponse` |
| Controller | `OrderApiController`, `InventoryApiController` |
| 응답 | `SuccessResponse` / `ErrorResponse` |
| 로깅 | `[주문] 생성 : [orderId]` 형식, 조회 포함 18곳 |
| Swagger | 전 컨트롤러 `@Tag`, 전 API `@Operation` + `@ApiResponses` |

**Application 계층이 presentation을 모릅니다**
`Request → toCommand() → Service(Command) → Result → Response.from()` 흐름이라, 나중에 내부 API가 붙어도 웹 DTO를 재활용할 필요가 없습니다.

---

## 6. 테스트

`@DataJpaTest` + H2. **CI에서 Postgres 없이 통과**합니다. Given/When/Then 패턴.

| 테스트 | 케이스 |
| --- | --- |
| `OrderCrudTest` (5) | 생성 · 조회 · 수정(집계 재계산) · 논리 삭제 · 검색(중복 집계 없음) |
| `InventoryCrudTest` (5) | 등록 · 조회 · 입고/보정 · 논리 삭제 후 재등록 · 가용수량 검색 |
| `OrderSnapshotCrudTest` (4) | 기록 · 타임라인(지난 이력 보존) · eventType 필터 · 논리 삭제 |
| `GlobalExceptionHandlerTest` (3) | 끝 슬래시 404 · 미매핑 경로 404 · 잘못된 UUID 400 |

**총 17개 통과**

---

## 7. 리뷰 포인트

1. **최상위 패키지를 `order` 하나로 두었습니다.** 컨벤션의 "서비스명 기준"과 delivery-service 예시(`Delivery`/`DeliveryRoute`/`DeliveryManager`를 한 패키지에)를 따랐는데, 재고를 별도 도메인 패키지로 분리하는 것이 나을지 의견 부탁드립니다.

2. **Result를 행동별로 쪼개지 않고 `OrderResult` 하나를 공유합니다.** 생성·수정·취소·조회의 응답 모양이 완전히 같아 동일 record가 4벌이 되기 때문입니다(DRY 우선). 재고는 응답이 실제로 달라 행동별로 나눴습니다.

3. **연동 전까지 클라이언트가 보내는 값들** — `supplierCompanyId`, `originHubId`, `productName`, `unitPrice`는 company/hub 연동 후 서버 조회값으로 대체됩니다. 지금은 **검증 없이 신뢰**하는 상태라 그 전제를 공유합니다.

4. **`/api/v1/**` 전체 permitAll** — 인증은 Gateway 담당이라 필터 체인을 열어뒀습니다. 운영 전에는 Gateway 외 접근 차단 + `require-gateway-headers=true` 전환이 필요합니다.

---

## 8. 다음 단계 (이번 PR 범위 밖)

- **재고 선점 배선** — `reserve()`/`confirm()`/`release()`는 엔티티에만 있고 호출부가 없어, 지금은 **주문을 넣어도 재고가 줄지 않습니다**
- **상태 전이** — `CONFIRMED`/`COMPLETED`는 delivery-service 통보가 있어야 하므로 진입 경로 없음
- 내부 API(`/internal/v1/**`) + `InternalApiInterceptor`
- Saga 오케스트레이션(`OrderFacade`) + 보상 트랜잭션
- company / hub / delivery / slack 연동

---

## PR 체크리스트

- [x] 최신 `develop` 반영 및 충돌 해결
- [x] 정상 실행 및 테스트 완료 (17개 통과, 로컬 Postgres 기동 검증)
- [x] 불필요한 파일/코드 정리
- [x] 커밋 메시지 형식 준수
- [ ] merge 후 브랜치 삭제
