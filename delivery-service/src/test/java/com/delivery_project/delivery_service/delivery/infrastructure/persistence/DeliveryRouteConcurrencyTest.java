package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.tracing.enabled=false",
        "system.id=00000000-0000-0000-0000-000000000001",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=delivery_schema"
})
class DeliveryRouteConcurrencyTest {

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
    private SpringDataDeliveryRouteRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("같은 DeliveryRoute를 동시에 조회하면 PESSIMISTIC_WRITE 락으로 직렬화된다")
    void pessimisticWriteLockSerializesConcurrentRouteAccess()
            throws Exception {

        // given
        DeliveryRoute route =
                DeliveryRoute.create(
                        UUID.randomUUID(),
                        1,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100.00"),
                        60
                );

        repository.saveAndFlush(route);

        UUID routeId = route.getId();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch firstLockAcquired =
                new CountDownLatch(1);

        CountDownLatch releaseFirstTransaction =
                new CountDownLatch(1);

        CountDownLatch secondTransactionStarted =
                new CountDownLatch(1);

        try {
            Future<Void> firstFuture =
                    executor.submit(() -> {

                        transactionTemplate.executeWithoutResult(
                                status -> {

                                    repository.findByIdForUpdate(
                                            routeId
                                    ).orElseThrow();

                                    firstLockAcquired.countDown();

                                    try {
                                        releaseFirstTransaction.await(
                                                5,
                                                TimeUnit.SECONDS
                                        );
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        throw new RuntimeException(e);
                                    }
                                }
                        );

                        return null;
                    });

            assertTrue(
                    firstLockAcquired.await(
                            3,
                            TimeUnit.SECONDS
                    )
            );

            Future<Void> secondFuture =
                    executor.submit(() -> {

                        transactionTemplate.executeWithoutResult(
                                status -> {

                                    secondTransactionStarted.countDown();

                                    repository.findByIdForUpdate(
                                            routeId
                                    ).orElseThrow();
                                }
                        );

                        return null;
                    });

            assertTrue(
                    secondTransactionStarted.await(
                            3,
                            TimeUnit.SECONDS
                    )
            );

            // 첫 번째 Transaction이 Lock을 잡고 있으므로
            // 두 번째 Transaction은 아직 끝나면 안 됨
            assertThrows(
                    TimeoutException.class,
                    () -> secondFuture.get(
                            500,
                            TimeUnit.MILLISECONDS
                    )
            );

            releaseFirstTransaction.countDown();

            assertTimeoutPreemptively(
                    Duration.ofSeconds(5),
                    () -> firstFuture.get()
            );

            assertTimeoutPreemptively(
                    Duration.ofSeconds(5),
                    () -> secondFuture.get()
            );

        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
        }
    }
}