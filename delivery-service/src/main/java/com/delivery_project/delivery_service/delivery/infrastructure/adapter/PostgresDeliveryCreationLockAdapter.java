package com.delivery_project.delivery_service.delivery.infrastructure.adapter;

import com.delivery_project.delivery_service.delivery.application.port.DeliveryCreationLockPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostgresDeliveryCreationLockAdapter
        implements DeliveryCreationLockPort {

    private final EntityManager entityManager;

    @Override
    public void lock(UUID orderId) { // 트랜잭션이 끝나면 자동으로 풀리는 Lock 사용
        entityManager.createNativeQuery("""
                SELECT pg_advisory_xact_lock(  
                    hashtextextended(CAST(:orderId AS text), 0)
                )
                """)
                .setParameter("orderId", orderId.toString())
                .getSingleResult();
    }
}