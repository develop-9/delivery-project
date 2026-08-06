package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryManagerQueryRepositoryImpl
        implements DeliveryManagerQueryRepository {

    private final SpringDataDeliveryManagerRepository springDataRepository;

    @Override
    public Optional<DeliveryManager> findById(UUID managerId) {
        return springDataRepository
                .findByIdAndDeletedAtIsNull(managerId);
    }

    @Override
    public Page<DeliveryManager> findAll(Pageable pageable) {
        return springDataRepository
                .findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public Optional<DeliveryManager> findByUserId(UUID userId) {
        return springDataRepository
                .findByUserIdAndDeletedAtIsNull(userId);
    }
}