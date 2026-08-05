package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaDeliveryManagerRepository implements DeliveryManagerRepository {

    private final SpringDataDeliveryManagerRepository springDataRepository;

    @Override
    public DeliveryManager save(DeliveryManager deliveryManager) {
        return springDataRepository.save(deliveryManager);
    }

    @Override
    public Optional<DeliveryManager> findById(UUID managerId) {
        return springDataRepository.findById(managerId);
    }

    @Override
    public Page<DeliveryManager> findAll(Pageable pageable) {
        return springDataRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return springDataRepository.existsByUserId(userId);
    }

    @Override
    public Optional<Integer> findMaxSequenceByType(
            DeliveryManagerType type
    ) {
        return springDataRepository.findMaxSequenceByType(type);
    }

    @Override
    public Optional<Integer> findMaxSequenceByHubIdAndType(
            UUID hubId,
            DeliveryManagerType type
    ) {
        return springDataRepository.findMaxSequenceByHubIdAndType(
                hubId,
                type
        );
    }

}
