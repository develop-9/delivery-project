# Redis 기반 중복 주문 방지

order-service / 2026-08-10

---

## 1. 문제

### 1-1. 같은 주문이 두 번 만들어질 수 있다

주문 접수(`POST /api/v1/orders`)는 주문 행 하나를 만드는 것으로 끝나지 않는다.

```
주문 접수
  ├─ 재고 선점        reserved_quantity += 수량
  ├─ 주문 · 주문 상품 저장
  ├─ 주문 이력 기록
  └─ 배송 생성 호출    delivery-service
```

이 요청이 두 번 처리되면 전부 두 번 일어난다.

| 대상 | 1회 처리 | 2회 처리 |
|---|---|---|
| 주문 | 1건 | **2건** |
| 재고 선점 | 50 | **100** |
| 배송 | 1건 | **2건** |

재고가 실제보다 두 배로 잠기고, 배송 담당자도 두 명이 배정된다.
사용자는 한 번 주문했는데 물건이 두 번 온다.

### 1-2. 어떻게 두 번 들어오는가

의도적인 공격이 아니라 **정상적인 사용 중에** 생긴다.

- 사용자가 주문 버튼을 빠르게 두 번 누른다
- 서버는 처리했는데 응답이 유실돼 클라이언트가 재요청한다
- 모바일에서 네트워크가 끊겨 자동 재시도가 걸린다

특히 두 번째가 위험하다. 클라이언트 입장에서는 **성공했는지 알 수 없어서** 재시도가 정당하다.

### 1-3. 왜 지금까지 안 막혔나

기존에 있던 방어 장치들은 이 경우를 못 막는다.

| 장치 | 막는 것 | 이 문제를 막나 |
|---|---|---|
| `validateNoDuplicatedProduct` | 한 주문 안의 중복 상품 줄 | ❌ 주문이 2건이면 각각 정상 |
| `Inventory` 의 `@Version` | 같은 재고 행의 동시 수정 | ❌ 순차 요청이면 충돌 없음 |
| `INSUFFICIENT_STOCK` | 재고 초과 주문 | ❌ 재고가 넉넉하면 둘 다 통과 |

낙관적 락은 **같은 순간**에 부딪힐 때만 동작한다. 0.5초 간격의 두 클릭은 충돌 없이 둘 다 성공한다.

---

## 2. 설계

### 2-1. 왜 DB 유니크 제약이 아닌가

먼저 검토한 것은 `p_orders` 에 요청 식별자 컬럼을 두고 유니크 제약을 거는 방법이다.
**하지만 늦다.**

```
요청 도착
  → 재고 선점        ← 여기서 이미 부작용 발생
  → 주문 INSERT      ← 유니크 제약은 여기서야 걸린다
```

막아야 하는 것은 **주문 행의 중복**이 아니라 **처리 과정 전체의 중복**이다.
주문 행이 만들어지기 전에 재고 선점과 외부 호출이 이미 시작되므로, 진입 시점에 막아야 한다.

또한 실패해서 롤백된 요청도 유니크 제약이 남아 있으면 정상적인 재시도가 막힌다.

### 2-2. 왜 Redis인가

| 조건 | 이유 |
|---|---|
| 진입 시점에 즉시 판단 | DB 트랜잭션 밖에서 확인해야 한다 |
| 원자적 선점 | "없으면 넣기"가 한 번의 연산이어야 한다 |
| 자동 만료 | 서버가 죽어 정리를 못 해도 스스로 풀려야 한다 |
| 서버 여러 대에서 공유 | 인스턴스가 늘어나도 동작해야 한다 |

Redis 의 `SET key value NX EX` 가 네 조건을 모두 만족한다.
프로젝트 인프라(`docker-compose-infra.yaml`)에 이미 Redis 가 있고,
slack-service 도 같은 이유로 Redis 기반 중복 발송 방지를 쓰고 있다.

### 2-3. 클라이언트가 요청을 식별한다

"같은 요청"인지는 서버가 판단할 수 없다.
같은 상품을 두 번 주문하는 것은 정상이기 때문이다.

그래서 **클라이언트가 요청마다 새 키를 만들어 보낸다.**

```
POST /api/v1/orders
Idempotency-Key: 3f2a9c1e-...
```

재시도할 때는 **같은 키**를 보낸다. 새 주문이면 새 키를 만든다.
이 규약은 결제 API 들이 널리 쓰는 방식이다.

### 2-4. 상태를 셋으로 나눈다

키 하나가 세 가지 상태 중 하나를 가진다.

```
없음          → 처음 온 요청. 선점하고 진행한다
IN_PROGRESS   → 앞선 요청이 처리 중. 지금 진행하면 중복이다
<주문 ID>     → 이미 끝난 요청. 그때 만든 주문을 돌려준다
```

**두 번째와 세 번째를 구분하는 것이 중요하다.**
둘 다 "중복"이지만 응답이 달라야 한다.

| 상태 | 응답 | 이유 |
|---|---|---|
| 처리 중 | `409 DUPLICATE_ORDER_REQUEST` | 아직 결과가 없어 돌려줄 것이 없다 |
| 완료됨 | `201` + 기존 주문 | 클라이언트가 원한 결과를 이미 갖고 있다 |

완료된 요청에 오류를 주면 클라이언트는 주문이 실패한 줄 알고 계속 재시도한다.
**재요청에 정상 응답을 주는 것이 멱등성의 핵심**이다.

### 2-5. TTL 을 두 종류로 나눈 이유

```yaml
order:
  idempotency:
    in-progress-ttl-seconds: 300   # 5분
    completed-ttl-hours: 24
```

`IN_PROGRESS` 는 **짧아야 한다.** 서버가 처리 도중 죽으면 키를 정리하지 못하는데,
TTL 이 길면 그 키로는 영영 주문할 수 없다. 주문 한 건 처리(업체·사용자·배송 호출)보다는
넉넉하되 사람이 기다릴 만한 시간으로 5분을 잡았다.

완료 기록은 **길어야 한다.** 사용자가 한참 뒤에 재시도해도 중복이 만들어지면 안 된다.

---

## 3. 구현

### 3-1. 구조

기존 Port-Adapter 구조를 따랐다. Redis 를 쓴다는 사실이 애플리케이션 계층에 새지 않는다.

```
order/application/port/IdempotencyPort.java              포트
order/infrastructure/adapter/RedisIdempotencyAdapter.java 어댑터
order/application/facade/OrderFacade.java                 사용처
```

### 3-2. 포트 — 세 상태를 한 번에 돌려준다

```java
public interface IdempotencyPort {

    Reservation begin(String key);

    void complete(String key, UUID orderId);

    void release(String key);

    record Reservation(boolean acquired, UUID completedOrderId) {

        public static Reservation started()               { return new Reservation(true, null); }
        public static Reservation inProgress()             { return new Reservation(false, null); }
        public static Reservation completed(UUID orderId)  { return new Reservation(false, orderId); }

        public boolean isInProgress() {
            return !acquired && completedOrderId == null;
        }
    }
}
```

`begin()` 하나가 "확인 + 선점"을 함께 한다.
`exists()` 와 `save()` 로 나누면 **그 사이에 다른 요청이 끼어든다.**

### 3-3. 어댑터 — SETNX 한 번으로 선점

```java
@Override
public Reservation begin(String key) {
    String redisKey = redisKey(key);

    if (tryOccupy(redisKey)) {
        return Reservation.started();
    }

    String value = redisTemplate.opsForValue().get(redisKey);

    if (value == null) {
        // 방금 TTL 이 만료됐다. 한 번 더 선점을 시도하고, 그것도 실패하면 다른 요청에 양보한다
        return tryOccupy(redisKey) ? Reservation.started() : Reservation.inProgress();
    }

    if (IN_PROGRESS.equals(value)) {
        return Reservation.inProgress();
    }

    return Reservation.completed(UUID.fromString(value));
}

private boolean tryOccupy(String redisKey) {
    return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(redisKey, IN_PROGRESS, inProgressTtl));
}
```

`setIfAbsent` 가 Redis 의 `SET NX EX` 로 나간다. **한 번의 왕복이고 원자적이다.**

`value == null` 분기는 드물지만 실재하는 경우다.
`setIfAbsent` 는 실패했는데 값을 읽는 사이 TTL 이 만료된 상황으로,
여기서 그냥 막으면 **정상 요청이 이유 없이 거부된다.**

### 3-4. 키는 사용자별로 나눈다

```java
private String scopedKey(UUID receiverUserId, String idempotencyKey) {
    return receiverUserId + ":" + idempotencyKey;
}
```

클라이언트가 `1`, `order-1` 같은 짧은 값을 키로 쓰면 다른 사용자와 겹칠 수 있다.
겹치면 **남의 주문이 내 요청의 응답으로 돌아간다.** 정보 유출이다.

최종 Redis 키는 이렇게 된다.

```
order:idempotency:{userId}:{clientKey}
```

### 3-5. Facade — 진입점에서 판단

```java
public OrderResult create(OrderCreateCommand command, String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        // 키를 안 보내는 클라이언트도 주문은 할 수 있어야 한다. 대신 중복은 막지 못한다
        return doCreate(command);
    }

    String key = scopedKey(command.receiverUserId(), idempotencyKey);
    IdempotencyPort.Reservation reservation = idempotencyPort.begin(key);

    if (reservation.isInProgress()) {
        throw new BusinessException(ErrorCode.DUPLICATE_ORDER_REQUEST);   // 409
    }

    if (reservation.completedOrderId() != null) {
        return orderQueryService.getOrder(reservation.completedOrderId());
    }

    try {
        OrderResult created = doCreate(command);
        idempotencyPort.complete(key, created.orderId());
        return created;

    } catch (RuntimeException exception) {
        // 실패한 요청까지 막아두면 정상적인 재시도가 영영 안 된다
        idempotencyPort.release(key);
        throw exception;
    }
}
```

**실패 시 `release()` 가 특히 중요하다.** 재고 부족으로 실패한 요청의 키를 남겨두면,
재고가 채워진 뒤에도 같은 키로는 주문할 수 없다.

### 3-6. 헤더를 필수로 하지 않은 이유

`@RequestHeader(required = false)` 로 두었다.

필수로 만들면 헤더를 모르는 기존 클라이언트의 주문이 전부 400 이 된다.
**중복을 막자고 정상 주문을 못 받게 하는 것은 손해가 더 크다.**

같은 판단으로 Redis 장애 시에도 주문은 계속 받는다(fail-open).
방어 장치가 없어지는 것은 맞지만, Redis 하나 때문에 주문 전체가 멈추는 편이 더 나쁘다.

---

## 4. 동작

### 4-1. 정상 — 처음 오는 요청

```
클라이언트                     order                    Redis
    │  POST /orders             │                        │
    │  Idempotency-Key: A       │                        │
    │─────────────────────────▶ │  SET key A NX EX 300   │
    │                           │──────────────────────▶ │
    │                           │  ◀── OK (선점 성공)     │
    │                           │                        │
    │                           │  주문 생성 · 재고 선점 · 배송 생성
    │                           │                        │
    │                           │  SET key A={orderId} EX 24h
    │                           │──────────────────────▶ │
    │  ◀──── 201 주문 정보       │                        │
```

### 4-2. 재요청 — 응답을 못 받아 다시 보냄

```
    │  POST /orders             │                        │
    │  Idempotency-Key: A       │  SET key A NX          │
    │─────────────────────────▶ │──────────────────────▶ │
    │                           │  ◀── 실패 (이미 있음)   │
    │                           │  GET key A             │
    │                           │──────────────────────▶ │
    │                           │  ◀── {orderId}          │
    │                           │                        │
    │  ◀──── 201 같은 주문       │  주문을 다시 만들지 않는다
```

### 4-3. 동시 클릭 — 두 요청이 거의 같이 도착

```
요청1  SET key A NX  → OK      → 처리 진행
요청2  SET key A NX  → 실패     → GET → IN_PROGRESS → 409
```

### 4-4. 실패 후 재시도

```
요청1  선점 → 재고 부족(409) → release(key A)
요청2  (재고 입고 후) SET key A NX → OK → 정상 처리
```

---

## 5. 테스트

총 **11건**을 추가했고 전체 **68건이 통과**한다.

### 5-1. 어댑터 — Redis 연산 규칙 (`RedisIdempotencyAdapterTest`, 6건)

```java
@DisplayName("처음 오는 키는 선점에 성공한다")
// 조회 없이 한 번의 연산으로 끝나야 한다. 조회 후 저장이면 그 사이에 끼어들 수 있다
then(valueOperations).should().setIfAbsent(eq(REDIS_KEY), eq("IN_PROGRESS"), any(Duration.class));

@DisplayName("앞선 요청이 처리 중이면 선점하지 못한다")
@DisplayName("이미 끝난 요청이면 그때 만든 주문 ID 를 돌려준다")

@DisplayName("선점 직후 TTL 이 만료돼 값이 사라지면 한 번 더 시도한다")
// 만료된 키 때문에 정상 요청이 막히면 안 된다
assertThat(reservation.acquired()).isTrue();

@DisplayName("완료 기록은 주문 ID 로 덮어쓰고 더 긴 TTL 을 건다")
then(valueOperations).should().set(REDIS_KEY, orderId.toString(), Duration.ofHours(24));

@DisplayName("실패한 요청의 키는 지워 재시도를 허용한다")
```

**"한 번의 연산으로 선점한다"를 검증에 넣은 것이 핵심**이다.
나중에 누군가 `get()` 후 `set()` 으로 바꾸면 동작은 같아 보이지만 경쟁 조건이 생긴다.

### 5-2. Facade — 판단 규칙 (`OrderFacadeTest`, 5건)

```java
@DisplayName("같은 멱등 키의 재요청은 처음 만든 주문을 그대로 돌려준다")
// 주문을 다시 만들지 않는다. 만들면 재고가 두 번 잡힌다
then(orderCommandService).should(never()).create(any());
then(deliveryPort).shouldHaveNoInteractions();

@DisplayName("같은 키의 요청이 아직 처리 중이면 막는다")
.extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_ORDER_REQUEST);

@DisplayName("주문이 만들어지면 그 키를 완료로 기록한다")
then(idempotencyPort).should().complete(receiverUserId + ":key-1", orderId);

@DisplayName("주문이 실패하면 키를 풀어 다시 시도할 수 있게 한다")
then(idempotencyPort).should().release(receiverUserId + ":key-1");
then(idempotencyPort).should(never()).complete(anyString(), any());

@DisplayName("멱등 키가 없으면 중복 검사 없이 그대로 진행한다")
then(idempotencyPort).shouldHaveNoInteractions();
```

`complete(receiverUserId + ":key-1", ...)` 로 **사용자별 키 범위까지 고정**했다.
이게 깨지면 다른 사용자의 주문이 섞인다.

### 5-3. 아직 확인하지 못한 것

실제 Redis 서버와의 통신은 확인하지 못했다.
현재 테스트는 `StringRedisTemplate` 을 mock 으로 대체해서, **`SET NX EX` 명령이 실제로 그렇게
나가는지는 검증되지 않는다.**

통합 확인 때 아래를 볼 예정이다.

- 같은 `Idempotency-Key` 로 두 번 호출 시 주문이 1건만 생기는지
- `redis-cli` 로 `order:idempotency:*` 키와 TTL 이 실제로 걸리는지
- 서버를 강제 종료했을 때 `IN_PROGRESS` 가 5분 뒤 풀리는지

---

## 6. 정리

| 구분 | 내용 |
|---|---|
| **문제** | 같은 주문 요청이 두 번 처리되면 재고가 두 배로 잠기고 배송도 두 건 생긴다 |
| **원인** | 기존 방어 장치(낙관적 락 · 중복 상품 검증)는 순차 재요청을 막지 못한다 |
| **해결** | `Idempotency-Key` 헤더 + Redis `SET NX EX` 로 진입 시점에 원자적 선점 |
| **판단** | 처리 중은 409, 완료됨은 기존 주문 반환 — 재요청을 오류로 만들지 않는다 |
| **결과** | 테스트 11건 추가, 전체 68건 통과 |

### 남은 과제

- **실제 Redis 통합 확인** — 위 5-3
- **다른 API 로 확대** — 현재는 주문 접수에만 적용했다. 재고 입고(`POST /inventories/{id}/inbound`)도
  중복되면 수량이 두 배로 오르므로 같은 방식이 필요하다
- **Redis 장애 시 동작** — 지금은 fail-open 이라 예외가 그대로 올라간다.
  장애 시 중복 방지를 건너뛰고 주문은 받도록 할지 팀 논의가 필요하다
