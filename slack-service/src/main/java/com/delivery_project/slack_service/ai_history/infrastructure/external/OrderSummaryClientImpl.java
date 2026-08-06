package com.delivery_project.slack_service.ai_history.infrastructure.external;

import com.delivery_project.slack_service.ai_history.application.port.OrderSummaryClient;
import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderSummaryClientImpl
        implements OrderSummaryClient {

    private static final String INCLUDE_OPTIONS =
            "snapshot,requester";

    private final OrderSummaryFeignClient orderSummaryFeignClient;

    @Override
    public OrderSummaryResult getOrderSummary(
            UUID orderId
    ) {
        OrderSummaryFeignClient.OrderSummaryApiResponse response =
                orderSummaryFeignClient.getOrderSummary(
                        orderId,
                        INCLUDE_OPTIONS
                );

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