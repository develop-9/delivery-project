package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.*;
import com.delivery_project.company_service.company.domain.repository.ProductQueryRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final SpringDataProductRepository springDataProductRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Product> findById(UUID productId) {
        return springDataProductRepository.findById(productId);
    }

    @Override
    public Page<Product> search(
            UUID companyId,
            String name,
            Integer minPrice,
            Integer maxPrice,
            Pageable pageable
    ) {
        QProduct product = QProduct.product;

        List<Product> content = jpaQueryFactory
                .selectFrom(product)
                .where(
                        companyIdEq(product, companyId),
                        nameContains(product, name),
                        priceGoe(product, minPrice),
                        priceLoe(product, maxPrice)
                )
                .orderBy(getOrderSpecifier(product, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(product.count())
                .from(product)
                .where(
                        companyIdEq(product, companyId),
                        nameContains(product, name),
                        priceGoe(product, minPrice),
                        priceLoe(product, maxPrice)
                );

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                countQuery::fetchOne
        );
    }

    private OrderSpecifier<?> getOrderSpecifier(
            QProduct product,
            Sort sort
    ) {
        Sort.Order order = sort.getOrderFor("createdAt");

        if (order == null || order.isDescending()) {
            return product.createdAt.desc();
        }

        return product.createdAt.asc();
    }

    private BooleanExpression companyIdEq(
            QProduct product,
            UUID companyId
    ) {
        return companyId != null
                ? product.companyId.eq(companyId)
                : null;
    }

    private BooleanExpression nameContains(
            QProduct product,
            String name
    ) {
        return name != null
                ? product.name.contains(name)
                : null;
    }

    private BooleanExpression priceGoe(
            QProduct product,
            Integer minPrice
    ) {
        return minPrice != null
                ? product.price.goe(minPrice)
                : null;
    }

    private BooleanExpression priceLoe(
            QProduct product,
            Integer maxPrice
    ) {
        return maxPrice != null
                ? product.price.loe(maxPrice)
                : null;
    }
}
