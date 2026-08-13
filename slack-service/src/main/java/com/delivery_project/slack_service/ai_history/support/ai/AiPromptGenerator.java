package com.delivery_project.slack_service.ai_history.support.ai;

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
                다음 주문의 납기 시간과 전체 배송 경로의 예상 소요 시간을 고려하여,
                첫 번째 출발 허브에서 상품을 발송해야 하는 최종 시한을 계산해 주세요.

                [주문 정보]
                - 주문 ID: %s
                - 주문 생성 시각: %s
                - 주문자: %s
                - 공급 업체: %s
                - 수령 업체: %s
                - 상품명: %s
                - 수량: %d
                - 요청사항: %s
                - 납기 시각: %s
                - 출발 허브: %s
                - 도착 허브: %s

                [배송 경로]
                %s

                납기 시각까지 배송이 완료될 수 있도록
                전체 배송 경로의 예상 소요 시간을 고려하여
                첫 번째 출발 허브의 최종 발송 시각을 계산해 주세요.

                요청사항에 별도의 배송 완료 희망 시각이나 조건이 있다면
                해당 내용도 함께 고려해 주세요.

                최종 응답은 반드시 ISO-8601 형식의 시각만 반환해 주세요.
                예: 2026-08-03T09:00:00Z
                """.formatted(
                orderSummary.orderId(),
                orderSummary.createdAt(),
                valueOrDefault(
                        orderSummary.requesterName(),
                        "확인되지 않음"
                ),
                companyInformation(
                        orderSummary.supplierCompanyName(),
                        orderSummary.supplierCompanyId()
                ),
                companyInformation(
                        orderSummary.receiverCompanyName(),
                        orderSummary.receiverCompanyId()
                ),
                valueOrDefault(
                        orderSummary.productName(),
                        "확인되지 않음"
                ),
                orderSummary.quantity(),
                valueOrDefault(
                        orderSummary.requestDetails(),
                        "요청사항 없음"
                ),
                orderSummary.dueAt(),
                formatHubFromSummary(
                        orderSummary.originHubName(),
                        orderSummary.originHubId(),
                        hubMap
                ),
                formatHubFromSummary(
                        orderSummary.destHubName(),
                        orderSummary.destHubId(),
                        hubMap
                ),
                routeInformation
        );
    }

    private String formatHub(
            HubBatchResult.HubResult hub,
            UUID hubId
    ) {
        if (hub == null) {
            return hubId == null
                    ? "확인되지 않음"
                    : hubId.toString();
        }

        return String.format(
                "%s(%s)",
                valueOrDefault(
                        hub.name(),
                        "이름 없음"
                ),
                valueOrDefault(
                        hub.address(),
                        "주소 없음"
                )
        );
    }

    private String formatHubFromSummary(
            String hubName,
            UUID hubId,
            Map<UUID, HubBatchResult.HubResult> hubMap
    ) {
        if (hubId != null) {
            HubBatchResult.HubResult hub =
                    hubMap.get(hubId);

            if (hub != null) {
                return formatHub(
                        hub,
                        hubId
                );
            }
        }

        if (
                hubName != null
                        && !hubName.isBlank()
        ) {
            return hubName;
        }

        if (hubId != null) {
            return hubId.toString();
        }

        return "확인되지 않음";
    }

    private String companyInformation(
            String companyName,
            UUID companyId
    ) {
        if (
                companyName != null
                        && !companyName.isBlank()
        ) {
            return companyId == null
                    ? companyName
                    : String.format(
                    "%s(%s)",
                    companyName,
                    companyId
            );
        }

        return companyId == null
                ? "확인되지 않음"
                : companyId.toString();
    }

    private String valueOrDefault(
            String value,
            String defaultValue
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return defaultValue;
        }

        return value;
    }
}