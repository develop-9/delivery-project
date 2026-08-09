package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

public record InternalApiResponse<T>(
        boolean success,
        T data
) {
}
