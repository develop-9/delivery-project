-- postgres 컨테이너가 "처음 초기화될 때" 1회만 실행된다.
-- 이미 postgres-data 볼륨이 있으면 실행되지 않는다. 다시 돌리려면 볼륨을 지운다.
--   docker compose -f docker/docker-compose-infra.yaml down -v
--
-- DB 는 delivery 하나만 쓰고 서비스별로 스키마를 나눈다
-- delivery DB 자체는 compose 의 POSTGRES_DB 가 만들고, 이 스크립트는 그 DB 안에서 실행된다.
-- Hibernate 는 CREATE SCHEMA 를 내지 않으므로 스키마는 여기서 미리 만들어야 한다.
--
-- 서비스별 스키마는 각 담당자가 아래에 자기 것을 추가한다.
-- (ERD 문서 「서비스 / 스키마 매핑」 기준: user_db / hub_db / company_db / order_db / delivery_db / slack_db)

CREATE SCHEMA IF NOT EXISTS hub_db;
