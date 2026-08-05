# order-service API 검증 가이드

Swagger UI 로 order-service 의 API 11개를 직접 호출해 검증하는 방법입니다.
다른 서비스(gateway · company · hub · delivery)가 하나도 안 떠 있어도 **order-service 단독으로** 전부 검증됩니다.

---

## 1. 사전 준비

### 1-1. PostgreSQL 기동

```bash
# 저장소 루트에서
docker compose -f docker/docker-compose-infra.yaml up -d postgres
docker ps        # 컨테이너가 Up 인지 확인
```

### 1-2. 환경변수 — 저장소 루트 `.env`

접속값은 전부 환경변수로 빠져 있고, 저장소 루트의 `.env` 를 읽습니다
(`spring.config.import`). 없으면 아래 기본값으로 뜹니다.

```bash
cp .env.example .env      # 저장소 루트에서
```

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `POSTGRES_HOST` | `localhost` | DB 호스트 |
| `POSTGRES_PORT` | `5432` | DB 포트 |
| `POSTGRES_DB` | `delivery` | **전 서비스 공유 DB** |
| `POSTGRES_USERNAME` | `postgres` | 사용자명 |
| `POSTGRES_PASSWORD` | `postgres` | 비밀번호 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka` | Eureka |
| `ZIPKIN_HOST` / `ZIPKIN_PORT` | `localhost` / `9411` | Zipkin |
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | 트레이스 샘플링 |
| `REQUIRE_GATEWAY_HEADERS` | `false` | true 면 인증 헤더 없는 요청을 401 로 차단 |
| `DEV_USER_ID` / `DEV_USER_ROLE` | `0000...0000` / `MASTER` | 인증 헤더가 없을 때 쓸 개발용 사용자 |

> **스키마명은 `.env` 에 없습니다.** `.env` 는 전 서비스가 공유해서, 여기에 두면 다른 서비스까지
> 같은 스키마를 보게 됩니다. order-service 의 `order_db` 는 `application.yaml` 에 고정돼 있습니다.
>
> ⚠️ 스키마명을 `order` 로 바꾸면 안 됩니다. SQL 예약어라 `from order.p_orders` 가 문법 오류로 깨집니다.

DB 는 `delivery` 하나를 공유하고 서비스는 스키마로 나뉩니다. `delivery` DB 가 없다면 먼저 만듭니다.

```sql
CREATE DATABASE delivery;
```

`order_db` 스키마는 `local` 프로파일에서 Hibernate 가 만들어 주므로 직접 칠 필요 없습니다.

### 1-3. 애플리케이션 기동

**프로파일을 반드시 지정해야 합니다.** 기본 프로파일이 없어서(배포 때 `ddl-auto: update` 로
조용히 뜨는 사고를 막기 위함) 프로파일 없이 띄우면 테이블이 만들어지지 않습니다.

```bash
cd order-service

# 로컬 개발 (테이블 생성 + SQL 로그)
./gradlew bootRun --args="--spring.profiles.active=local"

# IDE 실행 구성에서는 VM 옵션에
#   -Dspring.profiles.active=local
```

| 프로파일 | `ddl-auto` | 용도 |
| --- | --- | --- |
| `local` | `update` | 테이블 자동 생성 + `db/schema.sql` 로 부분 인덱스·CHECK 추가 |
| `prod` | `validate` | 스키마는 별도 DDL 로 관리, 앱은 검증만 |
| (없음) | 건드리지 않음 | 사고 방지용 |

Eureka 가 안 떠 있으면 등록 실패 로그가 반복되는데, API 검증에는 영향이 없습니다. 로그를 줄이려면:

```bash
./gradlew bootRun --args="--spring.profiles.active=local --eureka.client.enabled=false"
```

---

## 2. Swagger UI 접속

| 주소 | 용도 |
| --- | --- |
| http://localhost:9004/swagger-ui.html | **Swagger UI (여기로 접속)** |
| http://localhost:9004/v3/api-docs | OpenAPI 명세 (JSON) |
| http://localhost:9004/actuator/health | 기동 확인 |

`SecurityConfig` 가 모든 요청을 `permitAll` 로 열어두었기 때문에 로그인 없이 바로 **Try it out** 을 누를 수 있습니다.

### 인증 헤더에 대해

Gateway 없이 직접 호출하면 `UserContextInterceptor` 가 개발용 사용자
(`00000000-0000-0000-0000-000000000000`)로 대체합니다. 주문의 `requesterUserId`, 감사 필드
(`createdBy`)에 이 값이 들어갑니다. 실제 사용자로 검증하고 싶으면 요청 헤더에 직접 넣으면 됩니다.

```
X-User-Id: 11111111-1111-1111-1111-111111111111
X-User-Role: MASTER
```

---

## 3. 검증 시나리오 (순서대로 따라 하면 됩니다)

`{{...}}` 부분은 앞 단계 응답에서 받은 값으로 바꿔 넣으세요.
UUID 는 아무 값이나 써도 됩니다 (company/hub 서비스 연동 전이라 존재 검증을 하지 않습니다).

### STEP 1 — 재고 등록 `POST /api/v1/inventories`

경기 허브에 오징어 500개를 배치합니다.

```json
{
  "productId": "aaaaaaaa-0000-0000-0000-000000000001",
  "hubId": "bbbbbbbb-0000-0000-0000-000000000001",
  "companyId": "cccccccc-0000-0000-0000-000000000001",
  "quantity": 500
}
```

✅ **201 Created**, `availableQuantity: 500`, `reservedQuantity: 0`
→ 응답의 `inventoryId` 를 메모해 둡니다.

**실패 검증**: 같은 `productId` + `hubId` 로 한 번 더 호출
→ ❌ **409 `INVENTORY_ALREADY_EXISTS`** ("입고 API를 사용해 주세요")

---

### STEP 2 — 입고 `POST /api/v1/inventories/{{inventoryId}}/inbound`

```json
{ "quantity": 100, "note": "정기 입고" }
```

✅ **200**, `previousQuantity: 500` → `quantity: 600`

**실패 검증**: `quantity: 0` → ❌ **400 `INVALID_INPUT_VALUE`** ("입고 수량은 1 이상이어야 합니다.")

---

### STEP 3 — 실사 보정 `PATCH /api/v1/inventories/{{inventoryId}}/adjust`

```json
{ "quantity": 550, "reason": "실사 결과 파손 50개 확인" }
```

✅ **200**, `adjustedDelta: -50`, `quantity: 550`

**실패 검증**: `reason` 을 `""` 로 → ❌ **400** ("보정 사유는 필수입니다.")

---

### STEP 4 — 허브 간 이관 `POST /api/v1/inventories/transfer`

```json
{
  "productId": "aaaaaaaa-0000-0000-0000-000000000001",
  "fromHubId": "bbbbbbbb-0000-0000-0000-000000000001",
  "toHubId": "bbbbbbbb-0000-0000-0000-000000000002",
  "quantity": 50,
  "note": "부산 허브 보충"
}
```

✅ **200**, `from.quantity: 500`, `to.quantity: 50`, **`toCreated: true`**
→ 도착 허브에 재고 행이 없으면 자동 생성됩니다.

**실패 검증**
- `fromHubId` == `toHubId` → ❌ **400** ("출발 허브와 도착 허브가 같을 수 없습니다.")
- 가용 수량보다 큰 `quantity` → ❌ **409 `INSUFFICIENT_STOCK`**

---

### STEP 5 — 재고 검색 `GET /api/v1/inventories`

| 파라미터 | 값 | 확인할 것 |
| --- | --- | --- |
| (없음) | | 등록한 재고 2건이 모두 나옴 |
| `hubId` | 경기 허브 | 1건 |
| `onlyAvailable` | `true` | 가용 수량 1 이상만 |
| `maxAvailableQuantity` | `100` | 품절 임박 재고만 (부산 허브 50개) |
| `sort` | `quantity,asc` | 수량 오름차순 정렬 |
| `size` | `30` | 페이지 크기 30 |

✅ 응답 형태 — `content` / `page` / `size` / `totalElements` / `totalPages`

**정책 검증**: `size=1000` → 응답의 `size` 가 **10 으로 고정**됩니다.
허용 크기는 10 / 30 / 50 뿐이고, 그 외에는 기본값으로 내려갑니다.
`sort=아무거나` 도 `createdAt,desc` 로 무시됩니다. (인덱스 없는 컬럼 정렬로 인한 풀스캔 방지)

---

### STEP 6 — 재고 단건 조회 `GET /api/v1/inventories/{{inventoryId}}`

✅ **200**, `quantity` / `reservedQuantity` / `availableQuantity` 3개 값 확인

**실패 검증**: 없는 UUID → ❌ **404 `INVENTORY_NOT_FOUND`**

---

### STEP 7 — 주문 접수 `POST /api/v1/orders`

```json
{
  "supplierCompanyId": "cccccccc-0000-0000-0000-000000000001",
  "receiverCompanyId": "cccccccc-0000-0000-0000-000000000002",
  "originHubId": "bbbbbbbb-0000-0000-0000-000000000001",
  "destHubId": "bbbbbbbb-0000-0000-0000-000000000002",
  "requestDetails": "오전 중 배송 부탁드립니다",
  "dueAt": "2026-12-31T18:00:00",
  "items": [
    {
      "productId": "aaaaaaaa-0000-0000-0000-000000000001",
      "productName": "마른 오징어",
      "quantity": 50,
      "unitPrice": 1000
    },
    {
      "productId": "aaaaaaaa-0000-0000-0000-000000000002",
      "productName": "건조 다시마",
      "quantity": 20,
      "unitPrice": 500
    }
  ]
}
```

✅ **201 Created**
- `status: "PENDING"`
- `itemCount: 2`, `totalQuantity: 70`, **`totalPrice: 60000.00`** ← 서버가 계산한 값
- → 응답의 `orderId` 를 메모해 둡니다.

> ⚠️ **재고는 줄어들지 않습니다.** 재고 선점 연동이 아직 안 붙어 있어서 정상입니다.
> STEP 5 로 다시 조회해도 `reservedQuantity` 는 0 입니다.

**실패 검증**
- `items: []` → ❌ **400** ("주문 상품은 1개 이상이어야 합니다.")
- 같은 `productId` 를 두 줄로 → ❌ **400 `DUPLICATE_ORDER_ITEM`** ("수량을 합쳐서 요청해 주세요.")
- `quantity: 0` → ❌ **400** ("수량은 1 이상이어야 합니다.")
- `dueAt` 을 과거 날짜로 → ❌ **400** ("납품 기한은 현재 시각 이후여야 합니다.")
- `receiverCompanyId` 누락 → ❌ **400** ("수령 업체 ID는 필수입니다.")

---

### STEP 8 — 주문 상세 조회 `GET /api/v1/orders/{{orderId}}`

✅ **200**, `items` 배열에 2줄, 각 줄의 `linePrice` = `unitPrice × quantity`

**실패 검증**: 없는 UUID → ❌ **404 `ORDER_NOT_FOUND`**

---

### STEP 9 — 주문 수정 `PATCH /api/v1/orders/{{orderId}}`

오징어 수량을 50 → 10 으로 줄이고, 다시마를 빼고, 김을 새로 넣습니다.

```json
{
  "requestDetails": "수량 변경했습니다",
  "items": [
    {
      "productId": "aaaaaaaa-0000-0000-0000-000000000001",
      "productName": "마른 오징어",
      "quantity": 10,
      "unitPrice": 1000
    },
    {
      "productId": "aaaaaaaa-0000-0000-0000-000000000003",
      "productName": "구운 김",
      "quantity": 5,
      "unitPrice": 2000
    }
  ]
}
```

✅ **200** — 수정·추가·삭제가 한 번에 반영됩니다.
- `itemCount: 2`, `totalQuantity: 15`, **`totalPrice: 20000.00`** ← 자동 재계산
- 다시마 줄이 사라지고 김 줄이 생김

**부분 수정 확인**: `items` 를 아예 빼고 `requestDetails` 만 보내면
→ 상품 구성은 그대로 두고 요청사항만 바뀝니다.

---

### STEP 10 — 주문 이력 조회 `GET /api/v1/orders/{{orderId}}/snapshots`

✅ **200** — 이력 **2건**이 쌓여 있습니다.

| sequence | eventType | 내용 |
| --- | --- | --- |
| 1 | `ORDER_CREATED` | 접수 당시 (오징어 50 + 다시마 20, 60,000원) |
| 2 | `ORDER_MODIFIED` | 수정 후 (오징어 10 + 김 5, 20,000원) |

**여기서 꼭 확인할 것** — 1번 이력의 `items` 는 **여전히 오징어 50개 + 다시마 20개**입니다.
스냅샷은 바뀐 값만이 아니라 그 순간의 주문 전체를 복사해 두기 때문에,
나중에 상품이 삭제되거나 이름이 바뀌어도 그때 무엇을 주문했는지 그대로 남습니다.

**필터 검증**: `eventType=ORDER_MODIFIED` → 2번 이력만 조회됩니다.
**정렬**: `sort=sequence,asc` 로 시간순 정렬 (기본은 `createdAt,desc`)

---

### STEP 11 — 주문 이력 단건 `GET /api/v1/orders/{{orderId}}/snapshots/{{snapshotId}}`

✅ **200**, `items` 의 `lineNo` 가 1부터 순서대로

**실패 검증**: **다른 주문의** `orderId` 로 같은 `snapshotId` 를 조회
→ ❌ **404 `ORDER_SNAPSHOT_NOT_FOUND`** (경로만 바꿔 남의 이력을 들여다볼 수 없습니다)

---

### STEP 12 — 주문 검색 `GET /api/v1/orders`

| 파라미터 | 값 | 확인할 것 |
| --- | --- | --- |
| `status` | `PENDING` | 접수 상태만 |
| `originHubId` | 경기 허브 | 해당 허브 출발 주문 |
| `keyword` | `오징어` | **상품명**으로 검색됨 |
| `keyword` | `변경` | **요청사항**으로도 검색됨 |
| `productId` | 오징어 ID | 그 상품이 포함된 주문 |
| `minTotalPrice` | `10000` | 금액 범위 |
| `createdFrom` | `2026-01-01T00:00:00` | 생성일 범위 |
| `sort` | `totalPrice,desc` | 금액순 정렬 |

**중요 검증** — 상품 2줄짜리 주문을 `productId` 로 검색해도 **`totalElements` 가 1** 입니다.
줄 수만큼 주문이 중복 집계되면 페이징이 깨지는데, EXISTS 서브쿼리로 걸어 그걸 막았습니다.

---

### STEP 13 — 주문 취소 `PATCH /api/v1/orders/{{orderId}}/cancel`

```json
{ "cancelReason": "고객 요청으로 취소" }
```

✅ **200**, `status: "CANCELED"`, `cancelReason` 반영
→ 이력에 **3번 `ORDER_CANCELED`** 가 추가됩니다 (STEP 10 다시 조회)

**상태 기계 검증**: 취소된 주문을 **한 번 더 취소**
→ ❌ **400 `INVALID_ORDER_STATUS`** ("허용되지 않는 상태 전이입니다. (CANCELED → CANCELED)")

**수정 차단 검증**: 취소된 주문에 STEP 9 를 다시 실행
→ ❌ **400** ("CANCELED 상태의 주문은 변경할 수 없습니다.")

**실패 검증**: `cancelReason` 을 `""` 로 → ❌ **400** ("취소 사유는 필수입니다.")

---

### STEP 14 — 주문 삭제 `DELETE /api/v1/orders/{{orderId}}`

✅ **200** `{ "success": true }`

**논리 삭제 확인**
- `GET /api/v1/orders/{{orderId}}` → ❌ **404** (조회 안 됨)
- `GET /api/v1/orders` 검색 결과에서도 제외
- 이력 조회도 **404** (주문과 함께 감춰짐)
- **DB 에는 행이 남아 있습니다** ↓

```sql
SELECT id, status, deleted_at, deleted_by FROM order_db.p_orders;
-- deleted_at 이 채워진 채로 행이 남아 있음
```

**순서 검증**: `PENDING` 상태 주문을 바로 삭제
→ ❌ **400** ("PENDING 상태의 주문은 삭제할 수 없습니다. 주문 취소를 먼저 진행해 주세요.")
진행 중인 주문을 지우면 배송·재고와 상태가 어긋나기 때문에 취소를 먼저 거치게 막아둔 것입니다.

---

### STEP 15 — 재고 삭제 `DELETE /api/v1/inventories/{{inventoryId}}`

✅ **200**, `deletedAt` / `deletedBy` 반환 (논리 삭제)

> 선점된 재고(`reservedQuantity > 0`)는 ❌ **400 `INVENTORY_IN_USE`** 로 막히지만,
> 재고 선점 연동 전이라 지금은 이 경로를 만들 수 없습니다. 연동 후 검증 항목입니다.

---

## 4. 응답 형태

### 성공

```json
{
  "success": true,
  "data": { }
}
```

### 목록 (`data` 안이 페이지 구조)

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

### 실패

```json
{
  "success": false,
  "error": {
    "errorCode": "ORDER_NOT_FOUND",
    "message": "주문을 찾을 수 없습니다."
  },
  "timestamp": "2026-08-05T02:00:00Z"
}
```

---

## 5. 주요 에러 코드

| 코드 | HTTP | 발생 상황 |
| --- | --- | --- |
| `INVALID_INPUT_VALUE` | 400 | `@Valid` 검증 실패 (필드 메시지가 그대로 내려옴) |
| `INVALID_REQUEST` | 400 | 타입 불일치, 잘못된 JSON |
| `INVALID_ORDER_STATUS` | 400 | 허용되지 않는 상태 전이 |
| `DUPLICATE_ORDER_ITEM` | 400 | 같은 상품을 여러 줄로 담음 |
| `ORDER_ITEM_REQUIRED` | 400 | 주문 상품을 전부 제거 |
| `DELIVERY_ALREADY_STARTED` | 400 | 배송 생성된 주문의 상품 구성 변경 시도 |
| `ORDER_NOT_FOUND` | 404 | 없거나 삭제된 주문 |
| `ORDER_SNAPSHOT_NOT_FOUND` | 404 | 없는 이력 / 다른 주문의 이력 |
| `INVENTORY_NOT_FOUND` | 404 | 없거나 삭제된 재고 |
| `INVENTORY_ALREADY_EXISTS` | 409 | 같은 (상품, 허브) 재고 중복 등록 |
| `INSUFFICIENT_STOCK` | 409 | 가용 재고 부족 |
| `INVENTORY_IN_USE` | 409 → 400 | 선점된 재고 삭제 시도 |
| `CONCURRENT_UPDATE_CONFLICT` | 409 | 낙관적 락 충돌 (동시 수정) |

---

## 6. curl 로 검증하기 (Swagger 없이)

> ⚠️ **Windows(Git Bash·PowerShell)에서 한글이 든 JSON 을 `-d` 로 직접 넘기면 깨집니다.**
> 셸이 CP949 로 인코딩해 보내서 서버의 UTF-8 파싱이 실패하고 **400 `INVALID_REQUEST`** 가 납니다.
> API 문제가 아니라 셸 인코딩 문제입니다. 한글이 필요하면 UTF-8 파일로 저장해 `--data-binary @파일` 로 보내세요.
> Swagger UI 는 브라우저가 UTF-8 로 보내므로 이 문제가 없습니다.
>
> ```bash
> curl -X POST $BASE/orders -H 'Content-Type: application/json' --data-binary @order.json
> ```

```bash
BASE=http://localhost:9004/api/v1

# 재고 등록
curl -X POST $BASE/inventories \
  -H 'Content-Type: application/json' \
  -d '{"productId":"aaaaaaaa-0000-0000-0000-000000000001",
       "hubId":"bbbbbbbb-0000-0000-0000-000000000001",
       "companyId":"cccccccc-0000-0000-0000-000000000001",
       "quantity":500}'

# 주문 접수 (인증 헤더를 직접 넣는 예시)
curl -X POST $BASE/orders \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 11111111-1111-1111-1111-111111111111' \
  -H 'X-User-Role: MASTER' \
  -d '{"supplierCompanyId":"cccccccc-0000-0000-0000-000000000001",
       "receiverCompanyId":"cccccccc-0000-0000-0000-000000000002",
       "originHubId":"bbbbbbbb-0000-0000-0000-000000000001",
       "destHubId":"bbbbbbbb-0000-0000-0000-000000000002",
       "items":[{"productId":"aaaaaaaa-0000-0000-0000-000000000001",
                 "productName":"마른 오징어","quantity":50,"unitPrice":1000}]}'

# 주문 검색
curl "$BASE/orders?status=PENDING&keyword=오징어&size=30&sort=createdAt,desc"

# 주문 이력
curl "$BASE/orders/{{orderId}}/snapshots?sort=sequence,asc"
```

---

## 7. DB 로 직접 확인

```bash
docker exec -it <postgres-container> psql -U postgres -d delivery
```

```sql
SET search_path TO order_db;

\dt                                          -- 테이블 5개 확인
SELECT id, status, item_count, total_price, deleted_at FROM p_orders;
SELECT product_name, quantity, unit_price, line_price FROM p_order_items;
SELECT sequence, event_type, order_status, total_price FROM p_order_snapshots ORDER BY sequence;
SELECT product_id, hub_id, quantity, reserved_quantity FROM p_inventories;
```

---

## 8. 아직 검증할 수 없는 것

연동이 안 붙어서 **지금은 확인이 불가능한** 항목들입니다. 검증 중 이상하다고 느낄 수 있어 미리 적어 둡니다.

| 항목 | 이유 |
| --- | --- |
| 주문해도 재고가 안 줄어듦 | 재고 선점(`reserve`)이 주문 흐름에 아직 연결되지 않음 |
| `CONFIRMED` / `COMPLETED` 상태로 못 넘어감 | 배송 서비스가 통보하는 값이라 내부 API 필요 |
| 실물 재고 확정 차감 | 배송 완료 통보 시점에만 일어남 |
| 존재하지 않는 상품·허브·업체 ID 도 통과됨 | company/hub 서비스 연동 전이라 존재 검증 불가 |
| 단가를 클라이언트가 보냄 | 연동 후 서버가 조회한 값으로 대체됨 |
| 출발 허브 불일치(`MULTIPLE_ORIGIN_HUB`) 검증 | 상품별 허브를 company-service 에서 받아야 판단 가능 |
| 스냅샷의 업체명·허브명이 `null` | ID 는 항상 저장되고, 이름은 연동 후 채워짐 |
| 동시 요청 시 같은 (상품, 허브) 재고 2건 생성 가능 | `local` 프로파일이면 `db/schema.sql` 의 부분 유니크 인덱스가 막아 준다 |

```sql
-- 살아있는 행에만 유니크. 일반 UNIQUE 를 걸면 삭제 후 재등록이 영원히 불가해진다
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_alive
    ON order_db.p_inventories (product_id, hub_id) WHERE deleted_at IS NULL;
```
