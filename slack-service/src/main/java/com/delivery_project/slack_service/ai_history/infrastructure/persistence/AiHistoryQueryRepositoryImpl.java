package com.delivery_project.slack_service.ai_history.infrastructure.persistence;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;
import com.delivery_project.slack_service.ai_history.domain.repository.AiHistoryQueryRepository;
import com.delivery_project.slack_service.global.common.PageData;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AiHistoryQueryRepositoryImpl
        implements AiHistoryQueryRepository {

    private final SpringDataAiHistoryRepository springDataAiHistoryRepository;

    @Override
    public Optional<AiHistory> findById(
            UUID aiHistoryId
    ) {
        return springDataAiHistoryRepository.findById(aiHistoryId);
    }

    @Override
    public PageData<AiHistory> findAll(
            UUID orderId,
            AiHistoryStatus status,
            String modelName,
            Instant startDate,
            Instant endDate,
            int page,
            int size,
            String sortField,
            String sortDirection
    ) {
        Specification<AiHistory> specification =
                createSpecification(
                        orderId,
                        status,
                        modelName,
                        startDate,
                        endDate
                );

        Sort.Direction direction =
                Sort.Direction.fromString(sortDirection);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortField)
        );

        Page<AiHistory> aiHistoryPage =
                springDataAiHistoryRepository.findAll(
                        specification,
                        pageRequest
                );

        return new PageData<>(
                aiHistoryPage.getContent(),
                aiHistoryPage.getNumber(),
                aiHistoryPage.getSize(),
                aiHistoryPage.getTotalElements(),
                aiHistoryPage.getTotalPages()
        );
    }

    private Specification<AiHistory> createSpecification(
            UUID orderId,
            AiHistoryStatus status,
            String modelName,
            Instant startDate,
            Instant endDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (orderId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("orderId"),
                                orderId
                        )
                );
            }

            if (status != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                status
                        )
                );
            }

            if (modelName != null && !modelName.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("modelName"),
                                modelName
                        )
                );
            }

            if (startDate != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("requestedAt"),
                                startDate
                        )
                );
            }

            if (endDate != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("requestedAt"),
                                endDate
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}