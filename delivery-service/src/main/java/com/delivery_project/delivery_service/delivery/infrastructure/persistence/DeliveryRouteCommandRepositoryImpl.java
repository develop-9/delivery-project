package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRouteCommandRepositoryImpl
        implements DeliveryRouteCommandRepository {

    private final SpringDataDeliveryRouteRepository springDataRepository;
    private final SpringDataDeliveryRouteRepository springDataDeliveryRouteRepository;

    @Override
    public DeliveryRoute save(DeliveryRoute route) {
        return springDataDeliveryRouteRepository.save(route);
    }

    @Override
    public List<DeliveryRoute> saveAll(
            List<DeliveryRoute> deliveryRoutes
    ) {
        return springDataRepository.saveAllAndFlush(deliveryRoutes);
    }

    @Override
    public Optional<DeliveryRoute> findById(UUID routeId) {
        return springDataDeliveryRouteRepository
                .findByIdAndDeletedAtIsNull(routeId);
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

    @Override
    public Optional<DeliveryRoute> findByDeliveryIdAndSequenceAndDeletedAtIsNull(
            UUID deliveryId,
            Integer sequence
    ) {
        return springDataRepository
                .findByDeliveryIdAndSequenceAndDeletedAtIsNull(
                        deliveryId,
                        sequence
                );
    }

    @Override
    public Optional<DeliveryRoute> findLastByDeliveryId(UUID deliveryId){
        return springDataDeliveryRouteRepository
                .findFirstByDeliveryIdAndDeletedAtIsNullOrderBySequenceDesc(
                        deliveryId
                );
    }
}