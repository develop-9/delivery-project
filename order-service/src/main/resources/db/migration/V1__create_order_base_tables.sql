-- order-service 기본 테이블.
--
-- 이 파일이 생긴 이유:
--   prod 프로파일은 ddl-auto: validate 라 테이블을 만들지 않는다. 그런데 배포 경로 어디에도
--   테이블을 만드는 주체가 없어서(docker/postgres/init.sql 은 CREATE SCHEMA 만 한다)
--   EC2 새 DB 에서 order-service 가 validate 실패로 무한 재기동에 빠졌다.
--   이제 Flyway 가 스키마를 소유한다.
--
-- 내용은 로컬에서 ddl-auto 가 만든 스키마를 pg_dump 로 받아 옮긴 것이라 엔티티와 1:1 이다.
-- 손으로 옮겨 적은 것이 아니므로 timestamp(6) with time zone, version bigint NOT NULL 같은
-- Hibernate 의 실제 매핑이 그대로 들어가 있다. 타입이 어긋나면 validate 가 바로 튕긴다.
--
-- ⚠️ 이미 ddl-auto 로 테이블이 만들어져 있는 기존 로컬 DB 에서도 돌아가야 한다.
--    baseline-on-migrate 가 baseline 을 찍은 뒤 이 V1 을 실행하기 때문이다.
--    그래서 전부 IF NOT EXISTS 이고, 제약도 CREATE TABLE 안에 인라인으로 둔다
--    (ALTER TABLE ADD CONSTRAINT 는 멱등하지 않아 두 번째 실행에서 깨진다).

CREATE TABLE IF NOT EXISTS order_schema.p_orders (
    id                  uuid                        NOT NULL,
    supplier_company_id uuid                        NOT NULL,
    receiver_company_id uuid                        NOT NULL,
    receiver_user_id    uuid                        NOT NULL,
    request_details     text,
    status              character varying(30)       NOT NULL,
    created_at          timestamp(6) with time zone NOT NULL,
    created_by          uuid                        NOT NULL,
    updated_at          timestamp(6) with time zone NOT NULL,
    updated_by          uuid                        NOT NULL,
    deleted_at          timestamp(6) with time zone,
    deleted_by          uuid,

    CONSTRAINT p_orders_pkey PRIMARY KEY (id),
    -- Hibernate 가 @Enumerated(STRING) 에 자동으로 걸던 제약. 이름도 그대로 맞춘다.
    -- OrderStatus 에 값을 추가하면 이 제약도 새 마이그레이션으로 같이 고쳐야 한다.
    CONSTRAINT p_orders_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELED', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS order_schema.p_order_items (
    id           uuid                        NOT NULL,
    order_id     uuid                        NOT NULL,
    product_id   uuid                        NOT NULL,
    quantity     integer                     NOT NULL,
    inventory_id uuid                        NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    updated_at   timestamp(6) with time zone NOT NULL,

    CONSTRAINT p_order_items_pkey PRIMARY KEY (id),
    -- 같은 주문에 같은 상품이 두 줄로 들어가지 못하게 한다 (Order.addItem 의 DB 쪽 안전망)
    CONSTRAINT uk_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES order_schema.p_orders (id)
);

CREATE TABLE IF NOT EXISTS order_schema.p_inventories (
    id                uuid                        NOT NULL,
    product_id        uuid                        NOT NULL,
    hub_id            uuid                        NOT NULL,
    -- 내부 API(상품 등록 연동)는 productId 만 받아서 비어 있을 수 있다. 엔티티 주석 참고.
    company_id        uuid,
    quantity          integer                     NOT NULL,
    reserved_quantity integer                     NOT NULL,
    -- @Version. Hibernate 가 NOT NULL 로 만든다 (엔티티의 @Column 에는 표기가 없다).
    version           bigint                      NOT NULL,
    created_at        timestamp(6) with time zone NOT NULL,
    created_by        uuid                        NOT NULL,
    updated_at        timestamp(6) with time zone NOT NULL,
    updated_by        uuid                        NOT NULL,
    deleted_at        timestamp(6) with time zone,
    deleted_by        uuid,

    CONSTRAINT p_inventories_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS order_schema.p_order_snapshots (
    id                    uuid                        NOT NULL,
    -- p_orders.id 를 가리키지만 FK 를 걸지 않는다. 이력은 주문이 사라져도 남아야 한다.
    order_id              uuid                        NOT NULL,
    sequence              integer                     NOT NULL,
    event_type            character varying(30)       NOT NULL,
    order_status          character varying(30)       NOT NULL,
    supplier_company_name character varying(100),
    receiver_company_name character varying(100),
    origin_hub_name       character varying(100),
    dest_hub_name         character varying(100),
    request_details       text,
    created_at            timestamp(6) with time zone NOT NULL,
    created_by            uuid                        NOT NULL,
    deleted_at            timestamp(6) with time zone,
    deleted_by            uuid,

    CONSTRAINT p_order_snapshots_pkey PRIMARY KEY (id),
    CONSTRAINT uk_order_sequence UNIQUE (order_id, sequence),
    CONSTRAINT p_order_snapshots_event_type_check CHECK (event_type IN
        ('ORDER_CREATED', 'ORDER_MODIFIED', 'DELIVERY_CONFIRMED',
         'ORDER_COMPLETED', 'ORDER_CANCELED', 'ORDER_FAILED')),
    CONSTRAINT p_order_snapshots_order_status_check CHECK (order_status IN
        ('PENDING', 'CONFIRMED', 'CANCELED', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS order_schema.p_order_item_snapshots (
    id           uuid                        NOT NULL,
    snapshot_id  uuid                        NOT NULL,
    product_id   uuid                        NOT NULL,
    -- company-service 연동 전에는 상품명을 알 수 없어 nullable 이다. 엔티티 TODO 참고.
    product_name character varying(100),
    quantity     integer                     NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,

    CONSTRAINT p_order_item_snapshots_pkey PRIMARY KEY (id),
    CONSTRAINT uk_snapshot_product UNIQUE (snapshot_id, product_id),
    CONSTRAINT fk_order_item_snapshots_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES order_schema.p_order_snapshots (id)
);


-- 엔티티의 @Table(indexes = ...) 에 선언된 인덱스들
CREATE INDEX IF NOT EXISTS idx_orders_status_created  ON order_schema.p_orders (status, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_supplier        ON order_schema.p_orders (supplier_company_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_receiver        ON order_schema.p_orders (receiver_company_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_receiver_user   ON order_schema.p_orders (receiver_user_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product    ON order_schema.p_order_items (product_id);
CREATE INDEX IF NOT EXISTS idx_inventories_product_hub ON order_schema.p_inventories (product_id, hub_id);
CREATE INDEX IF NOT EXISTS idx_snapshots_order        ON order_schema.p_order_snapshots (order_id, sequence);
