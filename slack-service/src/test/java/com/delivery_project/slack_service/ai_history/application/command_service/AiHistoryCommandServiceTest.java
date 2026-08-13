package com.delivery_project.slack_service.ai_history.application.command_service;

import com.delivery_project.slack_service.ai_history.application.command.AiHistoryCreateCommand;
import com.delivery_project.slack_service.ai_history.application.persistence_service.AiHistoryPersistenceService;
import com.delivery_project.slack_service.ai_history.application.port.AiGeneratePort;
import com.delivery_project.slack_service.ai_history.application.port.DeliveryRoutePort;
import com.delivery_project.slack_service.ai_history.application.port.HubPort;
import com.delivery_project.slack_service.ai_history.application.port.HubManagerPort;
import com.delivery_project.slack_service.ai_history.application.port.OrderSummaryPort;
import com.delivery_project.slack_service.ai_history.application.result.AiGenerationResult;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryCreateResult;
import com.delivery_project.slack_service.ai_history.application.result.DeliveryRouteResult;
import com.delivery_project.slack_service.ai_history.application.result.HubBatchResult;
import com.delivery_project.slack_service.ai_history.application.result.HubManagerResult;
import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;
import com.delivery_project.slack_service.ai_history.support.ai.AiPromptGenerator;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.slack.application.command.SlackInternalMessageCreateCommand;
import com.delivery_project.slack_service.slack.application.command_service.SlackInternalMessageCommandService;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI History CommandService 테스트")
class AiHistoryCommandServiceTest {

    @Mock
    private OrderSummaryPort orderSummaryPort;

    @Mock
    private DeliveryRoutePort deliveryRoutePort;

    @Mock
    private HubPort hubPort;

    @Mock
    private HubManagerPort hubManagerPort;

    @Mock
    private AiGeneratePort aiGeneratePort;

    @Mock
    private AiPromptGenerator aiPromptGenerator;

    @Mock
    private AiHistoryPersistenceService aiHistoryPersistenceService;

    @Mock
    private SlackInternalMessageCommandService slackInternalMessageCommandService;

    @InjectMocks
    private AiHistoryCommandService aiHistoryCommandService;

    @Test
    @DisplayName("AI 요청 생성이 성공하면 SUCCESS 상태의 이력을 반환하고 Slack 발송 작업을 등록한다")
    void create_success() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID aiHistoryId = UUID.randomUUID();

        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        UUID hubManagerUserId = UUID.randomUUID();

        String prompt = "test prompt";

        Instant deadline =
                Instant.parse("2026-08-09T12:00:00Z");

        AiHistoryCreateCommand command =
                new AiHistoryCreateCommand(orderId);

        OrderSummaryResult orderSummary =
                org.mockito.Mockito.mock(OrderSummaryResult.class);

        DeliveryRouteResult deliveryRoute =
                org.mockito.Mockito.mock(DeliveryRouteResult.class);

        DeliveryRouteResult.RouteResult route =
                new DeliveryRouteResult.RouteResult(
                        1,
                        departureHubId,
                        arrivalHubId,
                        60
                );

        HubBatchResult hubBatch =
                new HubBatchResult(
                        List.of(
                                new HubBatchResult.HubResult(
                                        departureHubId,
                                        "출발 허브",
                                        "출발 허브 주소"
                                ),
                                new HubBatchResult.HubResult(
                                        arrivalHubId,
                                        "도착 허브",
                                        "도착 허브 주소"
                                )
                        ),
                        List.of()
                );

        HubManagerResult hubManager =
                new HubManagerResult(
                        hubManagerUserId,
                        "허브 관리자",
                        "HUB_MANAGER",
                        departureHubId
                );

        when(deliveryRoute.routes())
                .thenReturn(List.of(route));

        when(orderSummaryPort.getOrderSummary(orderId))
                .thenReturn(orderSummary);

        when(deliveryRoutePort.getRoutesByOrderId(orderId))
                .thenReturn(deliveryRoute);

        when(hubPort.getHubs(anyList()))
                .thenReturn(hubBatch);

        when(hubManagerPort.getHubManager(departureHubId))
                .thenReturn(hubManager);

        when(
                aiPromptGenerator.generate(
                        orderSummary,
                        deliveryRoute,
                        hubBatch
                )
        ).thenReturn(prompt);

        when(
                aiHistoryPersistenceService.createPending(
                        eq(orderId),
                        eq(prompt),
                        any(Instant.class)
                )
        ).thenReturn(aiHistoryId);

        when(
                aiGeneratePort.generateFinalDispatchDeadline(prompt)
        ).thenReturn(
                new AiGenerationResult(
                        "gemini-3.6-flash",
                        deadline
                )
        );

        AiHistory completedHistory =
                org.mockito.Mockito.mock(AiHistory.class);

        when(completedHistory.getStatus())
                .thenReturn(AiHistoryStatus.SUCCESS);

        when(
                aiHistoryPersistenceService.complete(
                        eq(aiHistoryId),
                        eq("gemini-3.6-flash"),
                        eq(deadline),
                        any(Instant.class)
                )
        ).thenReturn(completedHistory);

        // when
        AiHistoryCreateResult result =
                aiHistoryCommandService.create(command);

        // then
        assertThat(result).isNotNull();

        verify(orderSummaryPort)
                .getOrderSummary(orderId);

        verify(deliveryRoutePort)
                .getRoutesByOrderId(orderId);

        verify(hubPort)
                .getHubs(
                        argThat(hubIds ->
                                hubIds.size() == 2
                                        && hubIds.contains(departureHubId)
                                        && hubIds.contains(arrivalHubId)
                        )
                );

        verify(hubManagerPort)
                .getHubManager(departureHubId);

        verify(aiPromptGenerator)
                .generate(
                        orderSummary,
                        deliveryRoute,
                        hubBatch
                );

        verify(aiHistoryPersistenceService)
                .createPending(
                        eq(orderId),
                        eq(prompt),
                        any(Instant.class)
                );

        verify(aiGeneratePort)
                .generateFinalDispatchDeadline(prompt);

        verify(aiHistoryPersistenceService)
                .complete(
                        eq(aiHistoryId),
                        eq("gemini-3.6-flash"),
                        eq(deadline),
                        any(Instant.class)
                );

        verify(slackInternalMessageCommandService)
                .createAndPublish(
                        argThat(slackCommand ->
                                slackCommand.receiverUserId()
                                        .equals(hubManagerUserId)
                                        && slackCommand.orderId()
                                        .equals(orderId)
                                        && slackCommand.finalDispatchDeadline()
                                        .equals(deadline)
                        )
                );

        verify(aiHistoryPersistenceService, never())
                .fail(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("AI 생성 성공 후 Slack 발행이 실패해도 AI History 결과는 정상 반환한다")
    void create_slackPublishFailure() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID aiHistoryId = UUID.randomUUID();

        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        UUID hubManagerUserId = UUID.randomUUID();

        String prompt = "test prompt";

        Instant deadline =
                Instant.parse("2026-08-09T12:00:00Z");

        AiHistoryCreateCommand command =
                new AiHistoryCreateCommand(orderId);

        OrderSummaryResult orderSummary =
                org.mockito.Mockito.mock(
                        OrderSummaryResult.class
                );

        DeliveryRouteResult deliveryRoute =
                org.mockito.Mockito.mock(
                        DeliveryRouteResult.class
                );

        DeliveryRouteResult.RouteResult route =
                new DeliveryRouteResult.RouteResult(
                        1,
                        departureHubId,
                        arrivalHubId,
                        60
                );

        HubBatchResult hubBatch =
                new HubBatchResult(
                        List.of(
                                new HubBatchResult.HubResult(
                                        departureHubId,
                                        "출발 허브",
                                        "출발 허브 주소"
                                ),
                                new HubBatchResult.HubResult(
                                        arrivalHubId,
                                        "도착 허브",
                                        "도착 허브 주소"
                                )
                        ),
                        List.of()
                );

        HubManagerResult hubManager =
                new HubManagerResult(
                        hubManagerUserId,
                        "허브 관리자",
                        "HUB_MANAGER",
                        departureHubId
                );

        when(deliveryRoute.routes())
                .thenReturn(List.of(route));

        when(orderSummaryPort.getOrderSummary(orderId))
                .thenReturn(orderSummary);

        when(deliveryRoutePort.getRoutesByOrderId(orderId))
                .thenReturn(deliveryRoute);

        when(hubPort.getHubs(anyList()))
                .thenReturn(hubBatch);

        when(hubManagerPort.getHubManager(departureHubId))
                .thenReturn(hubManager);

        when(
                aiPromptGenerator.generate(
                        orderSummary,
                        deliveryRoute,
                        hubBatch
                )
        ).thenReturn(prompt);

        when(
                aiHistoryPersistenceService.createPending(
                        eq(orderId),
                        eq(prompt),
                        any(Instant.class)
                )
        ).thenReturn(aiHistoryId);

        when(
                aiGeneratePort.generateFinalDispatchDeadline(prompt)
        ).thenReturn(
                new AiGenerationResult(
                        "gemini-3.6-flash",
                        deadline
                )
        );

        AiHistory completedHistory =
                org.mockito.Mockito.mock(
                        AiHistory.class
                );

        when(completedHistory.getStatus())
                .thenReturn(AiHistoryStatus.SUCCESS);

        when(
                aiHistoryPersistenceService.complete(
                        eq(aiHistoryId),
                        eq("gemini-3.6-flash"),
                        eq(deadline),
                        any(Instant.class)
                )
        ).thenReturn(completedHistory);

        org.mockito.Mockito.doThrow(
                        new BusinessException(
                                ErrorCode.USER_SLACK_ID_NOT_FOUND
                        )
                )
                .when(slackInternalMessageCommandService)
                .createAndPublish(
                        any(
                                SlackInternalMessageCreateCommand.class
                        )
                );

        // when
        AiHistoryCreateResult result =
                aiHistoryCommandService.create(command);

        // then
        assertThat(result)
                .isNotNull();

        verify(aiHistoryPersistenceService)
                .complete(
                        eq(aiHistoryId),
                        eq("gemini-3.6-flash"),
                        eq(deadline),
                        any(Instant.class)
                );

        verify(slackInternalMessageCommandService)
                .createAndPublish(
                        any(
                                SlackInternalMessageCreateCommand.class
                        )
                );

        verify(aiHistoryPersistenceService, never())
                .fail(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("Gemini 요청이 실패하면 AI 이력을 FAILED 상태로 저장하고 Slack 작업은 등록하지 않는다")
    void create_geminiFailure() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID aiHistoryId = UUID.randomUUID();

        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        UUID hubManagerUserId = UUID.randomUUID();

        String prompt = "test prompt";

        AiHistoryCreateCommand command =
                new AiHistoryCreateCommand(orderId);

        OrderSummaryResult orderSummary =
                org.mockito.Mockito.mock(OrderSummaryResult.class);

        DeliveryRouteResult deliveryRoute =
                org.mockito.Mockito.mock(DeliveryRouteResult.class);

        DeliveryRouteResult.RouteResult route =
                new DeliveryRouteResult.RouteResult(
                        1,
                        departureHubId,
                        arrivalHubId,
                        60
                );

        HubBatchResult hubBatch =
                new HubBatchResult(
                        List.of(
                                new HubBatchResult.HubResult(
                                        departureHubId,
                                        "출발 허브",
                                        "출발 허브 주소"
                                ),
                                new HubBatchResult.HubResult(
                                        arrivalHubId,
                                        "도착 허브",
                                        "도착 허브 주소"
                                )
                        ),
                        List.of()
                );

        HubManagerResult hubManager =
                new HubManagerResult(
                        hubManagerUserId,
                        "허브 관리자",
                        "HUB_MANAGER",
                        departureHubId
                );

        when(deliveryRoute.routes())
                .thenReturn(List.of(route));

        when(orderSummaryPort.getOrderSummary(orderId))
                .thenReturn(orderSummary);

        when(deliveryRoutePort.getRoutesByOrderId(orderId))
                .thenReturn(deliveryRoute);

        when(hubPort.getHubs(anyList()))
                .thenReturn(hubBatch);

        when(hubManagerPort.getHubManager(departureHubId))
                .thenReturn(hubManager);

        when(
                aiPromptGenerator.generate(
                        orderSummary,
                        deliveryRoute,
                        hubBatch
                )
        ).thenReturn(prompt);

        when(
                aiHistoryPersistenceService.createPending(
                        eq(orderId),
                        eq(prompt),
                        any(Instant.class)
                )
        ).thenReturn(aiHistoryId);

        when(
                aiGeneratePort.generateFinalDispatchDeadline(prompt)
        ).thenThrow(
                new BusinessException(
                        ErrorCode.AI_REQUEST_FAILED
                )
        );

        // when & then
        assertThatThrownBy(
                () -> aiHistoryCommandService.create(command)
        )
                .isInstanceOf(BusinessException.class);

        verify(hubManagerPort)
                .getHubManager(departureHubId);

        verify(aiHistoryPersistenceService)
                .fail(
                        eq(aiHistoryId),
                        isNull(),
                        any(Instant.class)
                );

        verify(aiHistoryPersistenceService, never())
                .complete(
                        any(),
                        any(),
                        any(),
                        any()
                );

        verify(slackInternalMessageCommandService, never())
                .createAndPublish(
                        any(SlackInternalMessageCreateCommand.class)
                );
    }

    @Test
    @DisplayName("배송 경로가 없으면 DELIVERY_ROUTE_NOT_FOUND 예외가 발생한다")
    void create_deliveryRouteNotFound() {
        // given
        UUID orderId = UUID.randomUUID();

        AiHistoryCreateCommand command =
                new AiHistoryCreateCommand(orderId);

        OrderSummaryResult orderSummary =
                org.mockito.Mockito.mock(OrderSummaryResult.class);

        DeliveryRouteResult deliveryRoute =
                org.mockito.Mockito.mock(DeliveryRouteResult.class);

        when(orderSummaryPort.getOrderSummary(orderId))
                .thenReturn(orderSummary);

        when(deliveryRoutePort.getRoutesByOrderId(orderId))
                .thenReturn(deliveryRoute);

        when(deliveryRoute.routes())
                .thenReturn(List.of());

        // when & then
        assertThatThrownBy(
                () -> aiHistoryCommandService.create(command)
        )
                .isInstanceOf(BusinessException.class);

        verify(hubPort, never())
                .getHubs(anyList());

        verify(hubManagerPort, never())
                .getHubManager(any());

        verify(aiPromptGenerator, never())
                .generate(
                        any(),
                        any(),
                        any()
                );

        verify(aiHistoryPersistenceService, never())
                .createPending(
                        any(),
                        any(),
                        any()
                );

        verify(aiGeneratePort, never())
                .generateFinalDispatchDeadline(any());

        verify(slackInternalMessageCommandService, never())
                .createAndPublish(
                        any(SlackInternalMessageCreateCommand.class)
                );
    }
}