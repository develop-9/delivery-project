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
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiHistoryCommandService {

    private final OrderSummaryClient orderSummaryClient;
    private final DeliveryRouteClient deliveryRouteClient;
    private final AiGenerationClient aiGenerationClient;
    private final AiPromptGenerator aiPromptGenerator;
    private final AiHistoryPersistenceService aiHistoryPersistenceService;

    public AiHistoryCreateResult create(
            AiHistoryCreateCommand command
    ) {
        OrderSummaryResult orderSummary =
                orderSummaryClient.getOrderSummary(
                        command.orderId()
                );

        DeliveryRouteResult deliveryRoute =
                deliveryRouteClient.getRoutesByOrderId(
                        command.orderId()
                );

        validateDeliveryRoute(deliveryRoute);

        String prompt =
                aiPromptGenerator.generate(
                        orderSummary,
                        deliveryRoute
                );

        Instant requestedAt = Instant.now();

        UUID aiHistoryId =
                aiHistoryPersistenceService.createPending(
                        command.orderId(),
                        prompt,
                        requestedAt
                );

        AiGenerationResult generationResult;

        try {
            generationResult =
                    aiGenerationClient
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

        AiHistory completedAiHistory =
                aiHistoryPersistenceService.complete(
                        aiHistoryId,
                        generationResult.modelName(),
                        generationResult.finalDispatchDeadline(),
                        Instant.now()
                );

        return AiHistoryCreateResult.from(
                completedAiHistory
        );
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