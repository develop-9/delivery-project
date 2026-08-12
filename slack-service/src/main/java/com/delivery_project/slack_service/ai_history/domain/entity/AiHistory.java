package com.delivery_project.slack_service.ai_history.domain.entity;

import com.delivery_project.slack_service.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "p_ai_histories",
        schema = "slack_schema"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiHistory extends BaseEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "order_id",
            nullable = false
    )
    private UUID orderId;

    @Column(
            name = "model_name",
            length = 100
    )
    private String modelName;

    @Column(
            name = "prompt",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String prompt;

    @Column(name = "final_dispatch_deadline")
    private Instant finalDispatchDeadline;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private AiHistoryStatus status;

    @Column(
            name = "requested_at",
            nullable = false
    )
    private Instant requestedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    private AiHistory(
            UUID id,
            UUID orderId,
            String prompt,
            Instant requestedAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.prompt = prompt;
        this.status = AiHistoryStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public static AiHistory createPending(
            UUID orderId,
            String prompt,
            Instant requestedAt
    ) {
        return new AiHistory(
                UUID.randomUUID(),
                orderId,
                prompt,
                requestedAt
        );
    }

    public void complete(
            String modelName,
            Instant finalDispatchDeadline,
            Instant respondedAt
    ) {
        this.modelName = modelName;
        this.finalDispatchDeadline = finalDispatchDeadline;
        this.respondedAt = respondedAt;
        this.status = AiHistoryStatus.SUCCESS;
    }

    public void fail(
            String modelName,
            Instant respondedAt
    ) {
        this.modelName = modelName;
        this.respondedAt = respondedAt;
        this.status = AiHistoryStatus.FAILED;
    }
}