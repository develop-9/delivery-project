package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryQueryRepositoryImpl
        implements DeliveryQueryRepository {

    private final SpringDataDeliveryRepository springDataRepository;

    @Override
    public Optional<Delivery> findByOrderId(
            UUID orderId
    ){
        return springDataRepository
                .findByOrderIdAndDeletedAtIsNull(orderId);
    }
}
