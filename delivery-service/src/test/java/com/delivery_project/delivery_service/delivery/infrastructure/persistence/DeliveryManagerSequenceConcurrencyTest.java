package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManagerSequence;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
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
class DeliveryManagerSequenceConcurrencyTest {

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
    private SpringDataDeliveryManagerSequenceRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("같은 DeliveryManagerSequence를 동시에 조회하면 PESSIMISTIC_WRITE 락으로 직렬화된다")
    void pessimisticWriteLockSerializesConcurrentAccess()
            throws Exception {

        // given
        UUID hubId = UUID.randomUUID();

        DeliveryManagerSequence sequence =
                DeliveryManagerSequence.create(
                        DeliveryManagerType.COMPANY_DELIVERY,
                        hubId
                );

        repository.saveAndFlush(sequence);

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

                                    repository.findForUpdate(
                                            DeliveryManagerType.COMPANY_DELIVERY,
                                            hubId
                                    ).orElseThrow();

                                    // 첫 번째 트랜잭션이 Lock 획득
                                    firstLockAcquired.countDown();

                                    try {
                                        // main thread가 허용할 때까지 Lock 유지
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

                                    /*
                                     * 첫 번째 Transaction이 같은 row를
                                     * PESSIMISTIC_WRITE로 잡고 있으므로
                                     * 여기서 기다려야 한다.
                                     */
                                    repository.findForUpdate(
                                            DeliveryManagerType.COMPANY_DELIVERY,
                                            hubId
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

            /*
             * 첫 번째 Transaction이 Lock을 쥐고 있는 동안
             * 두 번째 Transaction은 완료되면 안 된다.
             */
            assertThrows(
                    TimeoutException.class,
                    () -> secondFuture.get(
                            500,
                            TimeUnit.MILLISECONDS
                    )
            );

            // 첫 번째 Transaction 종료 허용 → COMMIT → Lock 해제
            releaseFirstTransaction.countDown();

            assertTimeoutPreemptively(
                    Duration.ofSeconds(5),
                    () -> firstFuture.get()
            );

            /*
             * Lock 해제 이후에는 두 번째 Transaction도
             * 정상적으로 완료되어야 한다.
             */
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