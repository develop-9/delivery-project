package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerCreateCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerDeleteCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerInternalDeleteCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerUpdateCommand;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerCreateResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerDeleteResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerInternalDeleteResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerUpdateResult;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerCommandRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryManagerCommandService {
    private final DeliveryManagerCommandRepository
            deliveryManagerCommandRepository;
    @Value("${system.id}")
    private String systemId;

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

        try {
            DeliveryManager savedManager =
                    deliveryManagerCommandRepository.save(deliveryManager);

            return DeliveryManagerCreateResult.from(savedManager);

        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_ALREADY_EXISTS
            );
        }
    }

    private void validateDuplicateUser(
            DeliveryManagerCreateCommand command
    ){
        boolean alreadyExists =
                deliveryManagerCommandRepository.existsByUserId(command.userId());

        if (alreadyExists) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_ALREADY_EXISTS
            );
        }
    }

    private int calculateNextSequence(
            DeliveryManagerCreateCommand command
    ) {
        return calculateNextSequence(
                command.hubId(),
                command.type()
        );
    }

    private int calculateNextSequence(
            UUID hubId,
            DeliveryManagerType type
    ) {
        Optional<Integer> maxSequence;

        if (type == DeliveryManagerType.HUB_DELIVERY) {
            maxSequence =
                    deliveryManagerCommandRepository.findMaxSequenceByType(type);
        } else {
            maxSequence =
                    deliveryManagerCommandRepository.findMaxSequenceByHubIdAndType(
                            hubId,
                            type
                    );
        }

        return maxSequence.orElse(-1) + 1;
    }

    public DeliveryManagerUpdateResult update(
            DeliveryManagerUpdateCommand command
    ) {
        validateUpdateRequest(command);

        DeliveryManager deliveryManager =
                deliveryManagerCommandRepository
                        .findById(command.managerId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                                )
                        );

        DeliveryManagerType updatedType =
                command.type() != null
                        ? command.type()
                        : deliveryManager.getType();

        UUID updatedHubId =
                resolveUpdatedHubId(
                        deliveryManager,
                        command,
                        updatedType
                );

        boolean assignmentRangeChanged =
                deliveryManager.getType() != updatedType
                        || !Objects.equals(
                        deliveryManager.getHubId(),
                        updatedHubId
                );

        // TODO: COMPANY_DELIVERY인 경우 Hub Service에서
        //       updatedHubId 존재 여부를 검증한다.
        //       존재하지 않으면 BusinessException(ErrorCode.HUB_NOT_FOUND) 발생

        deliveryManager.update(
                updatedHubId,
                updatedType
        );

        if (assignmentRangeChanged) {
            deliveryManager.updateDeliverySequence(
                    calculateNextSequence(
                            updatedHubId,
                            updatedType
                    )
            );
        }

        return DeliveryManagerUpdateResult.from(deliveryManager);
    }

    private void validateUpdateRequest(
            DeliveryManagerUpdateCommand command
    ) {
        if (command.hubId() == null
                && command.type() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }
    }

    private UUID resolveUpdatedHubId(
            DeliveryManager deliveryManager,
            DeliveryManagerUpdateCommand command,
            DeliveryManagerType updatedType
    ) {
        if (updatedType == DeliveryManagerType.HUB_DELIVERY) {
            return null;
        }

        return command.hubId() != null
                ? command.hubId()
                : deliveryManager.getHubId();
    }

    public DeliveryManagerDeleteResult delete(
            DeliveryManagerDeleteCommand command
    ){
        DeliveryManager deliveryManager =
                deliveryManagerCommandRepository.findById(command.managerId())
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                                )
                        );
        // TODO: 진행 중인 Delivery 또는 DeliveryRoute 배정 여부 검증
        // 존재하면 ACTIVE_DELIVERY_EXISTS 예외 발생

        deliveryManager.deleteManager(command.deletedBy());

        return DeliveryManagerDeleteResult.from(deliveryManager);
    }

    public DeliveryManagerInternalDeleteResult deleteByUserId(
            DeliveryManagerInternalDeleteCommand command
    ) {
        DeliveryManager deliveryManager =
                deliveryManagerCommandRepository.findByUserId(command.userId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                                )
                        );
        UUID deletedBy = UUID.fromString(systemId);
        /*
         * TODO:
         * Delivery / DeliveryRoute 구현 후
         * 진행 중인 배송 배정 여부 검증
         */

        deliveryManager.deleteManager(deletedBy);

        return DeliveryManagerInternalDeleteResult.from(
                deliveryManager
        );
    }
}
