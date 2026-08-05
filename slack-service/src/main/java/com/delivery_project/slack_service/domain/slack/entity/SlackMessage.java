package com.delivery_project.slack_service.domain.slack.entity;

import com.delivery_project.slack_service.global.common.BaseDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_slack_messages", schema = "slack_db")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlackMessage extends BaseDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sender_user_id")
    private UUID senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType;

    @Column(name = "receiver_user_id")
    private UUID receiverUserId;

    @Column(name = "receiver_slack_id", nullable = false, length = 100)
    private String receiverSlackId;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SlackMessageStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    private SlackMessage(
            UUID senderUserId,
            SenderType senderType,
            UUID receiverUserId,
            String receiverSlackId,
            String message
    ) {
        this.senderUserId = senderUserId;
        this.senderType = senderType;
        this.receiverUserId = receiverUserId;
        this.receiverSlackId = receiverSlackId;
        this.message = message;
        this.status = SlackMessageStatus.PENDING;
        this.retryCount = 0;
    }

    public static SlackMessage create(
            UUID senderUserId,
            SenderType senderType,
            UUID receiverUserId,
            String receiverSlackId,
            String message
    ) {
        return new SlackMessage(
                senderUserId,
                senderType,
                receiverUserId,
                receiverSlackId,
                message
        );
    }

    public void updateMessage(String message) {
        this.message = message;
    }

    public void markAsSent() {
        this.status = SlackMessageStatus.SENT;
        this.sentAt = Instant.now();
        this.failureMessage = null;
    }

    public void markAsFailed(String failureMessage) {
        this.status = SlackMessageStatus.FAILED;
        this.retryCount++;
        this.failureMessage = failureMessage;
    }
}