package com.delivery_project.slack_service.ai_history.application.command_service;

import com.delivery_project.slack_service.ai_history.application.result.DeliveryRouteResult;
import com.delivery_project.slack_service.ai_history.application.result.HubBatchResult;
import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiPromptGenerator {

    public String generate(
            OrderSummaryResult orderSummary,
            DeliveryRouteResult deliveryRoute,
            HubBatchResult hubBatch
    ) {
        Map<UUID, HubBatchResult.HubResult> hubMap =
                hubBatch.hubs()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        HubBatchResult.HubResult::hubId,
                                        Function.identity()
                                )
                        );

        String itemInformation =
                orderSummary.items()
                        .stream()
                        .map(item ->
                                String.format(
                                        "- 상품 ID: %s, 수량: %d",
                                        item.productId(),
                                        item.quantity()
                                )
                        )
                        .collect(
                                Collectors.joining("\n")
                        );

        String routeInformation =
                deliveryRoute.routes()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        DeliveryRouteResult.RouteResult::sequence
                                )
                        )
                        .map(route -> {
                            HubBatchResult.HubResult departureHub =
                                    hubMap.get(
                                            route.departureHubId()
                                    );

                            HubBatchResult.HubResult arrivalHub =
                                    hubMap.get(
                                            route.arrivalHubId()
                                    );

                            return String.format(
                                    "- %d구간: %s → %s, 예상 소요 시간 %d분",
                                    route.sequence(),
                                    formatHub(
                                            departureHub,
                                            route.departureHubId()
                                    ),
                                    formatHub(
                                            arrivalHub,
                                            route.arrivalHubId()
                                    ),
                                    route.estimatedDurationMin()
                            );
                        })
                        .collect(
                                Collectors.joining("\n")
                        );

        return """
                다음 주문의 요청사항과 전체 배송 경로의 예상 소요 시간을 고려하여,
                첫 번째 출발 허브에서 상품을 발송해야 하는 최종 시한을 계산해 주세요.

                [주문 정보]
                - 주문 ID: %s
                - 공급 업체 ID: %s
                - 수령 업체 ID: %s
                - 요청사항: %s

                [주문 상품]
                %s

                [배송 경로]
                %s

                요청사항에 배송 완료 희망 시각 또는 납기 시각이 포함되어 있다면
                해당 시각까지 배송이 완료될 수 있도록 전체 배송 경로의 예상 소요 시간을 고려해
                첫 번째 출발 허브의 최종 발송 시각을 계산해 주세요.

                최종 응답은 반드시 ISO-8601 형식의 시각만 반환해 주세요.
                예: 2026-08-03T09:00:00Z
                """.formatted(
                orderSummary.orderId(),
                orderSummary.supplierCompanyId(),
                orderSummary.receiverCompanyId(),
                valueOrDefault(
                        orderSummary.requestDetails(),
                        "요청사항 없음"
                ),
                itemInformation,
                routeInformation
        );
    }

    private String formatHub(
            HubBatchResult.HubResult hub,
            UUID hubId
    ) {
        if (hub == null) {
            return hubId.toString();
        }

        return String.format(
                "%s(%s)",
                hub.name(),
                hub.address()
        );
    }

    private String valueOrDefault(
            String value,
            String defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}