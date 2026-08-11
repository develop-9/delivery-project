package com.delivery_project.delivery_service.delivery.application.result;

import java.util.UUID;

public record OrderCompanyInfo(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId
) {
}
