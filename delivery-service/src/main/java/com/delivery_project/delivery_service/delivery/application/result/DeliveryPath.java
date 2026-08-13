package com.delivery_project.delivery_service.delivery.application.result;

import java.util.List;

public record DeliveryPath(
        List<DeliveryPathSegment> segments
) {
    public int routeCount(){
        return segments.size();
    }
}
