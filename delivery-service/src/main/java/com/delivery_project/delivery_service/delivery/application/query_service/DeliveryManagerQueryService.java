package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerDetailResult;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerRepository;

import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerQueryService {
    private final DeliveryManagerRepository deliveryManagerRepository;

    public DeliveryManagerDetailResult getDeliveryManager(UUID managerId) {
        DeliveryManager deliveryManager = deliveryManagerRepository.findById(managerId)
                .orElseThrow(()-> new BusinessException(
                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                ));
        return DeliveryManagerDetailResult.from(deliveryManager);
    }
}
