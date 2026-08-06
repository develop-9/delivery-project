package com.delivery_project.slack_service.slack.application;

import com.delivery_project.slack_service.slack.application.result.SlackMessageTemplateData;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SlackMessageTemplateGenerator {

    private static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(KOREA_ZONE_ID);

    public String generateSystemMessage(
            SlackMessageTemplateData data
    ) {
        String formattedOrderedAt =
                DATE_TIME_FORMATTER.format(data.orderedAt());

        String formattedDeadline =
                DATE_TIME_FORMATTER.format(
                        data.finalDispatchDeadline()
                );

        String formattedRoute =
                formatRouteHubNames(data.routeHubNames());

        String requestMessage =
                normalizeText(
                        data.requestMessage(),
                        "요청사항 없음"
                );

        String deliveryManager =
                formatUserInformation(
                        data.deliveryManagerName(),
                        data.deliveryManagerSlackId()
                );

        String orderer =
                formatUserInformation(
                        data.ordererName(),
                        data.ordererSlackId()
                );

        return """
                💡 전달 메시지
                
                주문 번호 : %s
                주문자 정보 : %s
                주문시간 : %s
                상품 정보 : %s
                요청 사항 : %s
                발송지 : %s
                경유지 : %s
                도착지 : %s
                배송담당자 : %s
                
                위 내용을 기반으로 도출된 최종 발송 시한은 %s입니다.
                """.formatted(
                data.orderId(),
                orderer,
                formattedOrderedAt,
                data.productName(),
                requestMessage,
                data.originHubName(),
                formattedRoute,
                data.destinationAddress(),
                deliveryManager,
                formattedDeadline
        );
    }

    private String formatRouteHubNames(
            List<String> routeHubNames
    ) {
        if (routeHubNames == null || routeHubNames.isEmpty()) {
            return "경유지 없음";
        }

        return String.join(", ", routeHubNames);
    }

    private String formatUserInformation(
            String name,
            String slackId
    ) {
        String normalizedName =
                normalizeText(name, "이름 없음");

        String normalizedSlackId =
                normalizeText(slackId, "Slack ID 없음");

        return "%s / %s".formatted(
                normalizedName,
                normalizedSlackId
        );
    }

    private String normalizeText(
            String value,
            String defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}