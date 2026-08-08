package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.query.DeliveryGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryListQuery;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryDetailResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryListResult;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryQueryRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryQueryService {

    private final DeliveryQueryRepository deliveryQueryRepository;

    public DeliveryDetailResult getDelivery(
            DeliveryGetQuery query
    ){
        Delivery delivery =
                deliveryQueryRepository
                        .findById(query.deliveryId())
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_NOT_FOUND
                                )
                        );
        return DeliveryDetailResult.from(delivery);
    }

    public Page<DeliveryListResult> getDeliveries(
            DeliveryListQuery query
    ){
        validatePage(query.page());

        int size = validateSize(query.size());

        Pageable pageable =
                PageRequest.of(
                        query.page(),
                        size
                );

        return deliveryQueryRepository
                .search(query,pageable)
                .map(DeliveryListResult::from);
    }

    private void validatePage(
            int page
    ){
        if(page < 0){
            throw new BusinessException(
                    ErrorCode.INVALID_PAGE_NUMBER
            );
        }
    }

    private int validateSize(
            int size
    ){
        if(size != 10
                && size != 30
                && size != 50){
            return 10;
        }

        return size;
    }
}
