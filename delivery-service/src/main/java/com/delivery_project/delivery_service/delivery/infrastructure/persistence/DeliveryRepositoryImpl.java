package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryImpl implements DeliveryCommandRepository {
    private final SpringDataDeliveryRepository springDataDeliveryRepository;

    @Override
    public Delivery save(Delivery delivery) {
        return springDataDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    public Optional<Delivery> findByOrderIdAndDeletedAtIsNull(
            UUID orderId
    ){
        return springDataDeliveryRepository
                .findByOrderIdAndDeleteAtIsNull(orderId);
    }
}
