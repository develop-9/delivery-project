package com.delivery_project.slack_service.ai_history.infrastructure.client.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Delivery Route Feign 응답 계약 테스트")
class DeliveryRouteFeignClientContractTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    @DisplayName("Delivery Service의 SuccessResponse 구조를 역직렬화한다")
    void deserializeDeliveryRouteResponse() throws Exception {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();

        String responseJson = """
                {
                  "success": true,
                  "data": {
                    "deliveryId": "%s",
                    "orderId": "%s",
                    "routes": [
                      {
                        "sequence": 1,
                        "departureHubId": "%s",
                        "arrivalHubId": "%s",
                        "estimatedDurationMin": 60
                      }
                    ]
                  }
                }
                """.formatted(
                deliveryId,
                orderId,
                departureHubId,
                arrivalHubId
        );

        // when
        DeliveryRouteFeignClient.DeliveryRouteApiResponse response =
                objectMapper.readValue(
                        responseJson,
                        DeliveryRouteFeignClient
                                .DeliveryRouteApiResponse.class
                );

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNotNull();
        assertThat(response.data().deliveryId())
                .isEqualTo(deliveryId);
        assertThat(response.data().orderId())
                .isEqualTo(orderId);
        assertThat(response.data().routes()).hasSize(1);
        assertThat(response.data().routes().getFirst().sequence())
                .isEqualTo(1);
        assertThat(
                response.data()
                        .routes()
                        .getFirst()
                        .departureHubId()
        ).isEqualTo(departureHubId);
        assertThat(
                response.data()
                        .routes()
                        .getFirst()
                        .arrivalHubId()
        ).isEqualTo(arrivalHubId);
        assertThat(
                response.data()
                        .routes()
                        .getFirst()
                        .estimatedDurationMin()
        ).isEqualTo(60);

        JsonNode expectedJson =
                objectMapper.readTree(responseJson);

        JsonNode serializedResponse =
                objectMapper.valueToTree(response);

        assertThat(serializedResponse)
                .isEqualTo(expectedJson);
    }
}