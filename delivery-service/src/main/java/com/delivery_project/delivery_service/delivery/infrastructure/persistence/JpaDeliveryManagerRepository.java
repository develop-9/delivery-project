package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerRepository;
import lombok.RequiredArgsConstructor;
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
    public boolean existsByUserId(UUID userId) {
        return springDataRepository.existsByUserId(userId);
    }

    @Override
    public Optional<Integer> findMaxSequence(
            UUID hubId, DeliveryManagerType type
    ){
        return Optional.empty();
    }

}
