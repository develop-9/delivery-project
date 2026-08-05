package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository {

    DeliveryManager save(DeliveryManager deliveryManager); // 배송자 등록
    boolean existsByUserId(UUID userId);

    Optional<DeliveryManager> findById(UUID managerId); // 단건조회

    Page<DeliveryManager> findAll(Pageable pageable); // 목록 조회

    Optional<DeliveryManager> findByUserId(UUID userId);

    Optional<Integer> findMaxSequenceByType(
            DeliveryManagerType type
    );

    Optional<Integer> findMaxSequenceByHubIdAndType(
            UUID hubId,
            DeliveryManagerType type
    );
}