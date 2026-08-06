package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;

import java.util.List;

public interface DeliveryRouteCommandRepository {

    List<DeliveryRoute> saveAll(List<DeliveryRoute> routes);

}
