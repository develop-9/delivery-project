package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.query.DeliveryRoutesByOrderQuery;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRoutesByOrderResult;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryQueryRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteQueryRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryRouteQueryService {

    private final DeliveryQueryRepository deliveryQueryRepository;
    private final DeliveryRouteQueryRepository deliveryRouteQueryRepository;

    public DeliveryRoutesByOrderResult getRoutesByOrder(
            DeliveryRoutesByOrderQuery query
    ){
        Delivery delivery =
                deliveryQueryRepository
                        .findByOrderId(query.orderId())
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_NOT_FOUND
                                )
                        );
        List<DeliveryRoute> routes =
                deliveryRouteQueryRepository
                        .findAllByDeliveryIdOrderBySequenceAsc(
                                delivery.getId()
                        );
        if(routes.isEmpty()){
            throw new BusinessException(
                    ErrorCode.DELIVERY_ROUTE_NOT_FOUND
            );
        }

        return DeliveryRoutesByOrderResult.of(
                delivery,
                routes
        );
    }
}
