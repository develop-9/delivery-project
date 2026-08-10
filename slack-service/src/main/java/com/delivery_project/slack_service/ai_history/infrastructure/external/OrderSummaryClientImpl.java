package com.delivery_project.slack_service.ai_history.infrastructure.external;

import com.delivery_project.slack_service.ai_history.application.port.OrderSummaryClient;
import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderSummaryClientImpl
        implements OrderSummaryClient {

    private final OrderSummaryFeignClient orderSummaryFeignClient;

    @Override
    public OrderSummaryResult getOrderSummary(
            UUID orderId
    ) {
        OrderSummaryFeignClient.OrderSummaryApiResponse response;

        try {
            response =
                    orderSummaryFeignClient.getOrderSummary(
                            orderId
                    );

        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    ErrorCode.ORDER_NOT_FOUND
            );

        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }

        if (
                response == null
                        || !response.success()
                        || response.data() == null
        ) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }

        OrderSummaryFeignClient.OrderSummaryData data =
                response.data();

        List<OrderSummaryResult.ItemResult> items =
                data.items() == null
                        ? List.of()
                        : data.items()
                        .stream()
                        .map(item ->
                             new OrderSummaryResult.ItemResult(
                                     item.productId(),
                                     item.quantity()
                             )
                        )
                        .toList();

        return new OrderSummaryResult(
                data.orderId(),
                data.supplierCompanyId(),
                data.receiverCompanyId(),
                data.requestDetails(),
                items
        );
    }
}