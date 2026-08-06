package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerGetCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerGetMyCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerListCommand;
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
            DeliveryManagerGetCommand command
    ) {
        DeliveryManager deliveryManager = deliveryManagerQueryRepository.findById(command.managerId())
                .orElseThrow(()-> new BusinessException(
                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                ));
        return DeliveryManagerDetailResult.from(deliveryManager);
    }

    public Page<DeliveryManagerListResult> getDeliveryManagers(
            DeliveryManagerListCommand command
    ) {
        validatePage(command.page());

        int size = validateSize(command.size());

        Pageable pageable = PageRequest.of(
                command.page(),
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
            DeliveryManagerGetMyCommand command
    ) {
        DeliveryManager deliveryManager = deliveryManagerQueryRepository.findByUserId(command.userId())
                .orElseThrow(()-> new BusinessException(
                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                ));
        return DeliveryManagerDetailResult.from(deliveryManager);
    }

}
