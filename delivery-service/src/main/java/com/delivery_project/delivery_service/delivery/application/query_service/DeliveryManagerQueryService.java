package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.query.DeliveryManagerGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryManagerGetMyQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryManagerListQuery;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerDetailResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerListResult;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerQueryRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerQueryService {
    private final DeliveryManagerQueryRepository
            deliveryManagerQueryRepository;

    public DeliveryManagerDetailResult getDeliveryManager(
            DeliveryManagerGetQuery query
    ) {
        DeliveryManager deliveryManager = deliveryManagerQueryRepository.findById(query.managerId())
                .orElseThrow(()-> new BusinessException(
                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                ));
        return DeliveryManagerDetailResult.from(deliveryManager);
    }

    public Page<DeliveryManagerListResult> getDeliveryManagers(
            DeliveryManagerListQuery query
    ) {
        validatePage(query.page());

        int size = validateSize(query.size());

        Pageable pageable = PageRequest.of(
                query.page(),
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return deliveryManagerQueryRepository.findAll(pageable)
                .map(DeliveryManagerListResult::from);
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_PAGE_NUMBER
            );
        }
    }

    private int validateSize(int size) {
        if (size != 10 && size != 30 && size != 50) {
            return 10;
        }
        return size;
    }

    public DeliveryManagerDetailResult getMyDeliveryManager(
            DeliveryManagerGetMyQuery query
    ) {
        DeliveryManager deliveryManager = deliveryManagerQueryRepository.findByUserId(query.userId())
                .orElseThrow(()-> new BusinessException(
                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                ));
        return DeliveryManagerDetailResult.from(deliveryManager);
    }

}
