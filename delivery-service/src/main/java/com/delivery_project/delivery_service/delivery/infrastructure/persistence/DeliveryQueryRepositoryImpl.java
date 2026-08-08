package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.application.query.DeliveryListQuery;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryQueryRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.delivery_project.delivery_service.delivery.domain.entity.QDelivery.delivery;

@Repository
@RequiredArgsConstructor
public class DeliveryQueryRepositoryImpl
        implements DeliveryQueryRepository {

    private final SpringDataDeliveryRepository springDataRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Delivery> findById(
            UUID deliveryId
    ){
        return springDataRepository
                .findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Override
    public Optional<Delivery> findByOrderId(
            UUID orderId
    ){
        return springDataRepository
                .findByOrderIdAndDeletedAtIsNull(orderId);
    }

    @Override
    public Page<Delivery> search(
            DeliveryListQuery query,
            Pageable pageable
    ){
        List<Delivery> content =
                queryFactory
                        .selectFrom(delivery)
                        .where(
                                delivery.deletedAt.isNull(),
                                orderIdEq(query.orderId()),
                                statusEq(query.status()),
                                departureHubIdEq(query.departureHubId()),
                                destinationHubIdEq(query.destinationHubId()),
                                companyDeliveryManagerIdEq(
                                        query.companyDeliveryManagerId()
                                )
                        )
                        .orderBy(
                                orderBy(
                                        query.sortBy(),
                                        query.direction()
                                )
                        )
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        Long total =
                queryFactory
                        .select(delivery.count())
                        .from(delivery)
                        .where(
                                delivery.deletedAt.isNull(),
                                orderIdEq(query.orderId()),
                                statusEq(query.status()),
                                departureHubIdEq(query.departureHubId()),
                                destinationHubIdEq(query.destinationHubId()),
                                companyDeliveryManagerIdEq(
                                        query.companyDeliveryManagerId()
                                )
                        )
                        .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    private BooleanExpression orderIdEq(
            UUID orderId
    ) {
        return orderId != null
                ? delivery.orderId.eq(orderId)
                : null;
    }

    private BooleanExpression statusEq(
            DeliveryStatus status
    ) {
        return status != null
                ? delivery.status.eq(status)
                : null;
    }

    private BooleanExpression departureHubIdEq(
            UUID departureHubId
    ) {
        return departureHubId != null
                ? delivery.departureHubId.eq(departureHubId)
                : null;
    }

    private BooleanExpression destinationHubIdEq(
            UUID destinationHubId
    ) {
        return destinationHubId != null
                ? delivery.destinationHubId.eq(destinationHubId)
                : null;
    }

    private BooleanExpression companyDeliveryManagerIdEq(
            UUID companyDeliveryManagerId
    ) {
        return companyDeliveryManagerId != null
                ? delivery.companyDeliveryManagerId.eq(
                companyDeliveryManagerId
        )
                : null;
    }

    private OrderSpecifier<?> orderBy(
            String sortBy,
            String direction
    ) {
        boolean ascending =
                "asc".equalsIgnoreCase(direction);

        if ("updatedAt".equals(sortBy)) {
            return ascending
                    ? delivery.updatedAt.asc()
                    : delivery.updatedAt.desc();
        }

        return ascending
                ? delivery.createdAt.asc()
                : delivery.createdAt.desc();
    }
}
