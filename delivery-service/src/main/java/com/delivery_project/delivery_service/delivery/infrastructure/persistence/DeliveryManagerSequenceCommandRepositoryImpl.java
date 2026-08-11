package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManagerSequence;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerSequenceCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryManagerSequenceCommandRepositoryImpl
        implements DeliveryManagerSequenceCommandRepository {

    private final SpringDataDeliveryManagerSequenceRepository
            springDataRepository;

    @Override
    public DeliveryManagerSequence save(
            DeliveryManagerSequence sequence
    ){
        return springDataRepository.save(sequence);
    }

    @Override
    public Optional<DeliveryManagerSequence> findForUpdate(
            DeliveryManagerType managerType,
            UUID hubId
    ){
        return springDataRepository.findForUpdate(managerType, hubId);
    }
}
