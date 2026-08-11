package com.delivery_project.delivery_service.delivery.infrastructure.adapter;

import com.delivery_project.delivery_service.delivery.application.port.OrderPort;
import com.delivery_project.delivery_service.delivery.application.result.OrderCompanyInfo;
import com.delivery_project.delivery_service.delivery.infrastructure.client.OrderInternalClient;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.OrderInfoResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.RelatedOrderIdsResponse;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderFeignAdapter implements OrderPort {

    private final OrderInternalClient orderInternalClient;

    @Override
    public OrderCompanyInfo getOrderCompanyInfo(
            UUID orderId
    ){
        try {
            InternalApiResponse<OrderInfoResponse> response =
                    orderInternalClient.getOrder(orderId);

            OrderInfoResponse order = response.data();

            return new OrderCompanyInfo(
                    order.orderId(),
                    order.supplierCompanyId(),
                    order.receiverCompanyId()
            );
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    ErrorCode.ORDER_NOT_FOUND
            );

        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.ORDER_SERVICE_UNAVAILABLE
            );
        }
    }

    @Override
    public List<UUID> getRelatedOrderIds(
            UUID companyId
    ){
        try {
            InternalApiResponse<RelatedOrderIdsResponse> response =
                    orderInternalClient.getRelatedOrderIds(
                            companyId
                    );

            return response.data().orderIds();
        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.ORDER_SERVICE_UNAVAILABLE
            );
        }

    }
}
