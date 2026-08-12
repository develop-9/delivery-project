package com.delivery_project.delivery_service.delivery.application.port;

import com.delivery_project.delivery_service.delivery.application.result.OrderCompanyInfo;

import java.util.List;
import java.util.UUID;

public interface OrderPort {

    OrderCompanyInfo getOrderCompanyInfo(
            UUID orderId
    );

    List<UUID> getRelatedOrderIds(
            UUID companyId
    );

    void completeOrder(
            UUID orderId
    );
}
