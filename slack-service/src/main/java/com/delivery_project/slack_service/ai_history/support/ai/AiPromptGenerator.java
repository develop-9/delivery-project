package com.delivery_project.slack_service.ai_history.support.ai;

import com.delivery_project.slack_service.ai_history.application.result.DeliveryRouteResult;
import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class AiPromptGenerator {

    public String generate(
            OrderSummaryResult orderSummary,
            DeliveryRouteResult deliveryRoute
    ) {
        String routeInformation = deliveryRoute.routes()
                .stream()
                .sorted(
                        Comparator.comparing(
                                DeliveryRouteResult.RouteResult::sequence
                        )
                )
                .map(route ->
                        String.format(
                                "- %d구간: 출발 허브 %s → 도착 허브 %s, 예상 소요 시간 %d분",
                                route.sequence(),
                                route.departureHubId(),
                                route.arrivalHubId(),
                                route.estimatedDurationMin()
                        )
                )
                .collect(Collectors.joining("\n"));

        return """
                다음 주문의 납기 시간과 전체 배송 경로의 예상 소요 시간을 고려하여,
                첫 번째 출발 허브에서 상품을 발송해야 하는 최종 시한을 계산해 주세요.

                [주문 정보]
                - 주문 ID: %s
                - 주문 생성 시각: %s
                - 주문자: %s
                - 상품명: %s
                - 수량: %d
                - 요청사항: %s
                - 납기 시각: %s
                - 출발 허브: %s
                - 도착 허브: %s

                [배송 경로]
                %s

                최종 응답은 반드시 ISO-8601 형식의 시각만 반환해 주세요.
                예: 2026-08-03T09:00:00Z
                """.formatted(
                orderSummary.orderId(),
                orderSummary.createdAt(),
                valueOrDefault(
                        orderSummary.requesterName(),
                        "확인되지 않음"
                ),
                orderSummary.productName(),
                orderSummary.quantity(),
                valueOrDefault(
                        orderSummary.requestDetails(),
                        "요청사항 없음"
                ),
                orderSummary.dueAt(),
                valueOrDefault(
                        orderSummary.originHubName(),
                        orderSummary.originHubId().toString()
                ),
                valueOrDefault(
                        orderSummary.destHubName(),
                        orderSummary.destHubId().toString()
                ),
                routeInformation
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