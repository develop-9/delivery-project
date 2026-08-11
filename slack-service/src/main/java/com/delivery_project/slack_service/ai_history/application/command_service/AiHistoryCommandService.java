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
import com.delivery_project.slack_service.ai_history.support.ai.AiPromptGenerator;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.slack.application.command.SlackInternalMessageCreateCommand;
import com.delivery_project.slack_service.slack.application.command_service.SlackInternalMessageCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AiHistoryCommandService {

    private final OrderSummaryPort orderSummaryPort;
    private final DeliveryRoutePort deliveryRoutePort;
    private final HubPort hubPort;
    private final HubManagerPort hubManagerPort;
    private final AiGeneratePort aiGeneratePort;
    private final AiPromptGenerator aiPromptGenerator;
    private final AiHistoryPersistenceService aiHistoryPersistenceService;
    private final SlackInternalMessageCommandService slackInternalMessageCommandService;

    public AiHistoryCreateResult create(
            AiHistoryCreateCommand command
    ) {
        OrderSummaryResult orderSummary =
                orderSummaryPort.getOrderSummary(
                        command.orderId()
                );

        DeliveryRouteResult deliveryRoute =
                deliveryRoutePort.getRoutesByOrderId(
                        command.orderId()
                );

        validateDeliveryRoute(deliveryRoute);

        List<UUID> hubIds =
                extractHubIds(deliveryRoute);

        HubBatchResult hubBatch =
                hubPort.getHubs(hubIds);

        validateHubBatch(hubBatch);

        UUID firstDepartureHubId =
                getFirstDepartureHubId(
                        deliveryRoute
                );

        HubManagerResult hubManager =
                hubManagerPort.getHubManager(
                        firstDepartureHubId
                );

        String prompt =
                aiPromptGenerator.generate(
                        orderSummary,
                        deliveryRoute,
                        hubBatch
                );

        Instant requestedAt =
                Instant.now();

        UUID aiHistoryId =
                aiHistoryPersistenceService.createPending(
                        command.orderId(),
                        prompt,
                        requestedAt
                );

        AiGenerationResult generationResult;

        try {
            generationResult =
                    aiGeneratePort
                            .generateFinalDispatchDeadline(
                                    prompt
                            );

        } catch (BusinessException exception) {
            saveFailedHistory(
                    aiHistoryId,
                    null
            );

            throw exception;

        } catch (Exception exception) {
            saveFailedHistory(
                    aiHistoryId,
                    null
            );

            throw new BusinessException(
                    ErrorCode.AI_REQUEST_FAILED
            );
        }

        AiHistoryCreateResult result =
                AiHistoryCreateResult.from(
                        aiHistoryPersistenceService.complete(
                                aiHistoryId,
                                generationResult.modelName(),
                                generationResult.finalDispatchDeadline(),
                                Instant.now()
                        )
                );

        slackInternalMessageCommandService.createAndPublish(
                new SlackInternalMessageCreateCommand(
                        hubManager.userId(),
                        command.orderId(),
                        generationResult.finalDispatchDeadline()
                )
        );

        return result;
    }

    private void validateDeliveryRoute(
            DeliveryRouteResult deliveryRoute
    ) {
        if (
                deliveryRoute.routes() == null
                        || deliveryRoute.routes().isEmpty()
        ) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_ROUTE_NOT_FOUND
            );
        }
    }

    private List<UUID> extractHubIds(
            DeliveryRouteResult deliveryRoute
    ) {
        return deliveryRoute.routes()
                .stream()
                .flatMap(route ->
                        Stream.of(
                                route.departureHubId(),
                                route.arrivalHubId()
                        )
                )
                .distinct()
                .toList();
    }

    private UUID getFirstDepartureHubId(
            DeliveryRouteResult deliveryRoute
    ) {
        return deliveryRoute.routes()
                .stream()
                .min(
                        Comparator.comparing(
                                DeliveryRouteResult.RouteResult::sequence
                        )
                )
                .map(
                        DeliveryRouteResult.RouteResult::departureHubId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.DELIVERY_ROUTE_NOT_FOUND
                        )
                );
    }

    private void validateHubBatch(
            HubBatchResult hubBatch
    ) {
        if (
                hubBatch == null
                        || hubBatch.hubs() == null
        ) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }

        if (
                hubBatch.notFoundHubIds() != null
                        && !hubBatch.notFoundHubIds().isEmpty()
        ) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }
    }

    private void saveFailedHistory(
            UUID aiHistoryId,
            String modelName
    ) {
        aiHistoryPersistenceService.fail(
                aiHistoryId,
                modelName,
                Instant.now()
        );
    }
}