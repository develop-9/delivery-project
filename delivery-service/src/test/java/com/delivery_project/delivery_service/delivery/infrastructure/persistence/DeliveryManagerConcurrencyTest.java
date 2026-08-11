package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.tracing.enabled=false",
        "system.id=00000000-0000-0000-0000-000000000001",

        "spring.flyway.enabled=false",

        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=delivery_schema"
})
class DeliveryManagerConcurrencyTest {

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
    private SpringDataDeliveryManagerRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.flush();
    }

    @Test
    @DisplayName("비활성화된 배송 담당자는 라운드로빈 배정 후보에서 제외된다")
    void inactiveManagerIsExcludedFromRoundRobin() {

        // given
        DeliveryManager inactiveManager =
                DeliveryManager.create(
                        UUID.randomUUID(),
                        null,
                        DeliveryManagerType.HUB_DELIVERY,
                        0
                );

        inactiveManager.deactivate();

        DeliveryManager activeManager =
                DeliveryManager.create(
                        UUID.randomUUID(),
                        null,
                        DeliveryManagerType.HUB_DELIVERY,
                        1
                );

        repository.saveAndFlush(inactiveManager);
        repository.saveAndFlush(activeManager);

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        // when
        DeliveryManager selectedManager =
                transactionTemplate.execute(
                        status ->
                                repository
                                        .findFirstByTypeAndStatusAndActiveTrueAndDeliverySequenceGreaterThanAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                                                DeliveryManagerType.HUB_DELIVERY,
                                                DeliveryManagerStatus.AVAILABLE,
                                                -1
                                        )
                                        .orElseThrow()
                );

        // then
        assertNotNull(selectedManager);

        assertEquals(
                activeManager.getId(),
                selectedManager.getId()
        );

        assertNotEquals(
                inactiveManager.getId(),
                selectedManager.getId()
        );
    }

    @Test
    @DisplayName("배송 담당자 정지 처리와 라운드로빈 배정 조회는 PESSIMISTIC_WRITE 락으로 직렬화된다")
    void deactivateAndAssignmentAreSerializedByPessimisticWrite()
            throws Exception {

        // given
        UUID userId = UUID.randomUUID();

        DeliveryManager manager =
                DeliveryManager.create(
                        userId,
                        null,
                        DeliveryManagerType.HUB_DELIVERY,
                        0
                );

        repository.saveAndFlush(manager);

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch firstLockAcquired =
                new CountDownLatch(1);

        CountDownLatch releaseFirstTransaction =
                new CountDownLatch(1);

        CountDownLatch secondTransactionStarted =
                new CountDownLatch(1);

        try {
            /*
             * 정지 요청을 가정한다.
             * userId 기준으로 DeliveryManager row를 먼저 잠근다.
             */
            Future<Void> firstFuture =
                    executor.submit(() -> {

                        transactionTemplate.executeWithoutResult(
                                status -> {

                                    DeliveryManager lockedManager =
                                            repository
                                                    .findByUserIdForUpdate(userId)
                                                    .orElseThrow();

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

                                    lockedManager.deactivate();
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

            /*
             * 배송 배정을 가정한다.
             * active=true인 라운드로빈 후보를 조회하려고 한다.
             */
            Future<Optional<DeliveryManager>> secondFuture =
                    executor.submit(() ->
                            transactionTemplate.execute(
                                    status -> {

                                        secondTransactionStarted.countDown();

                                        return repository
                                                .findFirstByTypeAndStatusAndActiveTrueAndDeliverySequenceGreaterThanAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                                                        DeliveryManagerType.HUB_DELIVERY,
                                                        DeliveryManagerStatus.AVAILABLE,
                                                        -1
                                                );
                                    }
                            )
                    );

            assertTrue(
                    secondTransactionStarted.await(
                            3,
                            TimeUnit.SECONDS
                    )
            );

            /*
             * 첫 번째 Transaction이 같은 DeliveryManager row를
             * 잠그고 있으므로 배정 후보 조회가 끝나면 안 된다.
             */
            assertThrows(
                    TimeoutException.class,
                    () -> secondFuture.get(
                            500,
                            TimeUnit.MILLISECONDS
                    )
            );

            /*
             * 정지 Transaction 종료 허용
             * → active=false
             * → COMMIT
             * → Lock 해제
             */
            releaseFirstTransaction.countDown();

            assertTimeoutPreemptively(
                    Duration.ofSeconds(5),
                    () -> {
                        firstFuture.get();
                    }
            );

            /*
             * 배정 조회는 락이 풀린 뒤 실행되고,
             * manager가 active=false이므로 후보에서 제외되어야 한다.
             */
            Optional<DeliveryManager> selected =
                    assertTimeoutPreemptively(
                            Duration.ofSeconds(5),
                            () -> secondFuture.get()
                    );

            assertTrue(selected.isEmpty());

        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
        }
    }
}