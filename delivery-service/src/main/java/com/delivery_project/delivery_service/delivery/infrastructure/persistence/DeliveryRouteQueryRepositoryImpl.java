package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRouteQueryRepositoryImpl
        implements DeliveryRouteQueryRepository {

    private final SpringDataDeliveryRouteRepository springDataRepository;

    @Override
    public List<DeliveryRoute> findAllByDeliveryIdOrderBySequenceAsc(
            UUID deliveryId
    ){
        return springDataRepository
                .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(
                        deliveryId
                );
    }

    @Override
    public Optional<DeliveryRoute> findById(
            UUID routeId
    ) {
        return springDataRepository
                .findByIdAndDeletedAtIsNull(routeId);
    }

}
