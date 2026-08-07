package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRouteRepositoryImpl
        implements DeliveryRouteCommandRepository {

    private final SpringDataDeliveryRouteRepository springDataRepository;

    @Override
    public List<DeliveryRoute> saveAll(
            List<DeliveryRoute> deliveryRoutes
    ) {
        return springDataRepository.saveAllAndFlush(deliveryRoutes);
    }

    @Override
    public List<DeliveryRoute>
    findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
            UUID deliveryId,
            DeliveryRouteStatus status
    ) {
        return springDataRepository
                .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                        deliveryId,
                        status
                );
    }
}