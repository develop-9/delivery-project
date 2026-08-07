package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryCreateCommand;
import com.delivery_project.delivery_service.delivery.application.port.DeliveryCreationLockPort;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryCreateResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPath;
import com.delivery_project.delivery_service.delivery.application.result.ReceiverInfo;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryPersistenceService {

    private final DeliveryCommandRepository deliveryCommandRepository;
    private final DeliveryRouteCommandRepository deliveryRouteCommandRepository;
    private final DeliveryCreationLockPort deliveryCreationLockPort;

    @Transactional
    public DeliveryCreateResult create(
            DeliveryCreateCommand command,
            ReceiverInfo receiver,
            DeliveryPath deliveryPath
    ) {
        deliveryCreationLockPort.lock(command.orderId());

        boolean alreadyExists =
                deliveryCommandRepository
                        .findByOrderIdAndDeletedAtIsNull(command.orderId())
                        .isPresent();

        if (alreadyExists) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_ALREADY_EXISTS
            );
        }

        Delivery delivery = Delivery.create(
                command.orderId(),
                command.departureHubId(),
                command.destinationHubId(),
                command.deliveryAddress(),
                receiver.name(),
                receiver.slackId()
        );

        Delivery savedDelivery =
                deliveryCommandRepository.save(delivery);

        List<DeliveryRoute> deliveryRoutes =
                deliveryPath.segments()
                        .stream()
                        .map(segment -> DeliveryRoute.create(
                                savedDelivery.getId(),
                                segment.sequence(),
                                segment.departureHubId(),
                                segment.arrivalHubId(),
                                segment.distanceKm(),
                                segment.durationMin()
                        ))
                        .toList();

        deliveryRouteCommandRepository.saveAll(deliveryRoutes);

        return new DeliveryCreateResult(
                savedDelivery.getId(),
                savedDelivery.getOrderId(),
                savedDelivery.getStatus(),
                deliveryRoutes.size()
        );
    }
}