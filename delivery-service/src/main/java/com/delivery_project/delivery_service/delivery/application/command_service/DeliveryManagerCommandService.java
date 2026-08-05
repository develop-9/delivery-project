package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerCreateCommand;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerCreateResult;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryManagerCommandService {
    private final DeliveryManagerRepository deliveryManagerRepository;

    public DeliveryManagerCreateResult create(
            DeliveryManagerCreateCommand command
    ){
        validateDuplicateUser(command);

        int nextSequence = calculateNextSequence(command);

        DeliveryManager deliveryManager = DeliveryManager.create(
                command.userId(),
                command.hubId(),
                command.type(),
                nextSequence
        );

        DeliveryManager savedManager =
                deliveryManagerRepository.save(deliveryManager);
        return DeliveryManagerCreateResult.from(savedManager);
    }

    private void validateDuplicateUser(
            DeliveryManagerCreateCommand command
    ){
        boolean alreadyExists =
                deliveryManagerRepository.existsByUserId(command.userId());

        if (alreadyExists) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_ALREADY_EXISTS
            );
        }
    }

    private int calculateNextSequence(
            DeliveryManagerCreateCommand command
    ) {
        Optional<Integer> maxSequence;

        if (command.type() == DeliveryManagerType.HUB_DELIVERY) {
            maxSequence =
                    deliveryManagerRepository.findMaxSequenceByType(
                            command.type()
                    );
        } else {
            maxSequence =
                    deliveryManagerRepository.findMaxSequenceByHubIdAndType(
                            command.hubId(),
                            command.type()
                    );
        }

        return maxSequence.orElse(-1) + 1;
    }
}
