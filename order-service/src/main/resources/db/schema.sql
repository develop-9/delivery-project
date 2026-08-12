-- ddl-auto 가 테이블을 만든 뒤 실행된다 (defer-datasource-initialization: true).
-- 여기에는 ddl-auto 가 만들지 못하는 것만 둔다: 부분 인덱스와 CHECK 제약.
--
-- ⚠️ Spring 의 스크립트 실행기는 ';' 로 문장을 자르고 PL/pgSQL 의 $$ 인용을 이해하지 못한다.
--    DO $$ ... $$ 블록을 쓰면 중간에서 잘려 "Unterminated dollar quote" 로 깨진다.
--    그래서 제약은 DROP IF EXISTS → ADD 순서로 걸어 멱등성을 확보한다.

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
ALTER TABLE order_schema.p_inventories DROP CONSTRAINT IF EXISTS ck_inventory_quantity;
ALTER TABLE order_schema.p_inventories ADD CONSTRAINT ck_inventory_quantity
    CHECK (quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= quantity);

ALTER TABLE order_schema.p_order_items DROP CONSTRAINT IF EXISTS ck_order_item_quantity;
ALTER TABLE order_schema.p_order_items ADD CONSTRAINT ck_order_item_quantity
    CHECK (quantity >= 1);

ALTER TABLE order_schema.p_order_item_snapshots DROP CONSTRAINT IF EXISTS ck_snapshot_item_quantity;
ALTER TABLE order_schema.p_order_item_snapshots ADD CONSTRAINT ck_snapshot_item_quantity
    CHECK (quantity >= 1);
