-- 원래 db/schema.sql 에 있던 것을 그대로 옮겼다.
-- 그 파일은 local 프로파일의 spring.sql.init 로만 실행돼서 prod 에는 이 안전망이 통째로
-- 빠져 있었다. Flyway 로 옮기면서 두 프로파일 모두에 적용된다.
--
-- ddl-auto 가 만들지 못하는 것들이다: 부분 인덱스와 CHECK 제약.

-- ⭐ 살아있는 행에만 유니크. 일반 UNIQUE 를 걸면 삭제 후 같은 (상품, 허브) 재등록이 영원히 불가해진다.
--    애플리케이션의 중복 체크만으로는 동시 요청 시 같은 재고가 두 줄 생길 수 있다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_alive
    ON order_schema.p_inventories (product_id, hub_id) WHERE deleted_at IS NULL;

-- 검색에서 매번 타는 조건들
CREATE INDEX IF NOT EXISTS idx_orders_alive_status
    ON order_schema.p_orders (status, created_at DESC) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_snapshots_order_seq
    ON order_schema.p_order_snapshots (order_id, sequence DESC) WHERE deleted_at IS NULL;

-- 수량 불변식. 엔티티가 이미 막지만, 배치나 수동 UPDATE 로 새어 들어오는 것까지는 못 막는다.
-- 금액은 order 가 소유하지 않으므로(company-service 의 p_products) 여기서 걸 것이 없다.
--
-- 이미 제약이 있는 기존 로컬 DB 에서도 돌아야 해서 DROP IF EXISTS → ADD 순서로 건다.
ALTER TABLE order_schema.p_inventories DROP CONSTRAINT IF EXISTS ck_inventory_quantity;
ALTER TABLE order_schema.p_inventories ADD CONSTRAINT ck_inventory_quantity
    CHECK (quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= quantity);

ALTER TABLE order_schema.p_order_items DROP CONSTRAINT IF EXISTS ck_order_item_quantity;
ALTER TABLE order_schema.p_order_items ADD CONSTRAINT ck_order_item_quantity
    CHECK (quantity >= 1);

ALTER TABLE order_schema.p_order_item_snapshots DROP CONSTRAINT IF EXISTS ck_snapshot_item_quantity;
ALTER TABLE order_schema.p_order_item_snapshots ADD CONSTRAINT ck_snapshot_item_quantity
    CHECK (quantity >= 1);
