-- hub-service 스키마 정의.
--
-- 출처: docs/api/00_common.md 「DDL 제약」. 문서가 아니라 이 파일이 실행 대상이다.
-- 문서와 어긋나면 둘 중 하나가 틀린 것이므로 같이 고친다.
--
-- 왜 필요한가
--   ddl-auto 는 부분 인덱스(WHERE deleted_at IS NULL)와 CHECK 제약을 만들지 못한다.
--   논리 삭제를 쓰는 이 서비스에서 그 둘이 빠지면
--     - 삭제된 허브명이 자리를 계속 차지하거나(전체 유니크), 중복 허브명이 그냥 들어가거나(제약 없음)
--     - MAIN 인데 남을 가리키는 허브가 DB 에 남는다
--
-- 언제 실행되나
--   local  : Hibernate 가 테이블을 만든 뒤 애플리케이션이 자동 실행한다
--            (application.yaml 의 spring.sql.init + defer-datasource-initialization)
--   prod   : ddl-auto 가 validate 라 앱이 스키마를 못 만든다. 배포 전에 사람이 직접 적용한다.
--              psql -h <host> -U <user> -d delivery_db -f schema.sql
--
-- 두 가지 성질을 지킨다. 새 문장을 추가할 때도 마찬가지다.
--   멱등     : 몇 번을 돌려도 결과가 같다
--              (PostgreSQL 은 ADD CONSTRAINT 에 IF NOT EXISTS 가 없어 DROP 을 먼저 건다)
--   무부작용 : SET search_path 를 쓰지 않고 스키마를 매번 한정한다.
--              local 에서는 커넥션 풀의 커넥션으로 실행되므로, 세션 설정을 바꾸면
--              그 커넥션이 풀에 반납된 뒤 다른 쿼리까지 영향을 받는다.

-- ============================================================
-- 접속 대상 확인
-- ============================================================
-- 다른 DB 에 붙은 채 실행하면 엉뚱한 DB 에 hub_schema 와 테이블이 생긴다. 먼저 막는다.
--
-- psql 메타커맨드 \connect 를 쓰지 않는 이유와, 본문을 달러 인용($$)이 아니라
-- 홑따옴표로 감싼 이유는 같다 — local 에서는 Spring(ScriptUtils)이 이 파일을 JDBC 로 실행한다.
-- \connect 는 SQL 이 아니라 구문 오류가 나고, ScriptUtils 는 달러 인용을 몰라서
-- $$ 본문 안의 세미콜론에서 문장을 잘라버린다. 홑따옴표는 인식하므로 안전하다.
-- 문자열 안의 홑따옴표는 '' 로 쓴다.

DO '
BEGIN
    IF current_database() <> ''delivery_db'' THEN
        RAISE EXCEPTION ''schema.sql 은 delivery_db 에서 실행해야 한다. 현재 접속: %'', current_database();
    END IF;
END' LANGUAGE plpgsql;


CREATE SCHEMA IF NOT EXISTS hub_schema;


-- ============================================================
-- 테이블
-- ============================================================
-- local 에서는 Hibernate 가 먼저 만들어 두므로 IF NOT EXISTS 로 건너뛴다.
-- 여기 정의는 prod 처럼 빈 DB 에 처음 올릴 때 쓰인다.
--
-- 감사 컬럼 6개는 BaseEntity / BaseDeletableEntity 에서 온다.
-- created_by / updated_by 가 NOT NULL 인 것은 팀 공통 파일의 정의를 그대로 따른 것이다.

CREATE TABLE IF NOT EXISTS hub_schema.p_hubs (
    id            UUID          NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    address       VARCHAR(255)  NOT NULL,
    latitude      NUMERIC(10, 7),
    longitude     NUMERIC(10, 7),
    hub_type      VARCHAR(20)   NOT NULL,
    parent_hub_id UUID,

    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    UUID          NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_by    UUID          NOT NULL,
    deleted_at    TIMESTAMP(6) WITH TIME ZONE,
    deleted_by    UUID,

    CONSTRAINT pk_hubs PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS hub_schema.p_hub_routes (
    id                UUID           NOT NULL,
    departure_hub_id  UUID           NOT NULL,
    arrival_hub_id    UUID           NOT NULL,
    distance_km       NUMERIC(10, 2) NOT NULL,
    duration_min      INTEGER        NOT NULL,

    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by        UUID           NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_by        UUID           NOT NULL,
    deleted_at        TIMESTAMP(6) WITH TIME ZONE,
    deleted_by        UUID,

    CONSTRAINT pk_hub_routes PRIMARY KEY (id)
);


-- ============================================================
-- 외래키
-- ============================================================
-- p_hubs 와 p_hub_routes 는 같은 스키마 안이라 실제 FK 를 걸 수 있다 (ERD 문서).
-- 엔티티는 @ManyToOne 이 아니라 UUID 로 들고 있어 Hibernate 가 FK 를 만들지 않으므로 여기서 건다.
--
-- 논리 삭제라 참조 대상 행이 사라지지 않는다. 그래서 ON DELETE 정책이 필요 없다.
-- parent_hub_id 자기참조는 MAIN 이 자기 자신을 가리키는 D1 때문이며,
-- PostgreSQL 은 같은 INSERT 문 안의 자기참조를 허용한다.

ALTER TABLE hub_schema.p_hubs DROP CONSTRAINT IF EXISTS fk_hubs_parent;
ALTER TABLE hub_schema.p_hubs ADD CONSTRAINT fk_hubs_parent
    FOREIGN KEY (parent_hub_id) REFERENCES hub_schema.p_hubs (id);

ALTER TABLE hub_schema.p_hub_routes DROP CONSTRAINT IF EXISTS fk_hub_routes_departure;
ALTER TABLE hub_schema.p_hub_routes ADD CONSTRAINT fk_hub_routes_departure
    FOREIGN KEY (departure_hub_id) REFERENCES hub_schema.p_hubs (id);

ALTER TABLE hub_schema.p_hub_routes DROP CONSTRAINT IF EXISTS fk_hub_routes_arrival;
ALTER TABLE hub_schema.p_hub_routes ADD CONSTRAINT fk_hub_routes_arrival
    FOREIGN KEY (arrival_hub_id) REFERENCES hub_schema.p_hubs (id);


-- ============================================================
-- CHECK 제약  (ddl-auto 가 못 만드는 것 1)
-- ============================================================

-- MAIN 이면 parent_hub_id 가 자기 자신, SUB 이면 자기 자신이 아니어야 한다 (D1).
-- 이게 없으면 MAIN 인데 남을 가리키는 허브가 들어가고, 경로 산출(D2)이 조용히 틀린 답을 낸다.
-- mainOf(h) = h.parentHubId 라는 전제가 깨지기 때문이다.
ALTER TABLE hub_schema.p_hubs DROP CONSTRAINT IF EXISTS ck_hub_type_parent;
ALTER TABLE hub_schema.p_hubs ADD CONSTRAINT ck_hub_type_parent CHECK (
    (hub_type = 'MAIN' AND parent_hub_id =  id) OR
    (hub_type = 'SUB'  AND parent_hub_id <> id)
);

-- 자기 자신으로의 구간은 만들지 않는다. parent_hub_id 자기참조는 계층 표현일 뿐이다.
ALTER TABLE hub_schema.p_hub_routes DROP CONSTRAINT IF EXISTS ck_hub_route_not_self;
ALTER TABLE hub_schema.p_hub_routes ADD CONSTRAINT ck_hub_route_not_self
    CHECK (departure_hub_id <> arrival_hub_id);


-- ============================================================
-- 부분 유니크 인덱스  (ddl-auto 가 못 만드는 것 2)
-- ============================================================
-- WHERE deleted_at IS NULL 이 핵심이다. 이 조건이 빠진 전체 유니크로 걸면
-- 논리 삭제한 허브명·구간을 다시 등록할 수 없게 된다.
--
-- Service 의 existsBy... 검사만으로는 동시 요청을 막지 못한다(검사와 저장 사이가 벌어진다).
-- 중복을 실제로 막는 것은 이 두 인덱스다.

CREATE UNIQUE INDEX IF NOT EXISTS uq_hub_name
    ON hub_schema.p_hubs (name) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_hub_route
    ON hub_schema.p_hub_routes (departure_hub_id, arrival_hub_id) WHERE deleted_at IS NULL;


-- ============================================================
-- 조회 인덱스
-- ============================================================

-- 목록 기본 정렬이 createdAt desc 다 (공통 페이징 규약).
CREATE INDEX IF NOT EXISTS idx_hubs_created_at
    ON hub_schema.p_hubs (created_at DESC) WHERE deleted_at IS NULL;

-- parentHubId 로 관할 허브를 찾는다 (목록 검색 · 하위 허브 판정).
CREATE INDEX IF NOT EXISTS idx_hubs_parent
    ON hub_schema.p_hubs (parent_hub_id) WHERE deleted_at IS NULL;

-- 경로 산출이 구간마다 (출발, 도착) 으로 조회한다 (D2).
CREATE INDEX IF NOT EXISTS idx_hubroutes_dep_arr
    ON hub_schema.p_hub_routes (departure_hub_id, arrival_hub_id) WHERE deleted_at IS NULL;
