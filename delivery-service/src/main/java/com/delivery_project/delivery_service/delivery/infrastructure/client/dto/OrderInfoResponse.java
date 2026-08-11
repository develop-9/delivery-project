package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

import java.util.UUID;

public record OrderInfoResponse(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId
) {
}