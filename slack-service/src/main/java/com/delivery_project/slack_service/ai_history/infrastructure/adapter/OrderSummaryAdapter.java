package com.delivery_project.slack_service.ai_history.infrastructure.adapter;

import com.delivery_project.slack_service.ai_history.application.port.OrderSummaryPort;
import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;
import com.delivery_project.slack_service.ai_history.infrastructure.client.order.OrderSummaryFeignClient;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderSummaryAdapter
        implements OrderSummaryPort {

    private static final String INCLUDE_OPTIONS =
            "snapshot,requester";

    private final OrderSummaryFeignClient orderSummaryFeignClient;

    @Override
    public OrderSummaryResult getOrderSummary(
            UUID orderId
    ) {
        OrderSummaryFeignClient.OrderSummaryApiResponse response;

        try {
            response = orderSummaryFeignClient.getOrderSummary(
                    orderId,
                    INCLUDE_OPTIONS
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

        return new OrderSummaryResult(
                data.orderId(),
                data.status(),
                data.productId(),
                data.productName(),
                data.quantity(),
                data.supplierCompanyId(),
                data.supplierCompanyName(),
                data.receiverCompanyId(),
                data.receiverCompanyName(),
                data.originHubId(),
                data.destHubId(),
                data.originHubName(),
                data.destHubName(),
                data.requesterUserId(),
                data.requesterName(),
                data.requestDetails(),
                data.dueAt(),
                data.deliveryId(),
                data.createdAt(),
                toLatestSnapshotResult(
                        data.latestSnapshot()
                )
        );
    }

    private OrderSummaryResult.LatestSnapshotResult toLatestSnapshotResult(
            OrderSummaryFeignClient.LatestSnapshotData snapshot
    ) {
        if (snapshot == null) {
            return null;
        }

        return new OrderSummaryResult.LatestSnapshotResult(
                snapshot.snapshotId(),
                snapshot.sequence(),
                snapshot.eventType(),
                snapshot.productName(),
                snapshot.quantity(),
                snapshot.supplierCompanyName(),
                snapshot.receiverCompanyName(),
                snapshot.originHubName(),
                snapshot.destHubName(),
                snapshot.requestDetails(),
                snapshot.orderStatus(),
                snapshot.note(),
                snapshot.createdAt()
        );
    }
}