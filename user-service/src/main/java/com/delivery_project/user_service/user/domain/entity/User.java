package com.delivery_project.user_service.user.domain.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

import com.delivery_project.user_service.global.common.BaseDeletableEntity;

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
public class User extends BaseDeletableEntity {

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
	private Instant approvedAt;

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

	public void approve(UUID approvedBy) {
		if (this.approvalStatus != ApprovalStatus.PENDING) {
			throw new IllegalStateException("이미 처리된 가입 신청입니다.");
		}
		this.approvalStatus = ApprovalStatus.APPROVED;
		this.approvedAt = Instant.now();
		this.approvedBy = approvedBy;
	}

	/**
	 * 활성 MASTER가 한 명도 없는 상태에서 MASTER로 가입하는 경우 전용. 승인은 이미 APPROVED인
	 * MASTER만 할 수 있는데, 그 MASTER가 아무도 없으면 첫 가입자가 영구히 PENDING에 머무는
	 * 데드락이 생기므로 가입과 동시에 자동 승인한다. 승인해줄 다른 사람이 없어 approvedBy는
	 * 비워둔다.
	 */
	public void approveAsInitialMaster() {
		if (this.approvalStatus != ApprovalStatus.PENDING) {
			throw new IllegalStateException("이미 처리된 가입 신청입니다.");
		}
		this.approvalStatus = ApprovalStatus.APPROVED;
		this.approvedAt = Instant.now();
	}

	public void reject() {
		if (this.approvalStatus != ApprovalStatus.PENDING) {
			throw new IllegalStateException("이미 처리된 가입 신청입니다.");
		}
		this.approvalStatus = ApprovalStatus.REJECTED;
	}

	public void updateProfile(String name, String slackId) {
		if (name != null) {
			this.name = name;
		}
		if (slackId != null) {
			this.slackId = slackId;
		}
	}

	public boolean isMaster() {
		return role == Role.MASTER;
	}

	public boolean isHubManagerOf(UUID hubId) {
		return role == Role.HUB_MANAGER && hubId != null && hubId.equals(this.hubId);
	}

	public boolean isSelf(UUID userId) {
		return this.id != null && this.id.equals(userId);
	}
}
