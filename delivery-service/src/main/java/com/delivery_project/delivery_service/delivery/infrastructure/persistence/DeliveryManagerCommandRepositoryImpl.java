package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryManagerCommandRepositoryImpl
        implements DeliveryManagerCommandRepository {

    private final SpringDataDeliveryManagerRepository springDataRepository;

    @Override
    public DeliveryManager save(DeliveryManager deliveryManager) {
        return springDataRepository.saveAndFlush(deliveryManager);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return springDataRepository.existsByUserId(userId);
    }

    @Override
    public Optional<DeliveryManager> findById(UUID managerId) {
        return springDataRepository
                .findByIdAndDeletedAtIsNull(managerId);
    }

    @Override
    public Optional<DeliveryManager> findByUserId(UUID userId) {
        return springDataRepository
                .findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Optional<DeliveryManager> findNextAvailableHubManager(
            Integer lastAssignedSequence
    ){
        return springDataRepository
                .findFirstByTypeAndStatusAndDeliverySequenceGreaterThanAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                        DeliveryManagerType.HUB_DELIVERY,
                        DeliveryManagerStatus.AVAILABLE,
                        lastAssignedSequence
                );
    }

    @Override
    public Optional<DeliveryManager> findFirstAvailableHubManager(){
        return springDataRepository
                .findFirstByTypeAndStatusAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                        DeliveryManagerType.HUB_DELIVERY,
                        DeliveryManagerStatus.AVAILABLE
                );
    }

    @Override
    public Optional<DeliveryManager> findNextAvailableCompanyManager(
            UUID hubId,
            Integer lastAssignedSequence
    ){
        return springDataRepository
                .findFirstByHubIdAndTypeAndStatusAndDeliverySequenceGreaterThanAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                        hubId,
                        DeliveryManagerType.COMPANY_DELIVERY,
                        DeliveryManagerStatus.AVAILABLE,
                        lastAssignedSequence
                );
    }

    @Override
    public Optional<DeliveryManager> findFirstAvailableCompanyManager(
            UUID hubId
    ) {
        return springDataRepository
                .findFirstByHubIdAndTypeAndStatusAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                        hubId,
                        DeliveryManagerType.COMPANY_DELIVERY,
                        DeliveryManagerStatus.AVAILABLE
                );
    }
}