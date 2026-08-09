package com.delivery_project.slack_service.ai_history.application.command_service;

import com.delivery_project.slack_service.ai_history.application.command.AiHistoryCreateCommand;
import com.delivery_project.slack_service.ai_history.application.port.AiGenerationClient;
import com.delivery_project.slack_service.ai_history.application.port.DeliveryRouteClient;
import com.delivery_project.slack_service.ai_history.application.port.OrderSummaryClient;
import com.delivery_project.slack_service.ai_history.application.result.AiGenerationResult;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryCreateResult;
import com.delivery_project.slack_service.ai_history.application.result.DeliveryRouteResult;
import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI History CommandService 테스트")
class AiHistoryCommandServiceTest {

    @Mock
    private OrderSummaryClient orderSummaryClient;

    @Mock
    private DeliveryRouteClient deliveryRouteClient;

    @Mock
    private AiGenerationClient aiGenerationClient;

    @Mock
    private AiPromptGenerator aiPromptGenerator;

    @Mock
    private AiHistoryPersistenceService aiHistoryPersistenceService;

    @InjectMocks
    private AiHistoryCommandService aiHistoryCommandService;

    @Test
    @DisplayName("AI 요청 생성이 성공하면 SUCCESS 상태의 이력을 반환한다")
    void create_success() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID aiHistoryId = UUID.randomUUID();
        String prompt = "test prompt";
        Instant deadline = Instant.parse("2026-08-09T12:00:00Z");

        AiHistoryCreateCommand command =
                new AiHistoryCreateCommand(orderId);

        OrderSummaryResult orderSummary = mock(OrderSummaryResult.class);
        DeliveryRouteResult deliveryRoute = mock(DeliveryRouteResult.class);

        DeliveryRouteResult.RouteResult route =
                new DeliveryRouteResult.RouteResult(
                        1,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        60
                );

        when(deliveryRoute.routes())
                .thenReturn(List.of(route));

        when(orderSummaryClient.getOrderSummary(orderId))
                .thenReturn(orderSummary);

        when(deliveryRouteClient.getRoutesByOrderId(orderId))
                .thenReturn(deliveryRoute);

        when(aiPromptGenerator.generate(orderSummary, deliveryRoute))
                .thenReturn(prompt);

        when(aiHistoryPersistenceService.createPending(
                eq(orderId),
                eq(prompt),
                any(Instant.class)
        )).thenReturn(aiHistoryId);

        when(aiGenerationClient.generateFinalDispatchDeadline(prompt))
                .thenReturn(
                        new AiGenerationResult(
                                "gemini-2.5-flash",
                                deadline
                        )
                );

        AiHistory completedHistory = mock(AiHistory.class);

        when(completedHistory.getStatus())
                .thenReturn(AiHistoryStatus.SUCCESS);

        when(aiHistoryPersistenceService.complete(
                eq(aiHistoryId),
                eq("gemini-2.5-flash"),
                eq(deadline),
                any(Instant.class)
        )).thenReturn(completedHistory);

        // when
        AiHistoryCreateResult result =
                aiHistoryCommandService.create(command);

        // then
        assertThat(result).isNotNull();

        verify(orderSummaryClient).getOrderSummary(orderId);
        verify(deliveryRouteClient).getRoutesByOrderId(orderId);
        verify(aiPromptGenerator).generate(orderSummary, deliveryRoute);
        verify(aiHistoryPersistenceService).createPending(
                eq(orderId),
                eq(prompt),
                any(Instant.class)
        );
        verify(aiGenerationClient).generateFinalDispatchDeadline(prompt);
        verify(aiHistoryPersistenceService).complete(
                eq(aiHistoryId),
                eq("gemini-2.5-flash"),
                eq(deadline),
                any(Instant.class)
        );
        verify(aiHistoryPersistenceService, never())
                .fail(any(), any(), any());
    }

    @Test
    @DisplayName("Gemini 요청이 실패하면 AI 이력을 FAILED 상태로 저장한다")
    void create_geminiFailure() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID aiHistoryId = UUID.randomUUID();
        String prompt = "test prompt";

        AiHistoryCreateCommand command =
                new AiHistoryCreateCommand(orderId);

        OrderSummaryResult orderSummary = mock(OrderSummaryResult.class);
        DeliveryRouteResult deliveryRoute = mock(DeliveryRouteResult.class);

        DeliveryRouteResult.RouteResult route =
                new DeliveryRouteResult.RouteResult(
                        1,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        60
                );

        when(deliveryRoute.routes())
                .thenReturn(List.of(route));

        when(orderSummaryClient.getOrderSummary(orderId))
                .thenReturn(orderSummary);

        when(deliveryRouteClient.getRoutesByOrderId(orderId))
                .thenReturn(deliveryRoute);

        when(aiPromptGenerator.generate(orderSummary, deliveryRoute))
                .thenReturn(prompt);

        when(aiHistoryPersistenceService.createPending(
                eq(orderId),
                eq(prompt),
                any(Instant.class)
        )).thenReturn(aiHistoryId);

        when(aiGenerationClient.generateFinalDispatchDeadline(prompt))
                .thenThrow(
                        new BusinessException(
                                ErrorCode.AI_REQUEST_FAILED
                        )
                );

        // when & then
        assertThatThrownBy(
                () -> aiHistoryCommandService.create(command)
        )
                .isInstanceOf(BusinessException.class);

        verify(aiHistoryPersistenceService).fail(
                eq(aiHistoryId),
                isNull(),
                any(Instant.class)
        );

        verify(aiHistoryPersistenceService, never())
                .complete(any(), any(), any(), any());
    }

    @Test
    @DisplayName("배송 경로가 없으면 DELIVERY_ROUTE_NOT_FOUND 예외가 발생한다")
    void create_deliveryRouteNotFound() {
        // given
        UUID orderId = UUID.randomUUID();

        AiHistoryCreateCommand command =
                new AiHistoryCreateCommand(orderId);

        OrderSummaryResult orderSummary = mock(OrderSummaryResult.class);
        DeliveryRouteResult deliveryRoute = mock(DeliveryRouteResult.class);

        when(orderSummaryClient.getOrderSummary(orderId))
                .thenReturn(orderSummary);

        when(deliveryRouteClient.getRoutesByOrderId(orderId))
                .thenReturn(deliveryRoute);

        when(deliveryRoute.routes())
                .thenReturn(List.of());

        // when & then
        assertThatThrownBy(
                () -> aiHistoryCommandService.create(command)
        )
                .isInstanceOf(BusinessException.class);

        verify(aiPromptGenerator, never())
                .generate(any(), any());

        verify(aiHistoryPersistenceService, never())
                .createPending(any(), any(), any());

        verify(aiGenerationClient, never())
                .generateFinalDispatchDeadline(any());
    }
}