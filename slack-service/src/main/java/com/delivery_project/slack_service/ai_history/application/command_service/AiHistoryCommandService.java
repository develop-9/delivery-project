package com.delivery_project.slack_service.ai_history.application.command_service;

import com.delivery_project.slack_service.ai_history.application.command.AiHistoryCreateCommand;
import com.delivery_project.slack_service.ai_history.application.persistence_service.AiHistoryPersistenceService;
import com.delivery_project.slack_service.ai_history.application.port.AiGeneratePort;
import com.delivery_project.slack_service.ai_history.application.port.DeliveryRoutePort;
import com.delivery_project.slack_service.ai_history.application.port.OrderSummaryPort;
import com.delivery_project.slack_service.ai_history.application.result.AiGenerationResult;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryCreateResult;
import com.delivery_project.slack_service.ai_history.application.result.DeliveryRouteResult;
import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;
import com.delivery_project.slack_service.ai_history.support.ai.AiPromptGenerator;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiHistoryCommandService {

    private final OrderSummaryPort orderSummaryPort;
    private final DeliveryRoutePort deliveryRoutePort;
    private final AiGeneratePort aiGeneratePort;
    private final AiPromptGenerator aiPromptGenerator;
    private final AiHistoryPersistenceService aiHistoryPersistenceService;

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

        return AiHistoryCreateResult.from(
                aiHistoryPersistenceService.complete(
                        aiHistoryId,
                        generationResult.modelName(),
                        generationResult.finalDispatchDeadline(),
                        Instant.now()
                )
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