package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerCreateCommand;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerCreateResult;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        if(alreadyExists){
            throw new IllegalArgumentException(
                    "이미 등록된 배송 담당자입니다."
            );
        }
    }

    private int calculateNextSequence(
            DeliveryManagerCreateCommand command){
        return deliveryManagerRepository.findMaxSequence(
                command.hubId(),
                command.type()
        )
        .orElse(-1) + 1;
    }
}
