package com.delivery_project.delivery_service.delivery.application.port;

import com.delivery_project.delivery_service.delivery.application.result.OrderCompanyInfo;

import java.util.UUID;

public interface OrderPort {

    OrderCompanyInfo getOrderCompanyInfo(
            UUID orderId
    );
}
