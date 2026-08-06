package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.entity.QCompany;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
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
    public Page<Company> search(
            String name,
            CompanyType type,
            UUID hubId,
            Pageable pageable
    ) {
        QCompany company = QCompany.company;

        BooleanBuilder condition = new BooleanBuilder();

        if (name != null && !name.isBlank()) {
            condition.and(
                    company.name.contains(name)
            );
        }

        if (type != null) {
            condition.and(
                    company.type.eq(type)
            );
        }

        if (hubId != null) {
            condition.and(
                    company.hubId.eq(hubId)
            );
        }

        List<Company> content = jpaQueryFactory
                .selectFrom(company)
                .where(condition)
                .orderBy(
                        getOrderSpecifier(
                                company,
                                pageable.getSort()
                        )
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(company.count())
                .from(company)
                .where(condition);

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
}
