package com.delivery_project.slack_service.slack.application;

import com.delivery_project.slack_service.slack.application.result.SlackMessageTemplateData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SlackMessageTemplateGeneratorTest {

    private final SlackMessageTemplateGenerator generator = new SlackMessageTemplateGenerator();

    @Test
    void 모든_필드가_채워지면_메시지에_전부_포함된다() {
        // given
        UUID orderId = UUID.randomUUID();

        SlackMessageTemplateData data = new SlackMessageTemplateData(
                orderId,
                "홍길동",
                "U1111111111",
                Instant.parse("2026-08-08T01:00:00Z"),
                "노트북",
                "부재 시 경비실에 맡겨주세요",
                "서울 허브",
                List.of("대전 허브", "대구 허브"),
                "부산광역시 해운대구",
                "김배송",
                "U2222222222",
                Instant.parse("2026-08-08T05:00:00Z")
        );

        // when
        String result = generator.generateSystemMessage(data);

        // then
        assertThat(result)
                .contains(orderId.toString())
                .contains("홍길동 / U1111111111")
                .contains("노트북")
                .contains("부재 시 경비실에 맡겨주세요")
                .contains("서울 허브")
                .contains("대전 허브, 대구 허브")
                .contains("부산광역시 해운대구")
                .contains("김배송 / U2222222222");
    }

    @Test
    void 요청사항과_경유지가_없으면_기본_문구가_들어간다() {
        // given
        SlackMessageTemplateData data = new SlackMessageTemplateData(
                UUID.randomUUID(),
                "홍길동",
                "U1111111111",
                Instant.parse("2026-08-08T01:00:00Z"),
                "노트북",
                null,
                "서울 허브",
                List.of(),
                "부산광역시 해운대구",
                "김배송",
                "U2222222222",
                Instant.parse("2026-08-08T05:00:00Z")
        );

        // when
        String result = generator.generateSystemMessage(data);

        // then
        assertThat(result)
                .contains("요청사항 없음")
                .contains("경유지 없음");
    }
}
