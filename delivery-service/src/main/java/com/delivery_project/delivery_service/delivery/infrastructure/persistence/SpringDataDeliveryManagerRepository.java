package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryManagerRepository
        extends JpaRepository<DeliveryManager, UUID> {

    boolean existsByUserId(UUID userId);

    Page<DeliveryManager> findAllByDeletedAtIsNull(
            Pageable pageable
    );

    Page<DeliveryManager> findAllByHubIdAndDeletedAtIsNull(
            UUID hubId,
            Pageable pageable
    );

    Optional<DeliveryManager> findByIdAndDeletedAtIsNull(UUID managerId);

    Optional<DeliveryManager> findByUserIdAndDeletedAtIsNull(UUID userId);

    // HUB_DELIVERY 라운드로빈에서 마지막 배정 순번보다 큰 다음 후보 1명(다음 배정 찾기)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryManager>
    findFirstByTypeAndStatusAndActiveTrueAndDeliverySequenceGreaterThanAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType type,
            DeliveryManagerStatus status,
            Integer lastAssignedSequence
    );

    // HUB_DELIVERY에서 뒤쪽 후보가 없을 때 처음 순번부터 다시 찾는 fallback 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryManager>
    findFirstByTypeAndStatusAndActiveTrueAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType type,
            DeliveryManagerStatus status
    );

    // COMPANY_DELIVERY 라운드로빈에서 마지막 배정 순번보다 큰 다음 후보 1명(다음 배정 찾기)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryManager>
    findFirstByHubIdAndTypeAndStatusAndActiveTrueAndDeliverySequenceGreaterThanAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            UUID hubId,
            DeliveryManagerType type,
            DeliveryManagerStatus status,
            Integer lastAssignedSequence
    );

    // COMPANY_DELIVERY에서 마지막 순번 뒤에 후보가 없을 때 해당 허브에서 처음 순번 담당자로 돌아가는 fallback
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryManager>
    findFirstByHubIdAndTypeAndStatusAndActiveTrueAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            UUID hubId,
            DeliveryManagerType type,
            DeliveryManagerStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT dm
        FROM DeliveryManager dm
        WHERE dm.userId = :userId
          AND dm.deletedAt IS NULL
    """)
    Optional<DeliveryManager> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}
