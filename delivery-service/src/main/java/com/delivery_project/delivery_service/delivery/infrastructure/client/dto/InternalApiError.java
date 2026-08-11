package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

public record InternalApiError(
        String errorCode,
        String message
) {
}