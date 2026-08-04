-- postgres 컨테이너가 "처음 초기화될 때" 1회만 실행된다.
-- 이미 postgres-data 볼륨이 있으면 실행되지 않는다. 다시 돌리려면 볼륨을 지운다.
--   docker compose -f docker/docker-compose-infra.yaml down -v
--
-- 서비스별 DB 는 각 담당자가 아래에 자기 것을 추가한다.
-- (ERD 문서 「서비스 / DB 매핑」 기준: user_db / hub_db / company_db / order_db / delivery_db / slack_db)

CREATE DATABASE hub_db;
