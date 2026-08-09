package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManagerSequence;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryManagerSequenceRepository
        extends JpaRepository<DeliveryManagerSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 비관적 락
    @Query("""
            SELECT dms
            FROM DeliveryManagerSequence dms
            WHERE dms.managerType = :managerType
              AND (
                    (:hubId IS NULL AND dms.hubId IS NULL)
                    OR dms.hubId = :hubId
              )
            """)
    Optional<DeliveryManagerSequence> findForUpdate(
            @Param("managerType") DeliveryManagerType managerType,
            @Param("hubId")  UUID hubId
    );

    @Modifying
    @Query(
            value = """
                INSERT INTO delivery_schema.p_delivery_manager_sequences
                (
                    id,
                    manager_type,
                    hub_id,
                    next_sequence,
                    last_assigned_sequence
                )
                VALUES
                (
                    gen_random_uuid(),
                    :managerType,
                    :hubId,
                    0,
                    -1
                )
                ON CONFLICT DO NOTHING
                """,
            nativeQuery = true
    )
    void createIfAbsent(
            @Param("managerType") String managerType,
            @Param("hubId") UUID hubId
    );
}
