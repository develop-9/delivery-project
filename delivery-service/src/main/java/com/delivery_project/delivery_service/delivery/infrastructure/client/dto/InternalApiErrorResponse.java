package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

public record InternalApiErrorResponse(
        boolean success,
        InternalApiError error
) {
}