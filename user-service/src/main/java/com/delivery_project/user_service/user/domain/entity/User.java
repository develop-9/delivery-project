package com.delivery_project.user_service.user.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

import com.delivery_project.user_service.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "username", nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "password", nullable = false, length = 255)
	private String password;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "slack_id", nullable = false, unique = true, length = 100)
	private String slackId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 30)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", nullable = false, length = 20)
	private ApprovalStatus approvalStatus;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "approved_by")
	private UUID approvedBy;

	@Column(name = "hub_id")
	private UUID hubId;

	@Column(name = "company_id")
	private UUID companyId;

	@Builder
	private User(String username, String password, String name, String slackId,
			Role role, UUID hubId, UUID companyId) {
		this.username = username;
		this.password = password;
		this.name = name;
		this.slackId = slackId;
		this.role = role;
		this.approvalStatus = ApprovalStatus.PENDING;
		this.hubId = hubId;
		this.companyId = companyId;
	}
}
