package com.delivery_project.slack_service.slack.application;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class SlackMessageTemplateGenerator {

    private static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(KOREA_ZONE_ID);

    public String generateSystemMessage(
            UUID orderId,
            Instant finalDispatchDeadline
    ) {
        String formattedDeadline =
                DEADLINE_FORMATTER.format(finalDispatchDeadline);

        return """
                [배송 출발 알림]
                
                주문 번호: %s
                최종 발송 시한: %s
                
                해당 시한까지 배송 출발 처리를 완료해 주세요.
                """.formatted(
                orderId,
                formattedDeadline
        );
    }
}