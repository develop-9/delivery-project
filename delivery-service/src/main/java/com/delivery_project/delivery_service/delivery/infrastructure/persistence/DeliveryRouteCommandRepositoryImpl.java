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

    @Override
    public DeliveryRoute save(DeliveryRoute route) {
        return springDataRepository.save(route);
    }

    @Override
    public List<DeliveryRoute> saveAll(
            List<DeliveryRoute> deliveryRoutes
    ) {
        return springDataRepository.saveAllAndFlush(deliveryRoutes);
    }

    @Override
    public Optional<DeliveryRoute> findById(UUID routeId) {
        return springDataRepository
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
        return springDataRepository
                .findFirstByDeliveryIdAndDeletedAtIsNullOrderBySequenceDesc(
                        deliveryId
                );
    }

    @Override
    public List<DeliveryRoute> findAllByDeliveryIdAndDeletedAtIsNull(
            UUID deliveryId
    ) {
        return springDataRepository
                .findAllByDeliveryIdAndDeletedAtIsNull(deliveryId);
    }

    @Override
    public Optional<DeliveryRoute> findByIdForUpdate(
            UUID routeId
    ) {
        return springDataRepository
                .findByIdForUpdate(routeId);
    }

    @Override
    public boolean existsInTransitByDeliveryManagerId(
            UUID managerId
    ) {
        return springDataRepository
                .existsByDeliveryManagerIdAndStatusAndDeletedAtIsNull(
                        managerId,
                        DeliveryRouteStatus.IN_TRANSIT
                );
    }
}