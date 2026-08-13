package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.entity.QCompany;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
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
public class CompanyQueryRepositoryImpl implements CompanyQueryRepository {

    private final SpringDataCompanyRepository springDataCompanyRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Company> findById(UUID companyId) {
        return springDataCompanyRepository.findById(companyId);
    }

    @Override
    public Boolean existsById(UUID companyId) {
        return springDataCompanyRepository.existsById(companyId);
    }

    @Override
    public Page<Company> search(
            String name,
            CompanyType type,
            UUID hubId,
            Pageable pageable
    ) {
        QCompany company = QCompany.company;

        List<Company> content = jpaQueryFactory
                .selectFrom(company)
                .where(
                        nameContains(company, name),
                        typeEq(company, type),
                        hubIdEq(company, hubId)
                )
                .orderBy(getOrderSpecifier(company, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(company.count())
                .from(company)
                .where(
                        nameContains(company, name),
                        typeEq(company, type),
                        hubIdEq(company, hubId)
                );

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                countQuery::fetchOne
        );
    }

    private OrderSpecifier<?> getOrderSpecifier(
            QCompany company,
            Sort sort
    ) {
        Sort.Order order = sort.getOrderFor("createdAt");

        if (order == null || order.isDescending()) {
            return company.createdAt.desc();
        }

        return company.createdAt.asc();
    }

    private BooleanExpression nameContains(
            QCompany company,
            String name
    ) {
        return name != null
                ? company.name.contains(name)
                : null;
    }

    private BooleanExpression typeEq(
            QCompany company,
            CompanyType type
    ) {
        return type != null
                ? company.type.eq(type)
                : null;
    }

    private BooleanExpression hubIdEq(
            QCompany company,
            UUID hubId
    ) {
        return hubId != null
                ? company.hubId.eq(hubId)
                : null;
    }
}
