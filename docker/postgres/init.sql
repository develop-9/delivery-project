-- postgres 컨테이너가 "처음 초기화될 때" 1회만 실행된다.
-- 이미 postgres-data 볼륨이 있으면 실행되지 않는다. 다시 돌리려면 볼륨을 지운다.
--   docker compose -f docker/docker-compose-infra.yaml down -v
--
-- DB는 delivery_db 하나만 쓰고 서비스별로 스키마를 나눈다
-- (PROJECT_REFERENCE.md 4절 기준: user_schema / hub_schema / company_schema / order_schema / delivery_schema / slack_schema)
-- delivery_db 자체는 compose의 POSTGRES_DB가 만들고, 이 스크립트는 그 DB 안에서 실행된다.
-- Hibernate는 CREATE SCHEMA를 내지 않으므로 스키마는 여기서 미리 만들어야 한다.
--
-- 서비스별 스키마는 각 담당자가 아래에 자기 것을 추가한다.

CREATE DATABASE hub_db;

CREATE SCHEMA IF NOT EXISTS user_schema;
