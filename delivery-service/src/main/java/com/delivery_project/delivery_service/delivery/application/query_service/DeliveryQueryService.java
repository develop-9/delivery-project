package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryListQuery;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryDetailResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryListResult;
import com.delivery_project.delivery_service.delivery.application.result.UserAuthorizationInfo;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerQueryRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryQueryRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteQueryRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import com.delivery_project.delivery_service.global.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryQueryService {

    private final DeliveryQueryRepository deliveryQueryRepository;
    private final DeliveryManagerQueryRepository deliveryManagerQueryRepository;
    private final DeliveryRouteQueryRepository deliveryRouteQueryRepository;
    private final UserPort userPort;

    public DeliveryDetailResult getDelivery(
            DeliveryGetQuery query
    ){
        Delivery delivery =
                deliveryQueryRepository
                        .findById(query.deliveryId())
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_NOT_FOUND
                                )
                        );
        validateReadPermission(query, delivery);

        return DeliveryDetailResult.from(delivery);
    }

    public Page<DeliveryListResult> getDeliveries(
            DeliveryListQuery query
    ){
        validatePage(query.page());

        int size = validateSize(query.size());

        validateListPermission(query);

        UUID requesterManagerId = null;
        UUID requesterHubId = null;

        if (query.requesterRole() == Role.DELIVERY_MANAGER) {
            requesterManagerId =
                    deliveryManagerQueryRepository
                            .findByUserId(query.requesterId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.READ_DELIVERY_FORBIDDEN
                                    )
                            )
                            .getId();
        }

        if (query.requesterRole() == Role.HUB_MANAGER) {
            UserAuthorizationInfo requester =
                    userPort.getUserAuthorizationInfo(
                            query.requesterId()
                    );

            requesterHubId = requester.hubId();

            if (requesterHubId == null) {
                throw new BusinessException(
                        ErrorCode.READ_DELIVERY_FORBIDDEN
                );
            }
        }

        Pageable pageable =
                PageRequest.of(
                        query.page(),
                        size
                );

        return deliveryQueryRepository
                .search(
                        query,
                        pageable,
                        requesterManagerId,
                        requesterHubId)
                .map(DeliveryListResult::from);
    }

    private void validatePage(
            int page
    ){
        if(page < 0){
            throw new BusinessException(
                    ErrorCode.INVALID_PAGE_NUMBER
            );
        }
    }

    private int validateSize(
            int size
    ){
        if(size != 10
                && size != 30
                && size != 50){
            return 10;
        }

        return size;
    }

    private void validateReadPermission(
            DeliveryGetQuery query,
            Delivery delivery
    ) {
        if (query.requesterRole() == Role.MASTER) {
            return;
        }

        if (query.requesterRole() == Role.HUB_MANAGER) {
            validateHubManagerReadPermission(
                    query.requesterId(),
                    delivery
            );
            return;
        }

        if (query.requesterRole() == Role.COMPANY_MANAGER) {
            // TODO: 담당 업체 조회 방식 확정 후 범위 검증
            return;
        }

        if (query.requesterRole() == Role.DELIVERY_MANAGER) {
            validateDeliveryManagerReadPermission(
                    query.requesterId(),
                    delivery
            );
            return;
        }

        throw new BusinessException(
                ErrorCode.READ_DELIVERY_FORBIDDEN
        );
    }

    private void validateDeliveryManagerReadPermission(
            UUID requesterId,
            Delivery delivery
    ) {
        DeliveryManager manager =
                deliveryManagerQueryRepository
                        .findByUserId(requesterId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.READ_DELIVERY_FORBIDDEN
                                )
                        );

        // 최종 업체 배송 담당자인 경우
        if (manager.getId().equals(
                delivery.getCompanyDeliveryManagerId()
        )) {
            return;
        }

        // 허브 배송 Route 담당자인 경우
        boolean assignedRouteExists =
                deliveryRouteQueryRepository
                        .existsByDeliveryIdAndDeliveryManagerId(
                                delivery.getId(),
                                manager.getId()
                        );

        if (assignedRouteExists) {
            return;
        }

        throw new BusinessException(
                ErrorCode.READ_DELIVERY_FORBIDDEN
        );
    }

    private void validateListPermission(
            DeliveryListQuery query
    ) {
        if (query.requesterRole() == Role.MASTER) {
            return;
        }

        if (query.requesterRole() == Role.HUB_MANAGER) {
            return;
        }

        if (query.requesterRole() == Role.COMPANY_MANAGER) {
            // TODO: 담당 업체 기준 조회 범위 적용
            return;
        }

        if (query.requesterRole() == Role.DELIVERY_MANAGER) {
            return;
        }

        throw new BusinessException(
                ErrorCode.READ_DELIVERY_FORBIDDEN
        );
    }

    private void validateHubManagerReadPermission(
            UUID requesterId,
            Delivery delivery
    ) {
        UserAuthorizationInfo requester =
                userPort.getUserAuthorizationInfo(
                        requesterId
                );

        UUID requesterHubId = requester.hubId();

        if (requesterHubId == null) {
            throw new BusinessException(
                    ErrorCode.READ_DELIVERY_FORBIDDEN
            );
        }

        boolean relatedHubDelivery =
                deliveryRouteQueryRepository
                        .existsByDeliveryIdAndHubId(
                                delivery.getId(),
                                requesterHubId
                        );

        if (!relatedHubDelivery) {
            throw new BusinessException(
                    ErrorCode.READ_DELIVERY_FORBIDDEN
            );
        }
    }
}
