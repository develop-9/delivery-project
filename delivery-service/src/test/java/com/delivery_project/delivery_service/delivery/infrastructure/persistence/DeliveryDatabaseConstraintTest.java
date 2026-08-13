package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.tracing.enabled=false",
        "system.id=00000000-0000-0000-0000-000000000001",

        // 이번 테스트는 Flyway Migration 자체에 정의된
        // DB 제약조건을 검증해야 하므로 Flyway를 실제로 실행한다.
        "spring.flyway.enabled=true",
        "spring.flyway.schemas=delivery_schema",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=0",
        "spring.flyway.out-of-order=true",

        // Hibernate가 테스트용 테이블을 새로 생성하지 않고,
        // Flyway가 만든 스키마를 기준으로 검증하도록 한다.
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=delivery_schema"
})
class DeliveryDatabaseConstraintTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("delivery_db")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("init-test-schema.sql");

    @DynamicPropertySource
    static void configureDatasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );
        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
            DELETE FROM delivery_schema.p_delivery_routes
            """);

        jdbcTemplate.update("""
            DELETE FROM delivery_schema.p_delivery_managers
            """);
    }

    @Test
    @DisplayName(
            "HUB_DELIVERY의 deliverySequence는 논리 삭제되지 않은 담당자 사이에서 중복될 수 없다"
    )
    void hubDeliverySequenceMustBeUnique() {

        // given
        int duplicateSequence = 0;

        insertDeliveryManager(
                UUID.randomUUID(),
                null,
                "HUB_DELIVERY",
                duplicateSequence
        );

        // when & then
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        insertDeliveryManager(
                                UUID.randomUUID(),
                                null,
                                "HUB_DELIVERY",
                                duplicateSequence
                        )
        );
    }

    @Test
    @DisplayName(
            "같은 허브의 COMPANY_DELIVERY 담당자는 동일한 deliverySequence를 가질 수 없다"
    )
    void companyDeliverySequenceMustBeUniqueWithinHub() {

        // given
        UUID hubId = UUID.randomUUID();
        int duplicateSequence = 0;

        insertDeliveryManager(
                UUID.randomUUID(),
                hubId,
                "COMPANY_DELIVERY",
                duplicateSequence
        );

        // when & then
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        insertDeliveryManager(
                                UUID.randomUUID(),
                                hubId,
                                "COMPANY_DELIVERY",
                                duplicateSequence
                        )
        );
    }

    @Test
    @DisplayName(
            "서로 다른 허브의 COMPANY_DELIVERY 담당자는 동일한 deliverySequence를 가질 수 있다"
    )
    void companyDeliverySequenceCanBeSameAcrossDifferentHubs() {

        // given
        int sameSequence = 0;

        // when & then
        insertDeliveryManager(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "COMPANY_DELIVERY",
                sameSequence
        );

        insertDeliveryManager(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "COMPANY_DELIVERY",
                sameSequence
        );
    }

    @Test
    @DisplayName(
            "논리 삭제된 HUB_DELIVERY 담당자의 deliverySequence는 재사용할 수 있다"
    )
    void deletedHubDeliverySequenceCanBeReused() {

        // given
        int reusedSequence = 0;

        UUID deletedManagerId =
                insertDeliveryManager(
                        UUID.randomUUID(),
                        null,
                        "HUB_DELIVERY",
                        reusedSequence
                );

        jdbcTemplate.update(
                """
                UPDATE delivery_schema.p_delivery_managers
                SET deleted_at = now(),
                    deleted_by = ?
                WHERE id = ?
                """,
                UUID.randomUUID(),
                deletedManagerId
        );

        // when & then
        insertDeliveryManager(
                UUID.randomUUID(),
                null,
                "HUB_DELIVERY",
                reusedSequence
        );
    }

    @Test
    @DisplayName(
            "DeliveryRoute의 sequence는 0보다 커야 한다"
    )
    void deliveryRouteSequenceMustBePositive() {

        // when & then
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        insertDeliveryRoute(0)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        insertDeliveryRoute(-1)
        );
    }

    private UUID insertDeliveryManager(
            UUID userId,
            UUID hubId,
            String type,
            int deliverySequence
    ) {
        UUID managerId = UUID.randomUUID();
        UUID systemId =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                );

        jdbcTemplate.update(
                """
                INSERT INTO delivery_schema.p_delivery_managers
                (
                    id,
                    user_id,
                    hub_id,
                    type,
                    status,
                    active,
                    delivery_sequence,
                    created_at,
                    created_by,
                    updated_at,
                    updated_by,
                    deleted_at,
                    deleted_by
                )
                VALUES
                (
                    ?,
                    ?,
                    ?,
                    ?,
                    'AVAILABLE',
                    TRUE,
                    ?,
                    now(),
                    ?,
                    now(),
                    ?,
                    NULL,
                    NULL
                )
                """,
                managerId,
                userId,
                hubId,
                type,
                deliverySequence,
                systemId,
                systemId
        );

        return managerId;
    }

    private void insertDeliveryRoute(
            int sequence
    ) {
        UUID systemId =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                );

        jdbcTemplate.update(
                """
                INSERT INTO delivery_schema.p_delivery_routes
                (
                    id,
                    delivery_id,
                    sequence,
                    departure_hub_id,
                    arrival_hub_id,
                    estimated_distance_km,
                    estimated_duration_min,
                    actual_distance_km,
                    actual_duration_min,
                    status,
                    delivery_manager_id,
                    created_at,
                    created_by,
                    updated_at,
                    updated_by,
                    deleted_at,
                    deleted_by
                )
                VALUES
                (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    100.00,
                    60,
                    NULL,
                    NULL,
                    'WAITING',
                    NULL,
                    now(),
                    ?,
                    now(),
                    ?,
                    NULL,
                    NULL
                )
                """,
                UUID.randomUUID(),
                UUID.randomUUID(),
                sequence,
                UUID.randomUUID(),
                UUID.randomUUID(),
                systemId,
                systemId
        );
    }
}